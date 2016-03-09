// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.Team;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;

/**
 * Provides a {@link ResourceFilter} which uses the Eclipse Team API to test
 * whether items are ignored by Eclipse's Team ignore preferences.
 */
public class TeamIgnoredResourcesFilter extends ResourceFilter {
    public TeamIgnoredResourcesFilter() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        return Team.isIgnoredHint(resource) ? REJECT_AND_REJECT_CHILDREN : ACCEPT;
    }
}
