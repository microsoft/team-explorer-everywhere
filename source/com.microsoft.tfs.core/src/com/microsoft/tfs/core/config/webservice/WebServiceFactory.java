// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.webservice;

import java.net.URI;
import java.net.URISyntaxException;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.httpclient.HttpClient;

import ms.tfs.services.linking._03._IntegrationServiceSoap;
import ms.tfs.services.registration._03._RegistrationSoap;
import ms.ws._CatalogWebServiceSoap;
import ms.ws._CatalogWebServiceSoap12Service;
import ms.ws._LocationWebServiceSoap;
import ms.wss._ListsSoap;

/**
 * <p>
 * An {@link WebServiceFactory} is used by a {@link TFSConnection} to create web
 * services. An {@link WebServiceFactory} is supplied to a {@link TFSConnection}
 * by a {@link ConnectionAdvisor}.
 * </p>
 *
 * <p>
 * {@link TFSConnection} allows multiple threads to use a
 * {@link WebServiceFactory} concurrently.
 * </p>
 *
 * <p>
 * For a default implementation, see {@link DefaultWebServiceFactory}.
 * </p>
 *
 * @see TFSConnection
 * @see ConnectionAdvisor
 * @see DefaultWebServiceFactory
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface WebServiceFactory {
    /**
     * Creates a new registration web service, which cannot be created via
     * {@link #newWebService(TFSConnection, Class, URI, HttpClient, ServerDataProvider, RegistrationClient)}
     * because that method requires a working registration web service.
     *
     * @param connectionBaseURI
     *        the connection's base {@link URI} (must not be <code>null</code>)
     * @param httpClient
     *        the {@link HttpClient} to use for accessing the web service (must
     *        not be <code>null</code>)
     * @return a {@link _RegistrationSoap} (never <code>null</code>)
     * @throws URISyntaxException
     */
    public _RegistrationSoap newRegistrationWebService(final URI connectionBaseURI, final HttpClient httpClient)
        throws URISyntaxException;

    /**
     * Creates a new location web service, which cannot be created via
     * {@link #newWebService(TFSConnection, Class, URI, HttpClient, ServerDataProvider, RegistrationClient)}
     * because that method might require a working location web service.
     *
     * @param fullLocationServiceURI
     *        the full {@link URI} to the location service (must not be
     *        <code>null</code>)
     * @param httpClient
     *        the {@link HttpClient} to use for accessing the web service (must
     *        not be <code>null</code>)
     * @return a {@link _LocationWebServiceSoap} (never <code>null</code>)
     * @throws URISyntaxException
     */
    public _LocationWebServiceSoap newLocationWebService(final URI fullLocationServiceURI, final HttpClient httpClient)
        throws URISyntaxException;

    /**
     * Creates a new web service implementation.
     *
     * @param webServiceInterfaceType
     *        the web service interface type (for example,
     *        {@link _CatalogWebServiceSoap}; not an implementation like
     *        {@link _CatalogWebServiceSoap12Service}) (must not be
     *        <code>null</code>)
     * @param connectionBaseURI
     *        the connection's base {@link URI} (must not be <code>null</code>)
     * @param httpClient
     *        the {@link HttpClient} to use for accessing the web service (must
     *        not be <code>null</code>)
     * @param serverDataProvider
     *        the {@link ServerDataProvider} (must not be <code>null</code>
     *        except when webServiceInterfaceType is
     *        {@link _LocationWebServiceSoap}.class)
     * @param registrationClient
     *        the {@link RegistrationClient} (may be <code>null</code> if the
     *        web service type is for a {@link TFSConfigurationServer}, which
     *        does not support the registration service)
     * @return a web service implementation that implements the web service type
     *         specified by the type parameter or <code>null</code> if the web
     *         service type is known but no endpoint is appropriate for the
     *         current connection (for example, old server which doesn't support
     *         the requested service)
     * @throws UnknownWebServiceException
     *         if the specified web service type is unknown
     */
    public Object newWebService(
        TFSConnection connection,
        Class<?> webServiceInterfaceType,
        URI connectionBaseURI,
        HttpClient httpClient,
        ServerDataProvider serverDataProvider,
        RegistrationClient registrationClient) throws URISyntaxException, UnknownWebServiceException;

    /**
     * Creates a new {@link _ListsSoap} web service.
     *
     * @param connection
     *        the {@link TFSConnection}
     * @param projectInfo
     *        the team project to create the web service for (must not be
     *        <code>null</code>)
     * @param connectionBaseURI
     *        the connection's base {@link URI} (must not be <code>null</code>)
     * @param httpClient
     *        the {@link HttpClient} to use for accessing the web service (must
     *        not be <code>null</code>)
     * @param registrationClient
     *        the {@link RegistrationClient} (must not be <code>null</code>)
     * @return a {@link _ListsSoap} web service implementation (never
     *         <code>null</code>)
     */
    public _ListsSoap newWSSWebService(
        final TFSConnection connection,
        final ProjectInfo projectInfo,
        final URI connectionBaseURI,
        final HttpClient httpClient,
        final RegistrationClient registrationClient) throws URISyntaxException;

    /**
     * Creates a new {@link _IntegrationServiceSoap} web service.
     *
     * @param connection
     *        the {@link TFSConnection}
     * @param linkingEndpoint
     *        the endpoint the linking web service should use
     * @param connectionBaseURI
     *        the connection's base {@link URI} (must not be <code>null</code>)
     * @param httpClient
     *        the {@link HttpClient} to use for accessing the web service (must
     *        not be <code>null</code>)
     * @param registrationClient
     *        the {@link RegistrationClient} (must not be <code>null</code>)
     * @return a {@link _IntegrationServiceSoap} web service implementation
     *         (never <code>null</code>)
     */
    public _IntegrationServiceSoap newLinkingWebService(
        TFSConnection connection,
        String linkingEndpoint,
        URI connectionBaseURI,
        HttpClient httpClient,
        RegistrationClient registrationClient) throws URISyntaxException;

    /**
     * Called to obtain the {@link URI} that a web service previously created by
     * this {@link WebServiceFactory} is connected to.
     *
     * @param webServiceInterfaceType
     *        the web service interface type (for example,
     *        {@link _CatalogWebServiceSoap}; not an implementation like
     *        {@link _CatalogWebServiceSoap12Service}) (must not be
     *        <code>null</code>)
     * @return the {@link URI} the web service is connected to (never
     *         <code>null</code>)
     *
     * @throws IllegalArgumentException
     *         if the argument is <code>null</code> or is not a web service
     *         previously created by this {@link WebServiceFactory}
     * @throws {@link
     *         ClassCastException} if the argument is not a web service
     *         previously created by this {@link WebServiceFactory}
     */
    public URI getWebServiceURI(Object webServiceInterfaceType)
        throws IllegalArgumentException,
            ClassCastException,
            URISyntaxException;
}
