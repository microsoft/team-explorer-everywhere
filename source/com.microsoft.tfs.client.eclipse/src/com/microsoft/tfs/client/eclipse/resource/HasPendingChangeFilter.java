// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

/**
 * Accepts {@link IResource}s which have a pending change, rejects all others.
 * This filter is recursive: pending changes for items inside a resource will
 * cause it to match.
 *
 * @threadsafety thread-compatible
 */
public class HasPendingChangeFilter extends ResourceFilter {
    private final RepositoryUnavailablePolicy repositoryUnavailablePolicy;

    public HasPendingChangeFilter(final RepositoryUnavailablePolicy repositoryUnavailablePolicy) {
        Check.notNull(repositoryUnavailablePolicy, "repositoryUnavailablePolicy"); //$NON-NLS-1$

        this.repositoryUnavailablePolicy = repositoryUnavailablePolicy;
    }

    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(resource.getProject());

        if (repository == null) {
            return repositoryUnavailablePolicy.acceptResourceWithNoRepository(resource) ? ACCEPT : REJECT;
        }

        final PendingChange[] pendingChanges =
            PluginResourceHelpers.pendingChangesForResource(resource, repository, true);

        return (pendingChanges != null && pendingChanges.length > 0) ? ACCEPT : REJECT;
    }
}
