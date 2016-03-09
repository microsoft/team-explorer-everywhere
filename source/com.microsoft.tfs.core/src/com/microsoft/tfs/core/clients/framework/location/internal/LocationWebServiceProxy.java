// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.internal;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.framework.SupportedFeatures;
import com.microsoft.tfs.core.clients.framework.location.ConnectionData;
import com.microsoft.tfs.core.clients.framework.location.LocationServiceData;
import com.microsoft.tfs.core.exceptions.mappers.LocationExceptionMapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;

import ms.ws._ConnectionData;
import ms.ws._LocationServiceData;
import ms.ws._LocationWebServiceSoap;
import ms.ws._ServiceTypeFilter;

/**
 * A proxy class for the TFS location web service.
 */
public class LocationWebServiceProxy {
    _LocationWebServiceSoap webService;

    /**
     * Constructor
     */
    public LocationWebServiceProxy(final TFSConnection server) {
        webService = (_LocationWebServiceSoap) server.getWebService(_LocationWebServiceSoap.class);
    }

    /**
     * Retrieve service data for the specified service types.
     *
     * @param serviceTypeFilters
     *        Identifies the service types that should be returned. Use null or
     *        an empty array to retrieve all service types.
     *
     * @return The set of service types matching the specified filter.
     */
    public LocationServiceData queryServices(final ServiceTypeFilter[] serviceTypeFilters, final int lastChangeId) {
        try {
            final _LocationServiceData data = webService.queryServices(
                (_ServiceTypeFilter[]) WrapperUtils.unwrap(_ServiceTypeFilter.class, serviceTypeFilters),
                lastChangeId);
            return new LocationServiceData(data);
        } catch (final ProxyException e) {
            throw LocationExceptionMapper.map(e);
        }
    }

    /**
     * Connects to the location service and returns information about the
     * connection.
     */
    public ConnectionData connect(final int connectOptions, final int lastChangeId) {
        try {
            final _ConnectionData data =
                webService.connect(connectOptions, lastChangeId, SupportedFeatures.ALL.toIntFlags());
            return new ConnectionData(data);
        } catch (final ProxyException e) {
            throw LocationExceptionMapper.map(e);
        }
    }
}
