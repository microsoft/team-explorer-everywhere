// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.ws._LocationMapping;

/**
 * Wrapper class for the {@link LocationMapping} proxy object.
 *
 * @since TEE-SDK-10.1
 */
public class LocationMapping extends WebServiceObjectWrapper {
    /**
     * Wrapper constructor.
     */
    public LocationMapping(final _LocationMapping locationMapping) {
        super(locationMapping);
    }

    /**
     * Returns the wrapped proxy object.
     */
    public _LocationMapping getWebServiceObject() {
        return (_LocationMapping) webServiceObject;
    }

    /**
     * Returns the location for this location mapping.
     */
    public String getLocation() {
        return getWebServiceObject().getLocation();
    }

    /**
     * Returns the access mapping moniker for this location mapping.
     */
    public String getAccessMappingMoniker() {
        return getWebServiceObject().getAccessMappingMoniker();
    }
}
