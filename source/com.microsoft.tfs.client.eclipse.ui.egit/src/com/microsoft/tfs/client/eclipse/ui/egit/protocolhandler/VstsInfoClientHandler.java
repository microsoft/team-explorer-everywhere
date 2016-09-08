// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.protocolhandler;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import com.microsoft.alm.client.HttpMethod;
import com.microsoft.alm.client.TeeClientHandler;
import com.microsoft.alm.client.VssRestClientHandler;
import com.microsoft.alm.client.VssRestRequest;
import com.microsoft.alm.visualstudio.services.webapi.ApiResourceLocationCollection;
import com.microsoft.alm.visualstudio.services.webapi.ApiResourceVersion;
import com.microsoft.tfs.client.common.ui.config.UIClientConnectionAdvisor;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;

public class VstsInfoClientHandler extends TeeClientHandler implements VssRestClientHandler {

    private final static UUID UUID_EMPTY = UUID.fromString("00000000-0000-0000-0000-000000000000"); //$NON-NLS-1$

    public VstsInfoClientHandler(final String targetURL, final Credentials credentials) {
        super(getFakeCollection(targetURL, credentials).getHTTPClient());
    }

    public VstsInfoClientHandler(HttpClient httpClient) {
        super(httpClient);
    }

    /**
     * The checkConnection method is not applicable for this client.
     */
    @Override
    @Deprecated
    public boolean checkConnection() {
        Check.isTrue(false, "The checkConnection method is not applicable for this client."); //$NON-NLS-1$
        return false;
    }

    /**
     * The loadLocations method is not applicable for this client.
     */
    @Override
    public ApiResourceLocationCollection loadLocations() {
        Check.isTrue(false, "The loadLocations method is not applicable for this client."); //$NON-NLS-1$
        return null;
    }

    /**
     * This createRequest method is not applicable for this client
     */
    @Override
    @Deprecated
    public <TEntity> VssRestRequest createRequest(
        HttpMethod method,
        UUID locationId,
        Map<String, Object> routeValues,
        ApiResourceVersion version,
        TEntity value,
        String contentMediaType,
        Map<String, String> queryParameters,
        String acceptMediaType) {
        Check.isTrue(false, "This createRequest method is not applicable for this client."); //$NON-NLS-1$
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ApiResourceVersion negotiateRequestVersion(UUID locationId, ApiResourceVersion version) {
        // Do not supply api_version parameter in the "accept media type" header
        return null;
    }

    public <TEntity> VssRestRequest createRequest(
        final HttpMethod method,
        final URI target,
        final TEntity value,
        final String contentMediaType,
        final Map<String, String> queryParameters,
        final String acceptMediaType) {
        TeeRestRequest request = (TeeRestRequest) createRequest(
            method,
            target,
            UUID_EMPTY,
            null,
            value,
            contentMediaType,
            queryParameters,
            acceptMediaType);
        request.setFollowRedirects(false);

        return request;
    }

    private static TFSTeamProjectCollection getFakeCollection(final String targetURL, final Credentials credentials) {
        final URI targetUri = URIUtils.newURI(targetURL);
        final URI serverUri = URIUtils.removePathAndQueryParts(targetUri);
        final TFSTeamProjectCollection collection =
            new TFSTeamProjectCollection(serverUri, credentials, new UIClientConnectionAdvisor());

        return collection;
    }
}
