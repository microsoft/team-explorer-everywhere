// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.buildstatus;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.server.cache.buildstatus.BuildStatusManagerListener;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.BuildStatusNotificationDialog;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;

/**
 * Implements {@link BuildStatusManagerListener} in order to notify users via UI
 * popups that a build has completed. Contributed as an extension to the
 * teambuild plug-in.
 *
 * @threadsafety unknown
 */
public class BuildStatusAlerter implements BuildStatusManagerListener {
    private final static Log log = LogFactory.getLog(BuildStatusAlerter.class);

    private final Object lock = new Object();
    private int deferCount = 0;
    private final List<IQueuedBuild> completedBuilds = new ArrayList<IQueuedBuild>();

    public BuildStatusAlerter() {
    }

    @Override
    public void onUpdateStarted() {
        synchronized (lock) {
            deferCount++;
        }
    }

    @Override
    public void onUpdateFinished() {
        IQueuedBuild[] notifyBuilds = null;

        synchronized (lock) {
            deferCount--;

            if (deferCount == 0) {
                notifyBuilds = completedBuilds.toArray(new IQueuedBuild[completedBuilds.size()]);
                completedBuilds.clear();
            }
        }

        if (notifyBuilds != null) {
            for (int i = 0; i < notifyBuilds.length; i++) {
                notifyUser(notifyBuilds[i]);
            }
        }
    }

    @Override
    public void onWatchedBuildAdded(final IQueuedBuild watchedBuild) {
        /* We don't care when a build is being watched. */
    }

    @Override
    public void onWatchedBuildRemoved(final IQueuedBuild watchedBuild) {
        /* We don't care when a build is no longer being watched. */
    }

    @Override
    public void onBuildStatusChanged(final IQueuedBuild queuedBuild) {
        synchronized (lock) {
            if (deferCount > 0) {
                completedBuilds.add(queuedBuild);
                return;
            }
        }

        notifyUser(queuedBuild);
    }

    private void notifyUser(final IQueuedBuild queuedBuild) {
        /*
         * We only notify users for completed gated checkin builds. These must
         * be 2010 builds and must have been built from a shelveset.
         */

        final IBuildDetail detail = queuedBuild.getBuild();

        if (detail == null
            || !detail.isBuildFinished()
            || !detail.getReason().contains(BuildReason.CHECK_IN_SHELVESET)) {
            return;
        }

        /*
         * Investigate the build status to determine whether unshelve or
         * reconcile should be offered.
         */

        boolean notify = false;

        if ((detail.getStatus().contains(BuildStatus.SUCCEEDED)
            && getPreference(UIPreferenceConstants.BUILD_NOTIFICATION_SUCCESS) == true)
            || (detail.getStatus().contains(BuildStatus.PARTIALLY_SUCCEEDED)
                && getPreference(UIPreferenceConstants.BUILD_NOTIFICATION_PARTIALLY_SUCCEEDED) == true)
            || (detail.getStatus().contains(BuildStatus.FAILED)
                && getPreference(UIPreferenceConstants.BUILD_NOTIFICATION_FAILURE) == true)) {
            notify = true;
        }

        /* Nothing to do */
        if (!notify) {
            return;
        }

        final TFSServer server =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer();
        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

        if (server == null || repository == null) {
            log.warn("Notified of build completion, but currently working offline from TFS"); //$NON-NLS-1$
            return;
        }

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                final Shell workbenchShell = ShellUtils.getWorkbenchShell();

                if (workbenchShell == null) {
                    log.error("Could not locate workbench shell for build status notification"); //$NON-NLS-1$
                    return;
                }

                final BuildStatusNotificationDialog notificationDialog =
                    new BuildStatusNotificationDialog(workbenchShell);
                notificationDialog.setConnection(server.getConnection());
                notificationDialog.setQueuedBuild(queuedBuild);

                /*
                 * If an action was taken (return code is OK) then remove from
                 * the watched build list.
                 */
                if (notificationDialog.open() == IDialogConstants.OK_ID) {
                    server.getBuildStatusManager().removeWatchedBuild(queuedBuild);
                }

                /* Handle "never show again" preference */
                if (notificationDialog.getNeverShow()) {
                    final IPreferenceStore prefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

                    prefs.setValue(UIPreferenceConstants.BUILD_NOTIFICATION_SUCCESS, false);
                    prefs.setValue(UIPreferenceConstants.BUILD_NOTIFICATION_FAILURE, false);
                    prefs.setValue(UIPreferenceConstants.BUILD_NOTIFICATION_PARTIALLY_SUCCEEDED, false);
                }
            }
        });
    }

    private boolean getPreference(final String preferenceKey) {
        final IPreferenceStore prefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        if (prefs.contains(preferenceKey)) {
            return prefs.getBoolean(preferenceKey);
        } else {
            return prefs.getDefaultBoolean(preferenceKey);
        }
    }
}
