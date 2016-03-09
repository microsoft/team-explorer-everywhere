// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.util.Check;

/**
 * A {@link ResourceFilter} that rejects resources that are baseline metadata
 * folders or children.
 *
 * @threadsafety thread-safe
 */
public class LocalWorkspaceBaselineFilter extends ResourceFilter {
    private final RepositoryUnavailablePolicy repositoryUnavailablePolicy;

    /**
     * Constructs a {@link LocalWorkspaceBaselineFilter}.
     */
    public LocalWorkspaceBaselineFilter(final RepositoryUnavailablePolicy repositoryUnavailablePolicy) {
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
         * Reject any resource inside a baseline metadata folder (.tf/$tf).
         */
        IResource r = resource;
        while (r.getType() != IResource.ROOT) {
            if (r.getType() == IResource.FOLDER && BaselineFolder.isPotentialBaselineFolderName(r.getName())) {
                return REJECT_AND_REJECT_CHILDREN;
            }

            r = r.getParent();
        }

        return ACCEPT;
    }
}
