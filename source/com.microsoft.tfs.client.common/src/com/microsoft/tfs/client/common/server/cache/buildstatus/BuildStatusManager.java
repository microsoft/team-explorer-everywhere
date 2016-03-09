// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.server.cache.buildstatus;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.prefs.PreferenceConstants;
import com.microsoft.tfs.client.common.util.ConnectionHelper;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.buildstatus.BuildStatusCache;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.InformationTypes;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * A {@link BuildStatusManager} polls the Team Foundation Server periodically to
 * update the status of builds. Listeners will be notified when the status of a
 * build has changed. This allows integration points with reconciliation of
 * workspaces for gated checkins.
 */
public class BuildStatusManager {
    public static final CodeMarker CODEMARKER_WATCHED_BUILD_REMOVED = new CodeMarker(
        "com.microsoft.tfs.client.common.server.cache.buildstatus.BuildStatusManager#watchedBuildRemoved"); //$NON-NLS-1$

    private final Log log = LogFactory.getLog(BuildStatusManager.class);

    private final TFSTeamProjectCollection connection;

    private final Object watchedBuildLock = new Object();
    private final Map<Integer, IQueuedBuild> watchedBuilds = new HashMap<Integer, IQueuedBuild>();

    /*
     * Listeners who will be notified when a new build is being watched (or a
     * build is no longer being watched) or when a watched build's status
     * changes.
     */
    private final SingleListenerFacade listeners = new SingleListenerFacade(BuildStatusManagerListener.class);

    private volatile int refreshInterval = (5 * 60 * 1000);

    private final Object refreshLock = new Object();
    private boolean refreshInBackground = true;
    private boolean refreshInProgress = false;
    private long refreshLastTime = 0;

    /* Plugins may add build status listeners via extension points */
    public static final String LISTENER_EXTENSION_POINT_ID =
        "com.microsoft.tfs.client.common.buildStatusManagerListeners"; //$NON-NLS-1$

    public BuildStatusManager(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;

        loadConfiguration();
        loadExtensionListeners();

        final Thread refreshThread = new Thread(new BuildStatusManagerRefreshWorker());
        refreshThread.setName("Build Status Manager"); //$NON-NLS-1$
        refreshThread.start();
    }

    /**
     * Sets fields from saved preferences.
     */
    private void loadConfiguration() {
        /*
         * TODO Use a non-deprecated preference interface for this non-UI
         * plug-in. When Eclipse deprecated Preferences and
         * getPluginPreferences(), it put the modern equivalent only in
         * AbstractUIPlugin (which this plug-in isn't). We need to write our own
         * adapter or other preference layer and use that instead.
         */

        final Preferences prefs = TFSCommonClientPlugin.getDefault().getPluginPreferences();

        int refreshIntervalMillis = prefs.getInt(PreferenceConstants.BUILD_STATUS_REFRESH_INTERVAL);

        if (refreshIntervalMillis <= 0) {
            refreshIntervalMillis = prefs.getDefaultInt(PreferenceConstants.BUILD_STATUS_REFRESH_INTERVAL);
        }

        if (refreshIntervalMillis > 0) {
            refreshInterval = refreshIntervalMillis;
        }
    }

    private void loadExtensionListeners() {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint extensionPoint = registry.getExtensionPoint(LISTENER_EXTENSION_POINT_ID);

        final IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

        for (int i = 0; i < elements.length; i++) {
            try {
                final BuildStatusManagerListener listener =
                    (BuildStatusManagerListener) elements[0].createExecutableExtension("class"); //$NON-NLS-1$

                if (listener == null) {
                    log.warn(MessageFormat.format(
                        "Could not create build status manager listener with id {0}", //$NON-NLS-1$
                        elements[0].getAttribute("id"))); //$NON-NLS-1$
                } else {
                    addListener(listener);
                }
            } catch (final CoreException e) {
                log.warn(
                    MessageFormat.format(
                        "Could not create build status manager listener with id {0}", //$NON-NLS-1$
                        elements[0].getAttribute("id")), //$NON-NLS-1$
                    e);
            }
        }
    }

