// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.decorators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter.CompositeResourceFilterType;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilters;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.repository.cache.conflict.ConflictCacheEvent;
import com.microsoft.tfs.client.common.repository.cache.conflict.ConflictCacheListener;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheEvent;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheListener;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManagerListener;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceData;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceDataListener;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceDataManager;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class TFSLabelDecorator extends LabelProvider implements ILightweightLabelDecorator {
    /**
     * The decorator ID (must match the ID specified in the plugin.xml).
     */
    private static final String DECORATOR_ID = "com.microsoft.tfs.client.eclipse.ui.decorators.LabelDecorator"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(TFSLabelDecorator.class);

    // the icon to display for files which are pending adds/edits/locks/etc
    private static final String EDIT_ICON = "images/decorations/overlay_edit.gif"; //$NON-NLS-1$
    private static final String CHANGE_ICON = "images/decorations/overlay_edit.gif"; //$NON-NLS-1$
    private static final String LOCK_ICON = "images/decorations/overlay_locked.gif"; //$NON-NLS-1$
    private static final String ADD_ICON = "images/decorations/overlay_add.gif"; //$NON-NLS-1$
    private static final String UNKNOWN_ICON = "images/decorations/overlay_unknown.gif"; //$NON-NLS-1$
    private static final String TFS_ICON = "images/decorations/overlay_tfs.gif"; //$NON-NLS-1$
    private static final String TFS_OFFLINE_ICON = "images/decorations/overlay_tfs_offline.gif"; //$NON-NLS-1$
    private static final String IGNORED_ICON = "images/decorations/overlay_ignored.gif"; //$NON-NLS-1$

    private final CompositeResourceFilter eclipseIgnoreFilter = eclipseIgnoreFilter();

    private final ImageHelper imageHelper = new ImageHelper(TFSEclipseClientUIPlugin.PLUGIN_ID);

    // list of known tfsrepositories
    private final List<TFSRepository> tfsRepositories = new ArrayList<TFSRepository>();

    // listeners
    private final LabelDecoratorRepositoryListener repositoryListener = new LabelDecoratorRepositoryListener();
    private final LabelDecoratorResourceDataListener resourceDataListener = new LabelDecoratorResourceDataListener();
    private final LabelDecoratorPropertyChangeListener propertyChangeListener =
        new LabelDecoratorPropertyChangeListener();
    private final LabelDecoratorRepositoryManagerListener repositoryManagerListener =
        new LabelDecoratorRepositoryManagerListener();
    private final LabelDecoratorProjectManagerListener projectManagerListener =
        new LabelDecoratorProjectManagerListener();
    private final LabelDecoratorWorkspaceUpdatedListener workspaceUpdatedListener =
        new LabelDecoratorWorkspaceUpdatedListener();

    private final boolean decorateParentPendingChanges = true;

    private volatile boolean listenersHooked;

    public TFSLabelDecorator() {
        tryHookListeners();
    }

    /**
     * Hooks listeners to refresh label decoration when preferences change or
     * when repositories are connected/disconnected. Safe to call multiple
     * times.
     *
     * Construction of this class and label decoration itself may be triggered
     * before our dependent plugins are started, so this method may return
     * without having hooked listeners. So call this method often to ensure the
     * listeners are hooked.
     */
    private void tryHookListeners() {
        if (listenersHooked) {
            return;
        }

        // We can be called so early our dependencies aren't loaded.
        if (TFSCommonUIClientPlugin.getDefault() == null
            || TFSEclipseClientPlugin.getDefault() == null
            || !TFSEclipseClientPlugin.getDefault().getProjectManager().isStarted()) {
            log.debug("TFSEclipseClientPlugin not available yet, deferring repository hooks"); //$NON-NLS-1$
            return;
        }

        /*
         * Hook up a preference store listener: this will allow us to refresh
         * when our preferences are changed.
         */
        TFSCommonUIClientPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(propertyChangeListener);

        /*
         * Hook up a listener for online/offline events to catch new projects.
         */
        final ProjectRepositoryManager projectManager = TFSEclipseClientPlugin.getDefault().getProjectManager();
        projectManager.addListener(projectManagerListener);

        final IProject[] projects = projectManager.getProjectsOfStatus(ProjectRepositoryStatus.ONLINE);

        for (int i = 0; i < projects.length; i++) {
            final TFSRepository repository = projectManager.getRepository(projects[i]);

            /* Project could have gone offline in another thread. */
            if (repository != null) {
                addRepositoryListener(repository);
            }
        }

        /*
         * Hook up a listener for when repositories go offline. This will simply
         * allow us to unhook our events.
         */
        TFSEclipseClientPlugin.getDefault().getRepositoryManager().addListener(repositoryManagerListener);

        /* Hook up a listener for resource data events */
        TFSEclipseClientPlugin.getDefault().getResourceDataManager().addListener(resourceDataListener);

        listenersHooked = true;
    }

    public void close() {
        synchronized (tfsRepositories) {
            for (final Iterator<TFSRepository> i = tfsRepositories.iterator(); i.hasNext();) {
                removeRepositoryListener(i.next());
            }
        }

        TFSCommonUIClientPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(propertyChangeListener);

        TFSEclipseClientPlugin.getDefault().getRepositoryManager().removeListener(repositoryManagerListener);
        TFSEclipseClientPlugin.getDefault().getProjectManager().removeListener(projectManagerListener);
        TFSEclipseClientPlugin.getDefault().getResourceDataManager().removeListener(resourceDataListener);
    }

    /**
     * Hooks up invalidation listeners to a repository. Basically, whenever
     * resources change, we need to invalidate our label decorations for them.
     *
     * @param repository
     * @return
     */
    private void addRepositoryListener(final TFSRepository repository) {
        synchronized (tfsRepositories) {
            if (tfsRepositories.contains(repository)) {
                return;
            }

            // hook up repository listeners
            repository.getPendingChangeCache().addListener(repositoryListener);
            repository.getConflictManager().addConflictAddedListener(repositoryListener);
            repository.getConflictManager().addConflictModifiedListener(repositoryListener);
            repository.getConflictManager().addConflictRemovedListener(repositoryListener);

            repository.getWorkspace().getClient().getEventEngine().addWorkspaceUpdatedListener(
                workspaceUpdatedListener);

            tfsRepositories.add(repository);
        }

        refresh();
    }

    private void removeRepositoryListener(final TFSRepository repository) {
        synchronized (tfsRepositories) {
            repository.getPendingChangeCache().removeListener(repositoryListener);
            repository.getConflictManager().removeConflictAddedListener(repositoryListener);
            repository.getConflictManager().removeConflictModifiedListener(repositoryListener);
            repository.getConflictManager().removeConflictRemovedListener(repositoryListener);

            repository.getWorkspace().getClient().getEventEngine().removeWorkspaceUpdatedListener(
                workspaceUpdatedListener);

            tfsRepositories.remove(repository);
        }

        refresh();
    }

    /**
     * This creates a CompositeResourceFilter suitable for filtering based on
     * Eclipse ignored resources (linked, private or team ignored.)
     *
     * Note that this differs from {@link PluginResourceFilters#STANDARD_FILTER}
     * as it does not include .tpignore checks or plugin ignored resource
     * checks. This is so that we can potentially decorate TFS ignored resources
     * differently than Eclipse ignored resources.
     *
     * @return An IResourceFilter which will filter eclipse ignored resources.
     */
    private static CompositeResourceFilter eclipseIgnoreFilter() {
        final CompositeResourceFilter.Builder builder =
            new CompositeResourceFilter.Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT);

        // filter out non-tfs resources and ignored eclipse resources
        builder.addFilter(PluginResourceFilters.TEAM_IGNORED_RESOURCES_FILTER);
        builder.addFilter(ResourceFilters.TEAM_PRIVATE_RESOURCES_FILTER);

        return builder.build();
    }

    /**
     * This allows us to decorate either the label or the icon used for an
     * IResource.
     *
     * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object,
     *      org.eclipse.jface.viewers.IDecoration)
     */
    @Override
    public void decorate(final Object element, final IDecoration decoration) {
        tryHookListeners();

        // should never happen
        if (element == null || !(element instanceof IResource)) {
            return;
        }

        final IResource resource = (IResource) element;

        // if the resource is not in an open project, we don't decorate
        if (resource.getProject() == null || resource.getProject().isOpen() == false) {
            return;
        }

        // make sure that TFS is the repository provider for this resource
        if (!TeamUtils.isConfiguredWith(resource, TFSRepositoryProvider.PROVIDER_ID)) {
            return;
        }

        // ignore linked, private, team ignored resources
        if (eclipseIgnoreFilter.filter(resource).isReject()) {
            // add ignored suffix if enabled
            if (TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
                UIPreferenceConstants.LABEL_DECORATION_SHOW_IGNORED_STATUS)) {
                decoration.addSuffix(Messages.getString("TFSLabelDecorator.IgnoredDecorationSuffix")); //$NON-NLS-1$
            }

            return;
        }

        // try to get the repository for this resource
        final TFSRepositoryProvider repositoryProvider =
            (TFSRepositoryProvider) TeamUtils.getRepositoryProvider(resource);
        final TFSRepository repository = repositoryProvider.getRepository();

        // hook up any listeners, etc, if necessary
        if (repository != null) {
            addRepositoryListener(repository);
        }

        // try to get the full path for this resource
        final String resourcePath = Resources.getLocation(resource, LocationUnavailablePolicy.IGNORE_RESOURCE);

        final ResourceDataManager resourceDataManager = TFSEclipseClientPlugin.getDefault().getResourceDataManager();
        final ResourceData resourceData = resourceDataManager.getResourceData(resource);

        // decorate ignored resources with the ignored iconography
        if (PluginResourceFilters.TPIGNORE_FILTER.filter(resource).isReject()
            || PluginResourceFilters.TFS_IGNORE_FILTER.filter(resource).isReject()) {
            // add ignored suffix if enabled
            if (TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
                UIPreferenceConstants.LABEL_DECORATION_SHOW_IGNORED_STATUS)) {
                decoration.addSuffix(Messages.getString("TFSLabelDecorator.IgnoredDecorationSuffix")); //$NON-NLS-1$
            }

            // if this is in TFS, paint this as in-tfs but ignored
            if (resourceData != null) {
                decoration.addOverlay(imageHelper.getImageDescriptor(IGNORED_ICON));
            }

            // not in tfs, draw no iconography
            return;
        }

        // repository is offline
        if (repository == null) {
            decorateFromFilesystem(resource, decoration, repository);
            return;
        }

        // there are two passes here: first we update any information in the
        // pending changes cache, then we try to update information from the
        // itemcache. (if that fails, we use the readonly/writable hackery
        // from 2.0.) we draw high-priority overlays first, since the first
        // one wins.)

        /*
         * Add conflicts once conflict cache is considered reliable.
         */
        // decorateFromConflicts(resource, decoration, repository,
        // resourcePath);

        // add decorations from pending changes
        decorateFromPendingChanges(resource, decoration, repository, resourcePath);

        resource.getProject().getLocation().toOSString();

        if (resourceData != null) {
            decorateFromResourceData(resource, decoration, resourceData);
        } else if (!resourceDataManager.hasCompletedRefresh(resource.getProject())) {
            decorateFromFilesystem(resource, decoration, repository);
        } else if (resource instanceof IProject) {
            decoration.addOverlay(imageHelper.getImageDescriptor(TFS_ICON));
        }
    }

    /**
     * This decorator allows us to paint based on the pending changes cache. (it
     * will decorate adds, checkouts, locks, etc.)
     *
     * @param resource
     *        The IResource to decorate
     * @param decoration
     *        The IDecoration which will be decorated
     * @param repository
     *        The TFSRepository for this item
     * @param resourcePath
     *        The local path to the resource for querying the pending changes
     *        cache
     */
    private void decorateFromPendingChanges(
        final IResource resource,
        final IDecoration decoration,
        final TFSRepository repository,
        final String resourcePath) {
        if (resourcePath == null) {
            return;
        } else if (resource.getType() == IResource.FILE) {
            decorateFromFilePendingChanges(resource, decoration, repository, resourcePath);
        } else if (decorateParentPendingChanges) {
            decorateFromFolderPendingChanges(resource, decoration, repository, resourcePath);
        }
    }

    /**
     * This decorator allows us to paint based on the pending changes cache. (it
     * will decorate adds, checkouts, locks, etc.)
     *
     * @param resource
     *        The IResource to decorate
     * @param decoration
     *        The IDecoration which will be decorated
     * @param repository
     *        The TFSRepository for this item
     * @param resourcePath
     *        The local path to the resource for querying the pending changes
     *        cache
     */
    private void decorateFromFilePendingChanges(
        final IResource resource,
        final IDecoration decoration,
        final TFSRepository repository,
        final String resourcePath) {
        final PendingChange pendingChange =
            repository.getPendingChangeCache().getPendingChangeByLocalPath(resourcePath);

        // no pending changes, don't alter any decorations
        if (pendingChange == null || pendingChange.getChangeType() == null) {
            return;
        }

        final ChangeType pendingChangeType = pendingChange.getChangeType();

        // handle adds
        if (pendingChangeType.contains(ChangeType.ADD)) {
            decoration.addOverlay(imageHelper.getImageDescriptor(ADD_ICON));
            decoration.addPrefix("+"); //$NON-NLS-1$
        }
        // edits
        else if (pendingChangeType.contains(ChangeType.EDIT)) {
            decoration.addOverlay(imageHelper.getImageDescriptor(EDIT_ICON));
            decoration.addPrefix(">"); //$NON-NLS-1$
        }
        // non-edit locks
        else if (pendingChangeType.contains(ChangeType.LOCK)) {
            decoration.addOverlay(imageHelper.getImageDescriptor(LOCK_ICON));
        }
        // arbitrary pending changes just get a something
        else if (!pendingChangeType.isEmpty()) {
            decoration.addOverlay(imageHelper.getImageDescriptor(CHANGE_ICON));
        }
    }

    /**
     * This decorator allows us to paint based on the pending changes cache. (it
     * will decorate adds, checkouts, locks, etc.)
     *
     * @param resource
     *        The IResource to decorate
     * @param decoration
     *        The IDecoration which will be decorated
     * @param repository
     *        The TFSRepository for this item
     * @param resourcePath
     *        The local path to the resource for querying the pending changes
     *        cache
     */
    private void decorateFromFolderPendingChanges(
        final IResource resource,
        final IDecoration decoration,
        final TFSRepository repository,
        final String resourcePath) {
        if (repository.getPendingChangeCache().hasPendingChangesByLocalPathRecursive(resourcePath) == false) {
            return;
        }

        decoration.addOverlay(imageHelper.getImageDescriptor(CHANGE_ICON));
    }

    /**
     * Decorate a resource based on data in the ResourceDataManager. (This
     * includes knowledge of whether the resource is actually in TFS, local
     * version, changeset date, etc.)
     *
     * @param resource
     *        The IResource to decorate
     * @param decoration
     *        The IDecoration which will be decorated
     * @param resourceData
     *        The {@link ResourceData} for this local resource
     */
    private void decorateFromResourceData(
        final IResource resource,
        final IDecoration decoration,
        final ResourceData resourceData) {
        final List<String> suffixList = new ArrayList<String>();

        // there's no item cache data (but the item cache was updated, above)
        // so there's nothing to decorate
        if (resourceData == null) {
            return;
        }

        // indicate that this file is in tfs
        decoration.addOverlay(imageHelper.getImageDescriptor(TFS_ICON));

        // These preferences affect the decoration string
        final IPreferenceStore preferenceStore = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        // only show enhanced decorations for files, unless otherwise configured
        if (resource.getType() == IResource.FILE
            || preferenceStore.getBoolean(UIPreferenceConstants.LABEL_DECORATION_DECORATE_FOLDERS)) {
            // optionally show local changeset
            if (preferenceStore.getBoolean(UIPreferenceConstants.LABEL_DECORATION_SHOW_CHANGESET)
                && resourceData.getChangesetID() != 0) {
                suffixList.add(Integer.toString(resourceData.getChangesetID()));
            }

            if (preferenceStore.getBoolean(UIPreferenceConstants.LABEL_DECORATION_SHOW_SERVER_ITEM)) {
                suffixList.add(resourceData.getServerItem());
            }

            // pretty up any suffix decorations
            if (suffixList.size() > 0) {
                final StringBuffer suffix = new StringBuffer();

                suffix.append(" ["); //$NON-NLS-1$
                for (int i = 0; i < suffixList.size(); i++) {
                    if (i > 0) {
                        suffix.append(", "); //$NON-NLS-1$
                    }

                    suffix.append(suffixList.get(i));
                }
                suffix.append("]"); //$NON-NLS-1$

                decoration.addSuffix(suffix.toString());
            }
        }
    }

    /**
     * This is a fallback decorator for when the ItemCache is not available
     * (hasn't yet completed refreshing.) This is very similar to the 2.x
     * LabelDecorator.
     *
     * @param resource
     *        The IResource to decorate
     * @param decoration
     *        The IDecoration which will be deecorated
     */
    private void decorateFromFilesystem(
        final IResource resource,
        final IDecoration decoration,
        final TFSRepository repository) {
        // repository does not exist, we are offline
        if (repository == null) {
            decoration.addOverlay(imageHelper.getImageDescriptor(TFS_OFFLINE_ICON));
        } else if (resource.getType() == IResource.FILE && resource.isReadOnly()) {
            decoration.addOverlay(imageHelper.getImageDescriptor(TFS_ICON));
        } else if (resource.getType() == IResource.FILE) {
            decoration.addOverlay(imageHelper.getImageDescriptor(UNKNOWN_ICON));
        } else {
            decoration.addOverlay(imageHelper.getImageDescriptor(TFS_ICON));
        }
    }

    /**
     * Refreshes labels for the entire workspace on the workbench's active
     * {@link TFSLabelDecorator} (if there is one).
     *
     * @throws CoreException
     *         if there was an error collecting folder contents from a given
     *         resource
     */
    public static void refreshTFSLabelDecorator() {
        final IBaseLabelProvider decorator =
            PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(TFSLabelDecorator.DECORATOR_ID);

        // It's possible the decorator is not yet available
        if (decorator == null || decorator instanceof TFSLabelDecorator == false) {
            log.debug("Couldn't find TFSLabelDecorator for refresh"); //$NON-NLS-1$
            return;
        }

        ((TFSLabelDecorator) decorator).refresh();
    }

    /**
     * Refresh the label decorations for every resource in the Eclipse
     * workspace.
     */
    private void refresh() {
        refresh(new LabelProviderChangedEvent(this));
    }

    /**
     * Refresh the label decorations for particular resource.
     *
     * @param resource
     *        The IResource to refresh
     */
    private void refresh(final IResource resource) {
        refresh(new LabelProviderChangedEvent(this, resource));
    }

    /**
     * Refresh the label decorations for particular resource.
     *
     * @param resource
     *        The IResource to refresh
     */
    private void refresh(final IResource[] resources) {
        refresh(new LabelProviderChangedEvent(this, resources));
    }

    /**
     * Refresh on the UI thread.
     *
     * @param event
     *        The event to fire on the UI thread
     */
    private void refresh(final LabelProviderChangedEvent event) {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                fireLabelProviderChanged(event);
            }
        });
    }

    private class LabelDecoratorResourceDataListener implements ResourceDataListener {
        @Override
        public void resourceDataChanged(final IResource[] resources) {
            refresh(resources);
        }
    }

    private class LabelDecoratorPropertyChangeListener implements IPropertyChangeListener {
        /**
         * Respond to property changes -- ie, the user may wish to change the
         * way the labeldecorator displays.
         *
         * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
         */
        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            if (event.getProperty().startsWith(UIPreferenceConstants.LABEL_DECORATION_PREF_PREFIX)) {
                refresh();
            }
        }
    }

    /**
     * Handles events from TFSRepositories and various other classes contained
     * therein (ItemCache, PendingChangeCache, etc.)
     */
    private class LabelDecoratorRepositoryListener implements PendingChangeCacheListener, ConflictCacheListener {
        private final Object refreshLock = new Object();
        private int refreshDefer = 0;
        private boolean refreshWorkspace = false;

        private final List<IResource> resourceList = new ArrayList<IResource>();

        @Override
        public void onConflictEvent(final ConflictCacheEvent event) {
            final List<IResource> resources = new ArrayList<IResource>();

            final Conflict conflict = event.getConflict();

            if (conflict.getSourceLocalItem() != null) {
                final IResource resource =
                    getResourceFromItem(conflict.getSourceLocalItem(), conflict.getYourItemType());

                if (resource != null) {
                    resources.add(resource);
                }
            }

            if (conflict.getTargetLocalItem() != null) {
                final IResource resource =
                    getResourceFromItem(conflict.getTargetLocalItem(), conflict.getYourItemType());

                if (resource != null) {
                    resources.add(resource);
                }
            }

            refresh(resources.toArray(new IResource[resources.size()]));
        }

        /**
         * Hook to batch up pending change notifications - we don't notify the
         * LabelDecorator of changes until onAfterUpdatePendingChanges...
         *
         * @see com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheListener#onBeforeUpdatePendingChanges(com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheEvent)
         */
        @Override
        public void onBeforeUpdatePendingChanges(final PendingChangeCacheEvent event) {
            synchronized (refreshLock) {
                refreshDefer++;
            }
        }

        /**
         * Handle pending change completed action - send out our batch of
         * resource updates.
         *
         * @see com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheListener#onAfterUpdatePendingChanges(com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheEvent)
         */
        @Override
        public void onAfterUpdatePendingChanges(
            final PendingChangeCacheEvent event,
            final boolean modifiedDuringOperation) {
            boolean fullRefresh = false;
            IResource[] resources = null;

            synchronized (refreshLock) {
                if (--refreshDefer < 0) {
                    refreshDefer = 0;
                }

                if (refreshDefer > 0) {
                    return;
                }

                if (refreshWorkspace) {
                    fullRefresh = true;
                } else {
                    resources = resourceList.toArray(new IResource[resourceList.size()]);
                }

                resourceList.clear();
                refreshWorkspace = false;
            }

            if (fullRefresh) {
                refresh();
            } else if (resources != null && resources.length > 0) {
                refresh(resources);
            }
        }

        @Override
        public void onPendingChangeAdded(final PendingChangeCacheEvent event) {
            onPendingChangeEvent(event);
        }

        @Override
        public void onPendingChangeModified(final PendingChangeCacheEvent event) {
            onPendingChangeEvent(event);
        }

        @Override
        public void onPendingChangeRemoved(final PendingChangeCacheEvent event) {
            onPendingChangeEvent(event);
        }

        @Override
        public void onPendingChangesCleared(final PendingChangeCacheEvent event) {
            /*
             * The pending change cache is cleared only when the pending changes
             * are refreshed fully from the server. Invalidate all pending
             * changes (and thus label decorations) locally (the server may have
             * removed pending changes for us.)
             */
            synchronized (refreshLock) {
                if (refreshDefer > 0) {
                    refreshWorkspace = true;
                } else {
                    refresh();
                }
            }
        }

        private void onPendingChangeEvent(final PendingChangeCacheEvent event) {
            IResource oldResource = null, resource = null;

            // get the resource for the pending change(s)
            if (event.getOldPendingChange() != null) {
                oldResource = getResourceFromPendingChange(event.getOldPendingChange());
            }
            if (event.getPendingChange() != null) {
                resource = getResourceFromPendingChange(event.getPendingChange());
            }

            // refresh or queue the resources for update
            if (oldResource != null) {
                refreshOrQueue(oldResource);
            }
            if (resource != null) {
                refreshOrQueue(resource);
            }
        }

        private void refreshOrQueue(final IResource resource) {
            synchronized (refreshLock) {
                if (refreshDefer > 0) {
                    /*
                     * If we're already going to perform a full refresh, don't
                     * bother queueing
                     */
                    if (refreshWorkspace == false) {
                        resourceList.add(resource);
                    }
                } else {
                    refresh(resource);
                }
            }

            if (decorateParentPendingChanges && resource.getParent() != null) {
                refreshOrQueue(resource.getParent());
            }
        }

        private IResource getResourceFromPendingChange(final PendingChange pendingChange) {
            final String localItem = pendingChange.getLocalItem();

            final ResourceType resourceType =
                (ItemType.FILE == pendingChange.getItemType()) ? ResourceType.FILE : ResourceType.CONTAINER;

            if (localItem == null) {
                return null;
            }

            return Resources.getResourceForLocation(localItem, resourceType, false);
        }

        private IResource getResourceFromItem(final String localItem, final ItemType itemType) {
            final ResourceType resourceType = (ItemType.FILE == itemType) ? ResourceType.FILE : ResourceType.CONTAINER;

            return Resources.getResourceForLocation(localItem, resourceType, false);
        }
    }

    private final class LabelDecoratorRepositoryManagerListener extends RepositoryManagerAdapter {
        @Override
        public void onRepositoryRemoved(final RepositoryManagerEvent event) {
            removeRepositoryListener(event.getRepository());
        }
    }

    private final class LabelDecoratorProjectManagerListener implements ProjectRepositoryManagerListener {
        @Override
        public void onOperationStarted() {
        }

        @Override
        public void onProjectConnected(final IProject project, final TFSRepository repository) {
            addRepositoryListener(repository);

            refresh();
        }

        @Override
        public void onProjectDisconnected(final IProject project) {
            refresh();
        }

        @Override
        public void onProjectRemoved(final IProject project) {
            refresh();
        }

        @Override
        public void onOperationFinished() {
        }
    }

    private final class LabelDecoratorWorkspaceUpdatedListener implements WorkspaceUpdatedListener {
        @Override
        public void onWorkspaceUpdated(final WorkspaceUpdatedEvent e) {
            // Handle when the location changes. Original location will be null
            // if the event came from IPC, so don't consider that a location
            // change.
            if (e.getOriginalLocation() != null && e.getOriginalLocation() != e.getWorkspace().getLocation()) {
                refresh();
            }
        }
    }
}
