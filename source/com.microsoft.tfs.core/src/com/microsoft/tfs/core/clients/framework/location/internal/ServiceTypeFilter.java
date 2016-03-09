// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.internal;

import com.microsoft.tfs.core.clients.framework.location.LocationServiceConstants;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;

import ms.ws._ServiceTypeFilter;

public class ServiceTypeFilter extends WebServiceObjectWrapper {

    /**
     * Used to request all service definitions. Contains an array with exactly
     * one element with a service type of "*".
     */
    public static final ServiceTypeFilter[] ALL = new ServiceTypeFilter[] {
        new ServiceTypeFilter(LocationServiceConstants.ALL_SERVICES_TYPE_FILTER)
    };

    /**
     * Used to request only service information about the possible client zones.
     * Returns an empty array
     */
    public static final ServiceTypeFilter[] CLIENT_ZONE_ONLY = new ServiceTypeFilter[] {};

    protected ServiceTypeFilter(final _ServiceTypeFilter filter) {
        super(filter);
    }

    public _ServiceTypeFilter getWebServiceObject() {
        return (_ServiceTypeFilter) webServiceObject;
    }

    public ServiceTypeFilter(final String serviceType, final GUID guid) {
        this(new _ServiceTypeFilter(serviceType, guid.getGUIDString()));
    }

    public ServiceTypeFilter(final String serviceType) {
        this(serviceType, LocationServiceConstants.ALL_INSTANCES_IDENTIFIER);
    }

    public GUID getIdentifier() {
        return new GUID(getWebServiceObject().getIdentifier());
    }

    public String getServiceType() {
        return getWebServiceObject().getServiceType();
    }
}
