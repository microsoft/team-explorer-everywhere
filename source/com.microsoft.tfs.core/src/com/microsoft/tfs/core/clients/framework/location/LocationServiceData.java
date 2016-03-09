// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.ws._LocationServiceData;

/**
 * Wrapper class for the {@link _LocationServiceData} proxy object.
 *
 * @since TEE-SDK-10.1
 */
public class LocationServiceData extends WebServiceObjectWrapper {
    private final AccessMapping[] accessMappings;
    private final ServiceDefinition[] serviceDefinitions;

    /**
     * Wrapper constructor
     */
    public LocationServiceData(final _LocationServiceData locationServiceData) {
        super(locationServiceData);

        accessMappings =
            (AccessMapping[]) WrapperUtils.wrap(AccessMapping.class, locationServiceData.getAccessMappings());
        serviceDefinitions = (ServiceDefinition[]) WrapperUtils.wrap(
            ServiceDefinition.class,
            locationServiceData.getServiceDefinitions());
    }

    /**
     * Returns the wrapped proxy object.
     */
    public _LocationServiceData getWebServiceObject() {
        return (_LocationServiceData) webServiceObject;
    }

    /**
     * Returns the default access mapping monkier.
     */
    public String getDefaultAccessMappingMoniker() {
        return getWebServiceObject().getDefaultAccessMappingMoniker();
    }

    /**
     * Returns the last server change id for which this data current.
     */
    public int getLastChangeID() {
        return getWebServiceObject().getLastChangeId();
    }

    public boolean isClientCacheFresh() {
        return getWebServiceObject().isClientCacheFresh();
    }

    public boolean isAccessPointsDoNotIncludeWebAppRelativeDirectory() {
        return getWebServiceObject().isAccessPointsDoNotIncludeWebAppRelativeDirectory();
    }

    /**
     * Returns the access mappings.
     */
    public AccessMapping[] getAccessMappings() {
        return accessMappings;
    }

    /**
     * Return the service definitions.
     */
    public ServiceDefinition[] getServiceDefinitions() {
        return serviceDefinitions;
    }
}
