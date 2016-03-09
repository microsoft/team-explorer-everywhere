// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.offline;

/**
 * <p>
 * {@link OfflineSynchronizerProvider} is the interface for providing paths to
 * {@link OfflineSynchronizer}. It exists to allow clients to pass opaque types
 * ("resources") to the offline engine.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface OfflineSynchronizerProvider {
    /**
     * This is the list of resources (of some opaque type) which will be brought
     * back online. The offline engine will call getPathForResource() on these
     * types to determine their local path.
     *
     * @return An array of objects which represent the
     */
    public Object[] getResources();

    /**
     * Given an implementor-defined "resource", it will return the local path
     * which corresponds.
     *
     * @param resource
     *        An internally-defined resource
     * @return The local path
     */
    public String getLocalPathForResource(Object resource);
}
