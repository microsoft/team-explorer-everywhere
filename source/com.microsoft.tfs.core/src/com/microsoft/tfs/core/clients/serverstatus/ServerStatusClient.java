// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.serverstatus;

import com.microsoft.tfs.core.exceptions.mappers.ServerStatusExceptionMapper;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;

import ms.tfs.services.serverstatus._03._ServerStatusSoap;

/**
 * @since TEE-SDK-10.1
 */
public class ServerStatusClient {
    private final _ServerStatusSoap webService;

    public ServerStatusClient(final _ServerStatusSoap webService) {
        this.webService = webService;
    }

    public _ServerStatusSoap getWebService() {
        return webService;
    }

    public String checkAuthentication() {
        try {
            return getWebService().checkAuthentication();
        } catch (final ProxyException e) {
            throw ServerStatusExceptionMapper.map(e);
        }
    }
}
