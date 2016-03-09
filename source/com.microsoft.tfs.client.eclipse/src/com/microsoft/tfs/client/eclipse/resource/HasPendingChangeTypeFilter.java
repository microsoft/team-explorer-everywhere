// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

/**
 * Accepts {@link IResource}s which have a {@link PendingChange} with any of the
 * given {@link ChangeType}s. Rejects all other {@link IResource}s.
 *
 * @threadsafety thread-compatible
 */
public class HasPendingChangeTypeFilter extends ResourceFilter {
    private final ChangeType[] changeTypes;
    private final RepositoryUnavailablePolicy repositoryUnavailablePolicy;
    private final boolean matchChangesForChildResources;

    public HasPendingChangeTypeFilter(
        final ChangeType[] changeTypes,
        final RepositoryUnavailablePolicy repositoryUnavailablePolicy,
        final boolean matchChangesForChildResources) {
        Check.notNull(changeTypes, "changeTypes"); //$NON-NLS-1$
        Check.notNull(repositoryUnavailablePolicy, "repositoryUnavailablePolicy"); //$NON-NLS-1$

        this.changeTypes = changeTypes;
        this.repositoryUnavailablePolicy = repositoryUnavailablePolicy;
        this.matchChangesForChildResources = matchChangesForChildResources;
    }

    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(resource.getProject());

        if (repository == null) {
            return repositoryUnavailablePolicy.acceptResourceWithNoRepository(resource) ? ACCEPT : REJECT;
        }

        final PendingChange[] pendingChanges =
            PluginResourceHelpers.pendingChangesForResource(resource, repository, matchChangesForChildResources);

        for (int i = 0; i < pendingChanges.length; i++) {
            for (int j = 0; j < changeTypes.length; j++) {
                if (pendingChanges[i].getChangeType().contains(changeTypes[j])) {
                    return ACCEPT;
                }
            }
        }

        return REJECT;
    }
}
