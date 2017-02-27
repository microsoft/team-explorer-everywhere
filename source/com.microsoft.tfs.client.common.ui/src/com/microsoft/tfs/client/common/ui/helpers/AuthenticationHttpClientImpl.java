// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.alm.helpers.HttpResponse;
import com.microsoft.alm.helpers.StringContent;
import com.microsoft.tfs.client.common.ui.config.UIClientConnectionAdvisor;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpMethodBase;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.EntityEnclosingMethod;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.core.httpclient.methods.HeadMethod;
import com.microsoft.tfs.core.httpclient.methods.PostMethod;
import com.microsoft.tfs.core.httpclient.methods.StringRequestEntity;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.GUID;

public class AuthenticationHttpClientImpl implements com.microsoft.alm.helpers.HttpClient {

    final private HttpClient apacheClient;
    final private Map<String, String> headers;

    public AuthenticationHttpClientImpl() {
        final ConnectionInstanceData instanceData = new ConnectionInstanceData(URIUtils.VSTS_ROOT_URL, GUID.newGUID());
        final UIClientConnectionAdvisor connectionAdvisor = new UIClientConnectionAdvisor();

        apacheClient = connectionAdvisor.getHTTPClientFactory(instanceData).newHTTPClient();
        headers = new HashMap<String, String>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    private String getResponseText(final HttpMethodBase request) throws IOException {
        final int statusCode = apacheClient.executeMethod(request);
        final String responseText = request.getResponseBodyAsString();

        if (HttpStatus.isSuccessFamily(statusCode)) {
            return responseText;
        }

        throw new IOException(HttpStatus.getStatusText(statusCode) + ":" + responseText); //$NON-NLS-1$
    }

    /**
     * Timeout is ignored
     */
    @Override
    public String getGetResponseText(final URI uri, final int timeout) throws IOException {
        return getGetResponseText(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGetResponseText(final URI uri) throws IOException {
        final HttpMethodBase request = new GetMethod(uri.toString());
        addHeader(request);

        return getResponseText(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeaderField(final URI uri, final String header) throws IOException {
        final HttpMethodBase request = new HeadMethod(uri.toString());
        addHeader(request);

        apacheClient.executeMethod(request);

        final Header[] headers = request.getResponseHeaders(header);
        if (headers.length > 0) {
            final Header responseHeader = headers[0];
            return responseHeader.getValue();
        }

        return null;
    }

    private EntityEnclosingMethod createPostMethod(final URI uri, final StringContent content) {
        final EntityEnclosingMethod request = new PostMethod(uri.toString());

        request.setRequestEntity(new StringRequestEntity(content.getContent()));
        getHeaders().putAll(content.Headers);
        addHeader(request);

        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse getPostResponse(final URI uri, final StringContent content) throws IOException {
        final EntityEnclosingMethod request = createPostMethod(uri, content);

        final int statusCode = apacheClient.executeMethod(request);

        String responseOut = null;
        String responseError = null;

        if (HttpStatus.isSuccessFamily(statusCode)) {
            responseOut = request.getResponseBodyAsString();
        } else {
            responseError = request.getResponseBodyAsString();
        }

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.status = statusCode;
        httpResponse.errorText = responseError;
        httpResponse.responseText = responseOut;

        return httpResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPostResponseText(URI uri, StringContent content) throws IOException {
        final EntityEnclosingMethod request = createPostMethod(uri, content);

        return getResponseText(request);
    }

    private void addHeader(final HttpMethodBase request) {
        for (final Map.Entry<String, String> entry : headers.entrySet()) {
            request.setRequestHeader(entry.getKey(), entry.getValue());
        }
    }
}