    public void setRefreshInterval(final int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public void refresh() {
        synchronized (refreshLock) {
            if (refreshInProgress) {
                return;
            }

            refreshInProgress = true;
        }

        refreshInternal();
    }

    /**
     * You must set refreshInProgress to true before calling this method, to
     * block other refreshes from running.
     */
    private void refreshInternal() {
        synchronized (refreshLock) {
            Check.isTrue(refreshInProgress, "refreshInProgress"); //$NON-NLS-1$
        }

        try {
            /* If we're not connected, defer. */
            if (!ConnectionHelper.isConnected(connection)) {
                return;
            }

            /*
             * If the current server does not support queued builds at all (TFS
             * 2005, V1 service), do no work here.
             */
            if (connection.getBuildServer().getBuildServerVersion().isV1()) {
                return;
            }

            /* A list of build changes to notify listeners for */
            final List<IQueuedBuild> watchAddedList = new ArrayList<IQueuedBuild>();
            final List<IQueuedBuild> watchRemovedList = new ArrayList<IQueuedBuild>();
            final List<IQueuedBuild> statusChangedList = new ArrayList<IQueuedBuild>();

            /* The list of watched build IDs to query from the server */
            final List<Integer> queryIdList = new ArrayList<Integer>();

            BuildStatusCache statusCache;

            /* Add in the in-memory list of watched builds */
            synchronized (watchedBuildLock) {
                /*
                 * Compose a list of build ids that have been removed from the
                 * on-disk cache. (Ie, they are currently in memory, but are not
                 * in the on-disk cache, meaning that another process has
                 * removed this from the watch list.) Create the list by taking
                 * all watched builds in-memory and removing those on disk. The
                 * remainder is builds that were removed externally.
                 */
                final List<Integer> externallyRemovedList = new ArrayList<Integer>();
                externallyRemovedList.addAll(watchedBuilds.keySet());

                /*
                 * Synchronize with the list of data on-disk (another process
                 * may have modified this list.) Assume the build status cache
                 * is the canonical list of watched build IDs. (It will always
                 * include any modifications made in this client, or in any
                 * other client.)
                 */
                statusCache = BuildStatusCache.load(connection);
                queryIdList.addAll(statusCache.getBuilds());

                /*
                 * Remove any items in our in-memory list that did not exist on
                 * disk.
                 */
                externallyRemovedList.removeAll(queryIdList);

                for (final Iterator<Integer> i = externallyRemovedList.iterator(); i.hasNext();) {
                    final Integer removedId = i.next();

                    log.debug(
                        MessageFormat.format(
                            "Queued build id {0} no longer watched, removed by an external process", //$NON-NLS-1$
                            ("" + removedId))); //$NON-NLS-1$

                    watchRemovedList.add(watchedBuilds.remove(removedId));
                }
            }

            if (log.isDebugEnabled()) {
                if (queryIdList.size() == 0) {
                    log.debug("No watched builds to query"); //$NON-NLS-1$
                } else {
                    final StringBuffer buildIdDebugList = new StringBuffer();

                    for (int i = 0; i < queryIdList.size(); i++) {
                        buildIdDebugList.append(((i > 0) ? ", " : "") + queryIdList.get(i)); //$NON-NLS-1$ //$NON-NLS-2$
                    }

                    log.debug("Querying build status for queued build ids: " + buildIdDebugList.toString()); //$NON-NLS-1$
                }
            }

            if (queryIdList.size() > 0) {
                /* Query the server */
                final int[] queryIds = new int[queryIdList.size()];
                for (int i = 0; i < queryIdList.size(); i++) {
                    queryIds[i] = queryIdList.get(i).intValue();
                }

                /*
                 * Query with definitions so that we can get the definition name
                 * for the QueuedBuildsTableControl
                 */
                final IQueuedBuild[] queuedBuilds =
                    connection.getBuildServer().getQueuedBuild(queryIds, QueryOptions.DEFINITIONS);
                final List<IQueuedBuild> savedQueuedBuilds = new ArrayList<IQueuedBuild>();

                /*
                 * Store new queued build data, see if the data has changed
                 * since the last refresh
                 */
                synchronized (watchedBuildLock) {
                    for (int i = 0; i < queuedBuilds.length; i++) {
                        /*
                         * The server no longer has this build detail. This
                         * could be because the retention policy removed it or
                         * it was removed manually.
                         */
                        if (queuedBuilds[i].getID() == 0
                            || queuedBuilds[i].getBuild() == null && queuedBuilds[i].getStatus() == null) {
                            log.debug(
                                MessageFormat.format(
                                    "Watched build id {0} is no longer on the server, will no longer be watched", //$NON-NLS-1$
                                    queryIds[i]));

                            final IQueuedBuild existingQueuedBuildData = watchedBuilds.remove(queryIds[i]);

                            if (existingQueuedBuildData != null) {
                                watchRemovedList.add(existingQueuedBuildData);
                            }

                            continue;
                        }

                        savedQueuedBuilds.add(queuedBuilds[i]);

                        final IQueuedBuild existingQueuedBuildData = watchedBuilds.get(queuedBuilds[i].getID());

                        /*
                         * We do not have existing data, this was added by
                         * another process
                         */
                        if (existingQueuedBuildData == null) {
                            watchAddedList.add(queuedBuilds[i]);
                            watchedBuilds.put(queuedBuilds[i].getID(), queuedBuilds[i]);
                        }
                        /*
                         * If the build status has changed, notify listeners and
                         * update the cache with the newest data.
                         */
                        else if (buildStatusChanged(existingQueuedBuildData, queuedBuilds[i])) {
                            statusChangedList.add(queuedBuilds[i]);
                            watchedBuilds.put(queuedBuilds[i].getID(), queuedBuilds[i]);
                        }
                    }
                }

                /*
                 * Load the checkin details build information node for any newly
                 * added or status changed builds who have completed building.
                 */
                final List<IQueuedBuild> loadInformationList = new ArrayList<IQueuedBuild>();
                loadInformationList.addAll(watchAddedList);
                loadInformationList.addAll(statusChangedList);
                for (final IQueuedBuild queuedBuild : loadInformationList) {
                    if (queuedBuild.getBuild() == null || queuedBuild.getBuild().getStatus() == null) {
                        continue;
                    }

                    final IBuildDetail buildDetail = queuedBuild.getBuild();
                    final BuildStatus buildStatus = buildDetail.getStatus();

                    if ((buildStatus.contains(BuildStatus.PARTIALLY_SUCCEEDED)
                        || buildStatus.contains(BuildStatus.SUCCEEDED))
                        && (buildDetail.getInformation() == null
                            || buildDetail.getInformation().getNodesByType(
                                InformationTypes.CHECK_IN_OUTCOME).length == 0)) {
                        buildDetail.refresh(new String[] {
                            InformationTypes.CHECK_IN_OUTCOME
                        }, QueryOptions.ALL);
                    }
                }

                /* Persist changes to disk */
                statusCache.setBuilds(savedQueuedBuilds.toArray(new IQueuedBuild[savedQueuedBuilds.size()]));
                statusCache.save(connection);
            }

            /* Notify listeners of new / removed watched builds */
            if (statusChangedList.size() > 0 || watchAddedList.size() > 0 || watchRemovedList.size() > 0) {
                ((BuildStatusManagerListener) listeners.getListener()).onUpdateStarted();

                for (final IQueuedBuild removedBuild : watchRemovedList) {
                    ((BuildStatusManagerListener) listeners.getListener()).onWatchedBuildRemoved(removedBuild);
                }

                for (final IQueuedBuild addedBuild : watchAddedList) {
                    ((BuildStatusManagerListener) listeners.getListener()).onWatchedBuildAdded(addedBuild);
                }

                for (final IQueuedBuild changedBuild : statusChangedList) {
                    ((BuildStatusManagerListener) listeners.getListener()).onBuildStatusChanged(changedBuild);
                }

                ((BuildStatusManagerListener) listeners.getListener()).onUpdateFinished();
            }
        } finally {
            synchronized (refreshLock) {
                refreshInProgress = false;
                refreshLastTime = System.currentTimeMillis();
            }
        }
    }

    private boolean buildStatusChanged(final IQueuedBuild existingQueuedBuild, final IQueuedBuild newQueuedBuild) {
        Check.notNull(existingQueuedBuild, "existingQueuedBuild"); //$NON-NLS-1$
        Check.notNull(newQueuedBuild, "newQueuedBuild"); //$NON-NLS-1$

        /* If the build has not started, see if the queue status has changed */
        if (existingQueuedBuild.getBuild() == null && newQueuedBuild.getBuild() == null) {
            return (!existingQueuedBuild.getStatus().equals(newQueuedBuild.getStatus()));
        }

        /*
         * The build has started (an IBuildDetail exists in the new queued build
         * but not our cached data)
         */
        if (existingQueuedBuild.getBuild() == null && newQueuedBuild.getBuild() != null) {
            return true;
        }

        /* Otherwise, compare the old status with the new */
        return (!existingQueuedBuild.getBuild().getStatus().equals(newQueuedBuild.getBuild().getStatus()));
    }

    public boolean addWatchedBuild(final IQueuedBuild build) {
        Check.notNull(build, "build"); //$NON-NLS-1$

        boolean newWatchedBuild;

        synchronized (watchedBuildLock) {
            newWatchedBuild = (watchedBuilds.put(build.getID(), build) == null);
        }

        /* Only notify listeners if this build was not already being watched. */
        if (newWatchedBuild == true) {
            final BuildStatusCache statusCache = BuildStatusCache.load(connection);
            statusCache.addBuild(build);
            statusCache.save(connection);

            ((BuildStatusManagerListener) listeners.getListener()).onWatchedBuildAdded(build);
        }

        return newWatchedBuild;
    }

    /**
     * Removes the given {@link IBuildDetail} from the watched build list. This
     * is not guaranteed to succeed, as
     *
     * @param buildDetail
     * @return
     */
    public boolean removeWatchedBuild(final IBuildDetail buildDetail) {
        Check.notNull(buildDetail, "buildDetail"); //$NON-NLS-1$

        /*
         * Unfortunately, there's nothing tying an IBuildDetail to an
         * IQueuedBuild, so we have to go through the IQueuedBuilds to see if we
         * already have an IBuildDetail for them and get the queue id that way.
         */
        int buildId = -1;

        synchronized (watchedBuildLock) {
            for (final IQueuedBuild queuedBuild : watchedBuilds.values()) {
                if (queuedBuild.getBuild() == null) {
                    continue;
                }

                if (buildDetail.getBuildNumber().equals(queuedBuild.getBuild().getBuildNumber())) {
                    buildId = queuedBuild.getID();
                    break;
                }
            }
        }

        if (buildId >= 0) {
            return removeWatchedBuild(buildId);
        }

        return false;
    }

    public boolean removeWatchedBuild(final IQueuedBuild build) {
        Check.notNull(build, "build"); //$NON-NLS-1$

        return removeWatchedBuild(build.getID());
    }

    public boolean removeWatchedBuild(final int id) {
        IQueuedBuild existingWatchedBuild;

        synchronized (watchedBuildLock) {
            existingWatchedBuild = watchedBuilds.remove(id);
        }

        /* Only notify listeners if this build was being watched. */
        if (existingWatchedBuild != null) {
            final BuildStatusCache statusCache = BuildStatusCache.load(connection);
            statusCache.removeBuild(id);
            statusCache.save(connection);

            ((BuildStatusManagerListener) listeners.getListener()).onWatchedBuildRemoved(existingWatchedBuild);

            CodeMarkerDispatch.dispatch(CODEMARKER_WATCHED_BUILD_REMOVED);
            return true;
        } else {
            return false;
        }
    }

    public IQueuedBuild[] getWatchedBuilds() {
        synchronized (watchedBuildLock) {
            return watchedBuilds.values().toArray(new IQueuedBuild[watchedBuilds.size()]);
        }
    }

    /**
     * Stops the build status manager. Should be called when disconnecting from
     * a server.
     */
    public void stop() {
        synchronized (refreshLock) {
            refreshInBackground = false;
        }
    }

    public void addListener(final BuildStatusManagerListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        listeners.addListener(listener);
    }

    public void removeListener(final BuildStatusManagerListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        listeners.removeListener(listener);
    }

    private class BuildStatusManagerRefreshWorker implements Runnable {
        private final Log log = LogFactory.getLog(BuildStatusManagerRefreshWorker.class);

        @Override
        public void run() {
            log.info("Starting build status refresh worker"); //$NON-NLS-1$

            while (true) {
                boolean doRefresh = false;
                long sleepTime = 0;

                synchronized (refreshLock) {
                    /*
                     * The user may have cancelled this background job. This
                     * could be because the server is now invalid
                     * (disconnected.)
                     */
                    if (refreshInBackground == false) {
                        log.info("Stopping build status refresh worker"); //$NON-NLS-1$
                        break;
                    }

                    /*
                     * If another refresh is not in progress and we're past time
                     * for a refresh, then schedule one.
                     */
                    final long currentTime = System.currentTimeMillis();

                    if (refreshInProgress) {
                        /*
                         * If there's a refresh going on (manual refresh) defer
                         * until the refresh interval and check again
                         */
                        sleepTime = refreshInterval;
                    } else if (refreshLastTime + refreshInterval <= currentTime) {
                        /* Otherwise, it's time to do a refresh */
                        doRefresh = true;
                        refreshInProgress = true;
                        sleepTime = refreshInterval;
                    } else {
                        /*
                         * Otherwise, we got woken up before a refresh was due,
                         * simply sleep until we think it's time to do the
                         * refresh
                         */
                        sleepTime = (refreshLastTime + refreshInterval) - currentTime;
                    }
                }

                if (doRefresh) {
                    startRefreshJob();
                }

                try {
                    Thread.sleep(sleepTime);
                } catch (final InterruptedException e) {
                    log.warn("Build status refresh worker interrupted", e); //$NON-NLS-1$

                    break;
                }
            }
        }

        private void startRefreshJob() {
            final Job refreshJob = new Job(Messages.getString("BuildStatusManager.BuildStatusRefreshJobName")) //$NON-NLS-1$
            {
                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    try {
                        refreshInternal();
                    } catch (final Exception e) {
                        log.debug("Exception during auto-refresh.", e); //$NON-NLS-1$
                    }

                    return Status.OK_STATUS;
                }
            };

            refreshJob.schedule();
        }
    }
}
