// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnoreCache;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.util.Check;

/**
 * Adapts the non-filter {@link TPIgnoreCache} (.tpignore file) into a
 * {@link ResourceFilter}.
 *
 * Please use the singleton instance
 * {@link PluginResourceFilters#TPIGNORE_FILTER} instead of constructing new
 * instances so the .tpignore file cache can be most effective.
 *
 * @threadsafety unknown
 */
public class TPIgnoreResourcesFilter extends ResourceFilter {
    private final RepositoryUnavailablePolicy repositoryUnavailablePolicy;
    private final TPIgnoreCache ignorableResourcesCache;

    /**
     * Creates a new {@link TPIgnoreResourcesFilter} which uses a new
     * {@link TPIgnoreCache} instance.
     *
     * Please use the singleton instance
     * {@link PluginResourceFilters#TPIGNORE_FILTER} so the .tpignore file cache
     * can be most effective.
     */
    public TPIgnoreResourcesFilter(final RepositoryUnavailablePolicy repositoryUnavailablePolicy) {
        Check.notNull(repositoryUnavailablePolicy, "repositoryUnavailablePolicy"); //$NON-NLS-1$

        this.repositoryUnavailablePolicy = repositoryUnavailablePolicy;
        this.ignorableResourcesCache = new TPIgnoreCache();
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

        // Only filter for server workspaces
        if (repository.getWorkspace().getLocation() != WorkspaceLocation.SERVER) {
            return ACCEPT;
        }

        return ignorableResourcesCache.matchesAnyPattern(resource) ? REJECT : ACCEPT;
    }

    public TPIgnoreCache getIgnorableResourcesCache() {
        return ignorableResourcesCache;
    }
}
