// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.internal;

import com.microsoft.tfs.core.clients.framework.location.AccessMapping;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;

/**
 * The in-memory cache components that will be persisted to disk.
 */
public class LocationServiceCacheData {
    private final int lastChangeID;
    private final String defaultMappingMoniker;
    private final String virtualDirectoy;
    private final AccessMapping[] accessMappings;
    private final ServiceDefinition[] serviceDefinitions;

    public LocationServiceCacheData(
        final int theLastChangeID,
        final String theDefaultMappingMoniker,
        final String theVirtualDirectoryName,
        final AccessMapping[] theAccessMappings,
        final ServiceDefinition[] theServiceDefinitions) {
        lastChangeID = theLastChangeID;
        defaultMappingMoniker = theDefaultMappingMoniker;
        virtualDirectoy = theVirtualDirectoryName;
        accessMappings = theAccessMappings;
        serviceDefinitions = theServiceDefinitions;
    }

    public int getLastChangeID() {
        return lastChangeID;
    }

    public String getDefaultMappingMoniker() {
        return defaultMappingMoniker;
    }

    public String getVirtualDirectory() {
        return virtualDirectoy;
    }

    public AccessMapping[] getAccessMappings() {
        return accessMappings;
    }

    public ServiceDefinition[] getServiceDefinitions() {
        return serviceDefinitions;
    }

}
