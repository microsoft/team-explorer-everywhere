// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.util.Check;

/**
 * Provides a {@link ResourceFilter} which uses the Eclipse Team API to test
 * whether items are in a project configured for use with a specified team
 * provider.
 */
public class ConfiguredTeamProviderFilter extends ResourceFilter {
    private final String providerId;

    /**
     * Constructs a {@link ResourceFilter} that accepts only the resources whose
     * projects are configured to use the repository provider with the specified
     * ID. The resources that are rejected include resources under projects that
     * are either not configured to use a repository provider or are configured
     * to use a repository provider with a different ID.
     *
     * @param providerId
     *        the repository provider ID to use in tests (must not be
     *        <code>null</code>)
     */
    public ConfiguredTeamProviderFilter(final String providerId) {
        Check.notNull(providerId, "providerId"); //$NON-NLS-1$

        this.providerId = providerId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        if (IResource.ROOT == resource.getType()) {
            /*
             * The root resource is never configured, so this filter always
             * rejects it. However, the children of the root resource are not
             * rejected, since they may be configured (in all other cases we can
             * either reject or accept the children).
             */
            return REJECT;
        }

        if (TeamUtils.isConfiguredWith(resource, providerId)) {
            return ACCEPT_AND_ACCEPT_CHILDREN;
        }

        return REJECT_AND_REJECT_CHILDREN;
    }
}