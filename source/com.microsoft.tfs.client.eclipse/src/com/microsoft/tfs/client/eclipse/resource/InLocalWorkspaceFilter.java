// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.util.Check;

/**
 * A {@link ResourceFilter} that accepts resources in a TFS 2012 local
 * workspace.
 *
 * @threadsafety thread-safe
 */
public class InLocalWorkspaceFilter extends ResourceFilter {
    private final RepositoryUnavailablePolicy repositoryUnavailablePolicy;

    /**
     * Constructs a {@link InLocalWorkspaceFilter}.
     */
    public InLocalWorkspaceFilter(final RepositoryUnavailablePolicy repositoryUnavailablePolicy) {
        Check.notNull(repositoryUnavailablePolicy, "repositoryUnavailablePolicy"); //$NON-NLS-1$
        this.repositoryUnavailablePolicy = repositoryUnavailablePolicy;
    }

    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        if (resource.getType() == IResource.ROOT) {
            return REJECT;
        }

        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(resource.getProject());

        if (repository == null) {
            return repositoryUnavailablePolicy.acceptResourceWithNoRepository(resource) ? ACCEPT : REJECT;
        }

        return repository.getWorkspace().getLocation() == WorkspaceLocation.LOCAL ? ACCEPT : REJECT;
    }
}
