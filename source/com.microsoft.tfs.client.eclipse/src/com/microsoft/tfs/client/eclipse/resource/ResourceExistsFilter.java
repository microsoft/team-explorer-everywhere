// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;

/**
 * Accepts {@link IResource}s which are files, folders, or projects which exist
 * in a local working folder, rejects all others. This filter is non-recursive.
 *
 * @threadsafety thread-compatible
 */
public class ResourceExistsFilter extends ResourceFilter {
    public ResourceExistsFilter() {
    }

    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        /*
         * The workspace root is never appropriate for plug-in operations.
         */
        if (resource.getType() == IResource.ROOT) {
            return REJECT;
        }

        return resource.exists() ? ACCEPT : REJECT;
    }
}
