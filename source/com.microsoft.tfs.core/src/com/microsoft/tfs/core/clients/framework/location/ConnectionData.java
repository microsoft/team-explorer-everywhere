// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;

import ms.ws._ConnectionData;

/**
 * Wrapper class for the {@link _ConnectionData} proxy object.
 *
 * @since TEE-SDK-10.1
 */
public class ConnectionData extends WebServiceObjectWrapper {
    /**
     * Wrapper constructor
     */
    public ConnectionData(final _ConnectionData locationServiceData) {
        super(locationServiceData);
    }

    /**
     * Returns the wrapped proxy object.
     */
    public _ConnectionData getWebServiceObject() {
        return (_ConnectionData) webServiceObject;
    }

    public GUID getInstanceID() {
        return new GUID(getWebServiceObject().getInstanceId());
    }

    public GUID getCatalogResourceID() {
        return new GUID(getWebServiceObject().getCatalogResourceId());
    }

    public String getWebApplicationRelativeDirectory() {
        return getWebServiceObject().getWebApplicationRelativeDirectory();
    }

    public int getServerCapabilities() {
        return getWebServiceObject().getServerCapabilities();
    }

    public TeamFoundationIdentity getAuthenticatedUser() {
        return new TeamFoundationIdentity(getWebServiceObject().getAuthenticatedUser());
    }

    public TeamFoundationIdentity getAuthorizedUser() {
        return new TeamFoundationIdentity(getWebServiceObject().getAuthorizedUser());
    }

    public LocationServiceData getLocationServiceData() {
        return new LocationServiceData(getWebServiceObject().getLocationServiceData());
    }
}
