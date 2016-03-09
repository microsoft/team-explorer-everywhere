// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.RepositoryManagerListener;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.repository.TFSRepositoryUpdatedListener;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ItemNotMappedException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalItemExclusionEvaluator;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTableLock;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.util.Check;

/**
 * A {@link ResourceFilter} that uses the TFS 2012 exclusions lists stored on
 * the server and all applicable .tfignore files to filter items in local
 * workspaces.
 *
 * If used with a server workspace, the filter accepts all items to be
 * consistent with the core library behavior, which only ignores files for local
 * workspaces.
 *
 * @threadsafety thread-safe
 */
public class TFSIgnoreResourcesFilter extends ResourceFilter {
    private static final Log log = LogFactory.getLog(TFSIgnoreResourcesFilter.class);

    private final RepositoryUnavailablePolicy repositoryUnavailablePolicy;

    private volatile boolean repositoryManagerHooked;

    /**
     * Hooked to the current {@link TFSRepository} (and rehooked when that
     * changes) to clear out the cache when mappings change.
     */
    private final TFSRepositoryUpdatedListener repositoryUpdatedListener = new TFSRepositoryUpdatedListener() {
        @Override
        public void onRepositoryUpdated() {
            evaluators.clear();
        }

        @Override
        public void onGetCompletedEvent(final WorkspaceEventSource source) {
        }

        @Override
        public void onFolderContentChanged(final int changesetID) {
            // This comes from another process via IPC.
            evaluators.clear();
        }

        @Override
        public void onLocalWorkspaceScan(final WorkspaceEventSource source) {
        }
    };

    /**
     * A thread-safe map of local paths that are workspace mapping roots to
     * {@link LocalItemExclusionEvaluator}s configured with that path as the
     * start local item. Cleared when the plug-in's default workspace changes or
     * when the public {@link #clearCachedEvaluators()} is called.
     */
    private final Map<String, LocalItemExclusionEvaluator> evaluators =
        Collections.synchronizedMap(new TreeMap<String, LocalItemExclusionEvaluator>(String.CASE_INSENSITIVE_ORDER));

    /**
     * Constructs a {@link TFSIgnoreResourcesFilter}.
     *
     * @param repositoryUnavailablePolicy
     *        the policy to use when a {@link TFSRepository} for a resource
     *        cannot be found (must not be <code>null</code>)
     */
    public TFSIgnoreResourcesFilter(final RepositoryUnavailablePolicy repositoryUnavailablePolicy) {
        Check.notNull(repositoryUnavailablePolicy, "repositoryUnavailablePolicy"); //$NON-NLS-1$

        this.repositoryUnavailablePolicy = repositoryUnavailablePolicy;
    }

    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        if (resource.getType() == IResource.ROOT) {
            return ACCEPT;
        }

        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(resource.getProject());

        if (repository == null) {
            return repositoryUnavailablePolicy.acceptResourceWithNoRepository(resource) ? ACCEPT : REJECT;
        }

        // Only filter for local workspaces
        if (repository.getWorkspace().getLocation() != WorkspaceLocation.LOCAL) {
            return ACCEPT;
        }

        /*
         * We can't do this in the constructor because the plug-in might not
         * have finished starting.
         */
        ensureRepositoryManagerHooked();

        final String localPath = resource.getLocation().toOSString();

        /*
         * TODO Replace the infinite retry with something better (defer
         * decoration via another job?). We retry because it's common that a
         * auth dialog popup keeps another thread holding the metadata table
         * lock for greater than the normal timeout (~30 seconds, which is about
         * as long as it takes people to nagivate the login UI).
         *
         * This solution just blocks the decorator forever, which is probably
         * not a big deal.
         */
        WorkingFolder[] folders = null;
        while (folders == null) {
            try {
                folders = repository.getWorkspace().getFolders();
            } catch (final LocalMetadataTableLock.LocalMetadataTableTimeoutException e) {
                log.info("Timeout reading working folders while decorating labels, retrying"); //$NON-NLS-1$
                log.debug("Label decoration timeout exception details", e); //$NON-NLS-1$
            }
        }

        final String mappingRoot = getWorkspaceRoot(localPath, folders);

        if (mappingRoot == null) {
            // Should be very rare
            log.warn(MessageFormat.format(
                "No mapping root found for local item {0}, using default repository unavailable policy", //$NON-NLS-1$
                localPath));

            return repositoryUnavailablePolicy.acceptResourceWithNoRepository(resource) ? ACCEPT : REJECT;
        }

        try {
            // See if there's a cached evalutor for this root
            LocalItemExclusionEvaluator evaluator = evaluators.get(mappingRoot);
            if (evaluator == null) {
                // Use the mapping root, not the IProject, to support
                // mappings above the Eclipse workspace.
                evaluator = new LocalItemExclusionEvaluator(repository.getWorkspace(), mappingRoot);
                evaluators.put(mappingRoot, evaluator);
            }

            if (evaluator.isExcluded(localPath, resource.getType() == IFolder.FOLDER)) {
                return REJECT;
            }
        } catch (final ItemNotMappedException e) {
            log.info(MessageFormat.format("Error checking if resource {0} is excluded", resource), e); //$NON-NLS-1$
        }

        return ACCEPT;
    }

    public void clearCachedEvaluators() {
        evaluators.clear();
    }

    /**
     * Adds a listener to the repository manager which can attach more listeners
     * for when workspaces change. The repository manager listener is never
     * removed because the manager never goes away and this filter is expected
     * to be used in a static fashion (never go away).
     */
    private void ensureRepositoryManagerHooked() {
        if (repositoryManagerHooked) {
            return;
        }

        final TFSEclipseClientPlugin plugin = TFSEclipseClientPlugin.getDefault();
        if (plugin == null) {
            return;
        }

        /*
         * When the active repository (workspace) changes, remove the old
         * mappings listener and hook up a new one.
         */
        plugin.getRepositoryManager().addListener(new RepositoryManagerListener() {
            @Override
            public void onRepositoryRemoved(final RepositoryManagerEvent event) {
                evaluators.clear();
                event.getRepository().removeRepositoryUpdatedListener(repositoryUpdatedListener);
            }

            @Override
            public void onRepositoryAdded(final RepositoryManagerEvent event) {
                evaluators.clear();
                event.getRepository().addRepositoryUpdatedListener(repositoryUpdatedListener);
            }

            @Override
            public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
                // Added/remove events cover this.
            }
        });

        /*
         * Add the listener to the current default repository (since we probably
         * missed the last "added" event).
         */
        final TFSRepository defaultRepository = plugin.getRepositoryManager().getDefaultRepository();
        if (defaultRepository != null) {
            defaultRepository.addRepositoryUpdatedListener(repositoryUpdatedListener);
        }

        repositoryManagerHooked = true;
    }

    private String getWorkspaceRoot(final String localPath, final WorkingFolder[] workingFolders) {
        for (final String workspaceRoot : WorkingFolder.getWorkspaceRoots(workingFolders)) {
            if (LocalPath.isChild(workspaceRoot, localPath)) {
                return workspaceRoot;
            }
        }

        return null;
    }
}
