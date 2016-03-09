// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcedata;

import org.eclipse.core.resources.IResource;

/**
 * These listeners may be registered via
 * {@link ResourceDataManager#addListener(ResourceDataListener)} to receive
 * notifications when resource data has been changed for a resource.
 *
 * @threadsafety thread-compatible
 */
public interface ResourceDataListener {
    /**
     * Invoked when resource data has changed for some resources.
     *
     * @param resources
     *        the resources which had their resource data changed (must not be
     *        <code>null</code>)
     */
    public void resourceDataChanged(IResource[] resources);
}