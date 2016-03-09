// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

import com.microsoft.tfs.core.clients.framework.location.LocationService;

/**
 * <p>
 * Gets server configuration information from Team Foundation Servers which
 * support the TFS 2010 framework services.
 * </p>
 * <p>
 * {@link LocationService} does most of the work for us.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @since TFS 2010
 * @threadsafety thread-compatible
 */
public class FrameworkServerDataProvider extends LocationService {
    /**
     * Constructs a {@link FrameworkServerDataProvider} using a
     * {@link TFSConnection}, which can be either a
     * {@link TFSTeamProjectCollection} or {@link TFSConfigurationServer}.
     *
     * @param connection
     *        a {@link TFSConnection} configured to point to the correct server
     *        (must not be <code>null</code>)
     */
    public FrameworkServerDataProvider(final TFSConnection connection) {
        super(connection);
    }

    /**
     * @return <code>true</code> if location service data for this connection
     *         can be loaded from disk successfully, <code>false</code>
     *         otherwise
     */
    public boolean hasLocalCacheDataForConnection() {
        return getLocationCacheManager().hasLocalCacheDataForConnection();
    }
}
