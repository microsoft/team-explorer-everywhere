// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import com.microsoft.tfs.core.httpclient.methods.PostMethod;

/**
 * A SOAPRequest is created by {@link SOAPService#createSOAPRequest()} and
 * contains the already-initialized POST method and XML stream writer that is
 * ready to receive input parameters. When the request is configured (parameters
 * are written via the stream writer), complete the request via
 * {@link SOAPService#execSOAPRequest()}.
 *
 * @threadsafety thread-safe
 */
public final class SOAPRequest {
    private final PostMethod postMethod;
    private final SOAPRequestEntity requestEntity;

    public SOAPRequest(final PostMethod postMethod, final SOAPRequestEntity requestEntity) {
        this.postMethod = postMethod;
        this.requestEntity = requestEntity;
    }

    /**
     * @return get the HTTP method object so it can be modified (set headers,
     *         etc.).
     */
    public PostMethod getPostMethod() {
        return postMethod;
    }

    /**
     * @return get the SOAP request entity so it can be modified.
     */
    public SOAPRequestEntity getRequestEntity() {
        return requestEntity;
    }
}
