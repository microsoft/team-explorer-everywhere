// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources;

import org.eclipse.core.resources.IResource;

/**
 * <p>
 * {@link LocationUnavailablePolicy} is an "enum" that identifies a policy for
 * handling {@link IResource}s that do not have a location available.
 * </p>
 *
 * <p>
 * A resource does not have a location available if the
 * {@link IResource#getLocation()} method returns <code>null</code>. There are a
 * variety of reasons this can happen. For example, if the resource is not
 * stored in the local file system that method will return <code>null</code>.
 * </p>
 */
public class LocationUnavailablePolicy {
    /**
     * If an {@link IResource} does not have a location available, the method
     * that requires the location should ignore the resource and complete the
     * operation as if the resource had not been passed to the method.
     */
    public static final LocationUnavailablePolicy IGNORE_RESOURCE = new LocationUnavailablePolicy();

    /**
     * If an {@link IResource} does not have a location available, the method
     * that requires the location should throw an exception.
     */
    public static final LocationUnavailablePolicy THROW = new LocationUnavailablePolicy();

    private LocationUnavailablePolicy() {

    }
}
