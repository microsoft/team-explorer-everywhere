// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.alm.client;

import java.net.URI;

import com.microsoft.alm.client.model.NameValueCollection;
import com.microsoft.alm.teamfoundation.sourcecontrol.webapi.VstsInfo;
import com.microsoft.tfs.core.util.URIUtils;

public class VstsInfoHttpClient extends VssHttpClientBase {

    private final VstsInfoClientHandler clientHandler;

    public VstsInfoHttpClient(VstsInfoClientHandler clientHandler) {
        super(clientHandler, null);
        this.clientHandler = clientHandler;
    }

    /**
     * Get get server info from the Git repository URL.
     * 
     * @param url
     *        Git repository URL
     * 
     * @return VstsInfo
     */
    public VstsInfo getServerRepositoryInfo(final String url) {

        final StringBuilder sb = new StringBuilder(url);

        if (sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        sb.append("vsts/info"); //$NON-NLS-1$

        final URI target = URIUtils.newURI(sb.toString());
        final NameValueCollection queryParameters = new NameValueCollection();

        final VssRestRequest httpRequest = clientHandler.createRequest(
            HttpMethod.GET,
            target,
            null,
            null,
            queryParameters,
            VssMediaTypes.APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, VstsInfo.class);
    }
}
