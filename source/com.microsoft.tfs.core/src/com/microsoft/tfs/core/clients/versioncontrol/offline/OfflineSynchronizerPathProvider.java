// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.offline;

/**
 * This is the simplest implementation of an OfflineSynchronizerProvider, which
 * simply works in Strings representing the local paths.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class OfflineSynchronizerPathProvider implements OfflineSynchronizerProvider {
    private final String[] localPaths;

    /**
     * Setup the list of resource to bring online. Resources may be any object
     * whose toString() method returns a fully-qualified local path to the
     * object in question.
     *
     * @param localPaths
     *        an array of resources to bring online
     */
    public OfflineSynchronizerPathProvider(final String[] localPaths) {
        this.localPaths = localPaths;
    }

    /**
     * Return an array containing the resources to be brought back online. Do
     * not modify the returned objects to ensure thread-safety.
     *
     * @return the resources to bring online
     */
    @Override
    public Object[] getResources() {
        return localPaths;
    }

    /**
     * Returns the resource provided.
     *
     * @param o
     *        a resource configured in the constructor
     * @return a fully-qualified local path to the resource
     */
    @Override
    public String getLocalPathForResource(final Object o) {
        if (!(o instanceof String)) {
            return null;
        }

        return (String) o;
    }
}
