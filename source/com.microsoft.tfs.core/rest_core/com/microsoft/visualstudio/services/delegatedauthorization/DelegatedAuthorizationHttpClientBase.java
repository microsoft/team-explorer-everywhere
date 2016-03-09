// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.delegatedauthorization;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.microsoft.visualstudio.services.delegatedauthorization.model.SessionToken;
import com.microsoft.vss.client.core.VssHttpClientBase;
import com.microsoft.vss.client.core.model.ApiResourceVersion;
import com.microsoft.vss.client.core.utils.ArgumentUtility;

/**
 * Placeholder class
 *
 * Should convert to generated class in the future
 */
public class DelegatedAuthorizationHttpClientBase extends VssHttpClientBase {

    private static final Map<String, Class<? extends Exception>> TRANSLATED_EXCEPTIONS;

    static {
        TRANSLATED_EXCEPTIONS = new HashMap<String, Class<? extends Exception>>();
    }

    /**
     * Create a new instance of DelegatedAuthorizationHttpClientBase
     *
     * @param jaxrsClient
     *        an initialized instance of a JAX-RS Client implementation
     * @param baseUrl
     *        a TFS project collection URL
     */
    protected DelegatedAuthorizationHttpClientBase(final Object jaxrsClient, final URI baseUrl) {
        super(jaxrsClient, baseUrl);
    }

    /**
     * Create a new instance of DelegatedAuthorizationHttpClientBase
     *
     * @param tfsConnection
     *        an initialized instance of a TfsTeamProjectCollection
     */
    protected DelegatedAuthorizationHttpClientBase(final Object tfsConnection) {
        super(tfsConnection);
    }

    @Override
    protected Map<String, Class<? extends Exception>> getTranslatedExceptions() {
        return TRANSLATED_EXCEPTIONS;
    }

    /**
     * Create new Personal Access Token
     */
    public SessionToken createSessionToken(final SessionToken sessionToken) {
        ArgumentUtility.checkForNull(sessionToken, "sessionToken"); //$NON-NLS-1$

        final UUID locationId = UUID.fromString("ADA996BC-8C18-4193-B20C-CD41B13F5B4D"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("3.0-preview.1"); //$NON-NLS-1$
        final Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("tokentype", "compact"); //$NON-NLS-1$ //$NON-NLS-2$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            null,
            apiVersion,
            sessionToken,
            APPLICATION_JSON_TYPE,
            queryParams,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, SessionToken.class);
    }
}
