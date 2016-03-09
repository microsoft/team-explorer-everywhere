// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.filter;

import org.eclipse.core.resources.IResource;

/**
 * <p>
 * {@link ResourceTypeFilter} is an {@link ResourceFilter} that filters
 * resources based on resource type ({@link IResource#getType()}).
 * </p>
 *
 * <p>
 * A {@link ResourceTypeFilter} accepts only those resources that have the
 * specified type. A filter that does the opposite (accepts all resources other
 * than those of a certain type) can be constructed using the
 * {@link ResourceFilters#getInverse(ResourceFilter)} method.
 * </p>
 */
public class ResourceTypeFilter extends ResourceFilter {
    private final int type;

    /**
     * Creates a new {@link ResourceTypeFilter} that will accept only those
     * resources that have the specified type.
     *
     * @param type
     *        either {@link IResource#FILE}, {@link IResource#FOLDER},
     *        {@link IResource#PROJECT}, or {@link IResource#ROOT}
     */
    public ResourceTypeFilter(final int type) {
        if ((type != IResource.FILE)
            && (type != IResource.FOLDER)
            && (type != IResource.PROJECT)
            && (type != IResource.ROOT)) {
            throw new IllegalArgumentException("illegal type: " + type); //$NON-NLS-1$
        }

        this.type = type;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.resources.filter.IResourceFilter#filter
     * (org.eclipse.core.resources.IResource)
     */
    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        int resultFlags = (type == resource.getType() ? RESULT_FLAG_ACCEPT : RESULT_FLAG_REJECT);

        if (type == IResource.ROOT) {
            resultFlags |= (IResource.FILE == resource.getType() ? RESULT_FLAG_NONE : RESULT_FLAG_REJECT_CHILDREN);
        } else if (type == IResource.PROJECT) {
            resultFlags |= ((IResource.PROJECT == resource.getType() || IResource.FOLDER == resource.getType())
                ? RESULT_FLAG_REJECT_CHILDREN : RESULT_FLAG_NONE);
        }

        return ResourceFilterResult.getInstance(resultFlags);
    }
}
