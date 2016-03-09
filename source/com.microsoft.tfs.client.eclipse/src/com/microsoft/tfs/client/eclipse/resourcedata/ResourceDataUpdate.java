// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcedata;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.util.Check;

/**
 * Associates {@link ResourceData} with an {@link IResource}, for the purpose of
 * feeing to {@link ResourceDataManager#update(ResourceDataUpdate[])}.
 *
 * @threadsafety thread-safe
 */
public class ResourceDataUpdate {
    private final IResource[] resources;
    private final ResourceData resourceData;

    /**
     * Creates a {@link ResourceDataUpdate} for an {@link IResource}, whose data
     * is the {@link ResourceData} (which may be <code>null</code> to signify no
     * resource data or the resource data should be deleted).
     *
     * @param resource
     *        the resource this data belongs to (must not be <code>null</code>)
     * @param resourceData
     *        the new data to save (if <code>null</code> the resource data is
     *        removed from the resource when the update is processed)
     */
    public ResourceDataUpdate(final IResource resource, final ResourceData resourceData) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        resources = new IResource[] {
            resource
        };
        this.resourceData = resourceData;
    }

    public ResourceDataUpdate(final IResource[] resources, final ResourceData resourceData) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$

        this.resources = resources;
        this.resourceData = resourceData;
    }

    public IResource[] getResources() {
        return resources;
    }

    public ResourceData getResourceData() {
        return resourceData;
    }
}
