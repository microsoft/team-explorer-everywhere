// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.account;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.visualstudio.services.account.model.Account;
import com.microsoft.visualstudio.services.account.model.Profile;
import com.microsoft.vss.client.core.VssHttpClientBase;
import com.microsoft.vss.client.core.model.ApiResourceVersion;

/**
 * Placeholder class
 *
 * Should convert to generated class in the future
 */
public class AccountHttpClientBase extends VssHttpClientBase {

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
    protected AccountHttpClientBase(final Object jaxrsClient, final URI baseUrl) {
        super(jaxrsClient, baseUrl);
    }

    /**
     * Create a new instance of DelegatedAuthorizationHttpClientBase
     *
     * @param tfsConnection
     *        an initialized instance of a TfsTeamProjectCollection
     */
    protected AccountHttpClientBase(final Object tfsConnection) {
        super(tfsConnection);
    }

    @Override
    protected Map<String, Class<? extends Exception>> getTranslatedExceptions() {
        return TRANSLATED_EXCEPTIONS;
    }

    public List<Account> getAccounts(final UUID memberId) {
        final UUID locationId = UUID.fromString("229A6A53-B428-4FFB-A835-E8F36B5B4B1E"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("3.0-preview.1"); //$NON-NLS-1$
        final Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("memberId", memberId.toString()); //$NON-NLS-1$

        final Object httpRequest =
            super.createRequest(HttpMethod.GET, locationId, null, apiVersion, queryParams, APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Account>>() {
        });
    }

    public Profile getMyProfile() {
        final UUID locationId = UUID.fromString("f83735dc-483f-4238-a291-d45f6080a9af"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("3.0-preview.1"); //$NON-NLS-1$
        final Map<String, Object> routeValue = new HashMap<String, Object>();
        routeValue.put("id", "me"); //$NON-NLS-1$ //$NON-NLS-2$

        final Object httpRequest =
            super.createRequest(HttpMethod.GET, locationId, routeValue, apiVersion, APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Profile.class);
    }
}
