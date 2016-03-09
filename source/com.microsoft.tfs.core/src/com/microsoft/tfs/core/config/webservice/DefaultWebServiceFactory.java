// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.webservice;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.namespace.QName;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.internal.ServerAttributes;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.framework.internal.SpecialURLs;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.reporting.ReportUtils;
import com.microsoft.tfs.core.clients.sharepoint.WSSUtils;
import com.microsoft.tfs.core.clients.workitem.internal.WITRequestIDHeaderProvider;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.core.ws.runtime.client.TransportRequestHandler;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.sql.reporting.reportingservices._ReportingService2005Soap;
import ms.sql.reporting.reportingservices._ReportingService2005SoapService;
import ms.tfs.build.buildcontroller._03._BuildControllerWebServiceSoap;
import ms.tfs.build.buildcontroller._03._BuildControllerWebServiceSoap12Service;
import ms.tfs.build.buildinfo._03._BuildStoreWebServiceSoap;
import ms.tfs.build.buildinfo._03._BuildStoreWebServiceSoap12Service;
import ms.tfs.build.buildservice._03._AdministrationWebServiceSoap;
import ms.tfs.build.buildservice._03._AdministrationWebServiceSoap12Service;
import ms.tfs.build.buildservice._03._BuildQueueWebServiceSoap;
import ms.tfs.build.buildservice._03._BuildQueueWebServiceSoap12Service;
import ms.tfs.build.buildservice._03._BuildServiceSoap;
import ms.tfs.build.buildservice._03._BuildServiceSoap12Service;
import ms.tfs.build.buildservice._03._BuildWebServiceSoap;
import ms.tfs.build.buildservice._03._BuildWebServiceSoap12Service;
import ms.tfs.build.buildservice._04._AdministrationServiceSoap;
import ms.tfs.build.buildservice._04._AdministrationServiceSoap12Service;
import ms.tfs.build.buildservice._04._BuildQueueServiceSoap;
import ms.tfs.build.buildservice._04._BuildQueueServiceSoap12Service;
import ms.tfs.services.classification._03._Classification4Soap;
import ms.tfs.services.classification._03._Classification4Soap12Service;
import ms.tfs.services.classification._03._ClassificationSoap;
import ms.tfs.services.classification._03._ClassificationSoap12Service;
import ms.tfs.services.groupsecurity._03._GroupSecurityServiceSoap;
import ms.tfs.services.groupsecurity._03._GroupSecurityServiceSoap12Service;
import ms.tfs.services.linking._03._IntegrationServiceSoap;
import ms.tfs.services.linking._03._IntegrationServiceSoapService;
import ms.tfs.services.registration._03._RegistrationSoap;
import ms.tfs.services.registration._03._RegistrationSoap12Service;
import ms.tfs.services.serverstatus._03._ServerStatusSoap;
import ms.tfs.services.serverstatus._03._ServerStatusSoap12Service;
import ms.tfs.services.teamconfiguration._01._TeamConfigurationServiceSoap;
import ms.tfs.services.teamconfiguration._01._TeamConfigurationServiceSoap12Service;
import ms.tfs.versioncontrol.admin._03._AdminSoap;
import ms.tfs.versioncontrol.admin._03._AdminSoap12Service;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap12Service;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap12Service;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap12Service;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap12Service;
import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap12Service;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap12Service;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap12Service;
import ms.tfs.workitemtracking.configurationsettingsservice._03._ConfigurationSettingsServiceSoap;
import ms.tfs.workitemtracking.configurationsettingsservice._03._ConfigurationSettingsServiceSoap12Service;
import ms.ws._CatalogWebServiceSoap;
import ms.ws._CatalogWebServiceSoap12Service;
import ms.ws._IdentityManagementWebService2Soap;
import ms.ws._IdentityManagementWebService2Soap12Service;
import ms.ws._IdentityManagementWebServiceSoap;
import ms.ws._IdentityManagementWebServiceSoap12Service;
import ms.ws._LocationWebServiceSoap;
import ms.ws._LocationWebServiceSoap12Service;
import ms.ws._SecurityWebServiceSoap;
import ms.ws._SecurityWebServiceSoap12Service;
import ms.wss._ListsSoap;
import ms.wss._ListsSoapService;

/**
 * <p>
 * A default implementation of the {@link WebServiceFactory} interface.
 * </p>
 *
 * @see WebServiceFactory
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class DefaultWebServiceFactory implements WebServiceFactory {
    private final Locale acceptLangaugeLocale;
    private final TransportRequestHandler transportRequestHandler;

    private final Object initLock = new Object();
    private boolean initialized = false;
    private final Map<Class<?>, WebServiceMetadata> serviceInterfaceClassToMetadata =
        new HashMap<Class<?>, WebServiceMetadata>();

    /**
     * Creates a {@link DefaultWebServiceFactory} that configures each new web
     * service it creates with an HTTP Accept-Language header with a value
     * derived from the given {@link Locale}.
     *
     * @param acceptLanguageLocale
     *        the {@link Locale} to use to set the Accept-Language HTTP header
     *        for requests made by web service proxies created by this factory.
     *        Specify <code>null</code> to not set the HTTP header.
     * @param transportRequestHandler
     *        handles transport authentication to hosted TFS so a web request
     *        can be prepared properly or can be retried with new credentials.
     *        Specify <code>null</code> to cause the
     *        {@link FederatedAuthException} to be rethrown.
     */
    public DefaultWebServiceFactory(
        final Locale acceptLanguageLocale,
        final TransportRequestHandler transportRequestHandler) {
        this.acceptLangaugeLocale = acceptLanguageLocale;
        this.transportRequestHandler = transportRequestHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public _RegistrationSoap newRegistrationWebService(final URI connectionBaseURI, final HttpClient httpClient)
        throws URISyntaxException {
        Check.notNull(connectionBaseURI, "connectionBaseURI"); //$NON-NLS-1$
        Check.notNull(httpClient, "httpClient"); //$NON-NLS-1$

        final WebServiceMetadata metadata = getWebServiceMetadata(_RegistrationSoap.class);
        Check.notNull(metadata, "metadata"); //$NON-NLS-1$

        /*
         * We can't use ServerDataProvider to find Registration, because
         * ServerDataProvider might be implemented using Registration, so find
         * it directly using the default endpoint path.
         */
        URI webServiceURI = resolveEndpointURI(connectionBaseURI, metadata.getDefaultEndpointPath());
        webServiceURI = getSafeURI(webServiceURI);

        final _RegistrationSoap service = (_RegistrationSoap) metadata.getInstantiator().newWebServiceImplementation(
            httpClient,
            webServiceURI,
            metadata.getPortQName());

        ((SOAPService) service).setAcceptLanguage(acceptLangaugeLocale);
        ((SOAPService) service).addTransportRequestHandler(transportRequestHandler);

        return service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public _LocationWebServiceSoap newLocationWebService(final URI fullLocationServiceURI, final HttpClient httpClient)
        throws URISyntaxException {
        Check.notNull(fullLocationServiceURI, "fullLocationServiceURI"); //$NON-NLS-1$
        Check.notNull(httpClient, "httpClient"); //$NON-NLS-1$

        final WebServiceMetadata metadata = getWebServiceMetadata(_LocationWebServiceSoap.class);
        Check.notNull(metadata, "metadata"); //$NON-NLS-1$

        final URI webServiceURI = getSafeURI(fullLocationServiceURI);

        final _LocationWebServiceSoap service =
            (_LocationWebServiceSoap) metadata.getInstantiator().newWebServiceImplementation(
                httpClient,
                webServiceURI,
                metadata.getPortQName());

        ((SOAPService) service).setAcceptLanguage(acceptLangaugeLocale);
        ((SOAPService) service).addTransportRequestHandler(transportRequestHandler);

        return service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object newWebService(
        final TFSConnection connection,
        final Class<?> webServiceInterfaceType,
        final URI connectionBaseURI,
        final HttpClient httpClient,
        final ServerDataProvider serverDataProvider,
        final RegistrationClient registrationClient) throws URISyntaxException, UnknownWebServiceException {
        Check.notNull(webServiceInterfaceType, "webServiceInterfaceType"); //$NON-NLS-1$
        Check.notNull(connectionBaseURI, "connectionBaseURI"); //$NON-NLS-1$
        Check.notNull(httpClient, "httpClient"); //$NON-NLS-1$
        Check.notNull(serverDataProvider, "serverDataProvider"); //$NON-NLS-1$

        final WebServiceMetadata metadata = getWebServiceMetadata(webServiceInterfaceType);

        if (metadata == null) {
            throw new UnknownWebServiceException(webServiceInterfaceType);
        }

        /*
         * Reporting service must be resolved in a special way.
         */
        String endpointPath = null;
        if (webServiceInterfaceType == _ReportingService2005Soap.class) {
            Check.isTrue(
                connection instanceof TFSTeamProjectCollection,
                "connection instanceof TFSTeamProjectCollection"); //$NON-NLS-1$
            Check.notNull(registrationClient, "registrationClient"); //$NON-NLS-1$

            /*
             * 2010 introduces a new way to resolve the reporting service. Try
             * this first.
             */
            endpointPath = ReportUtils.getReportServiceURL((TFSTeamProjectCollection) connection);
        }

        /*
         * Use ServerDataProvider to resolve the endpoint if still not resolved.
         */
        if (endpointPath == null && metadata.getServiceInterfaceName() != null) {
            endpointPath = getServerDataProviderEndpointPath(
                serverDataProvider,
                metadata.getServiceInterfaceName(),
                metadata.getServiceInterfaceIdentifier());
        }

        /*
         * Even though there's a default endpoint in the metadata, don't use it
         * because TFS's location and registration services are authoritative.
         *
         * This web service may not be available for older server versions, and
         * we don't want to return an instance that unavailable for this
         * connection.
         */
        if (endpointPath == null) {
            return null;
        }

        URI webServiceURI = resolveEndpointURI(connectionBaseURI, endpointPath);
        if (!isConnection2010OrLater(connection)) {
            webServiceURI = rewriteInternalHostToConnectionHost(webServiceURI, connectionBaseURI, registrationClient);
        }
        webServiceURI = getSafeURI(webServiceURI);

        final Object service =
            metadata.getInstantiator().newWebServiceImplementation(httpClient, webServiceURI, metadata.getPortQName());

        ((SOAPService) service).setAcceptLanguage(acceptLangaugeLocale);
        ((SOAPService) service).addTransportRequestHandler(transportRequestHandler);

        return service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public _IntegrationServiceSoap newLinkingWebService(
        final TFSConnection connection,
        final String linkingEndpoint,
        final URI connectionBaseURI,
        final HttpClient httpClient,
        final RegistrationClient registrationClient) throws URISyntaxException {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNullOrEmpty(linkingEndpoint, "linkingEndpoint"); //$NON-NLS-1$
        Check.notNull(connectionBaseURI, "connectionBaseURI"); //$NON-NLS-1$
        Check.notNull(httpClient, "httpClient"); //$NON-NLS-1$
        Check.notNull(registrationClient, "registrationClient"); //$NON-NLS-1$

        final WebServiceMetadata metadata = getWebServiceMetadata(_IntegrationServiceSoap.class);
        Check.notNull(metadata, "metadata"); //$NON-NLS-1$

        /*
         * The server data provider is not used to resolve the linking endpoint,
         * the parameter is used instead.
         */

        URI webServiceURI = resolveEndpointURI(connectionBaseURI, linkingEndpoint);
        if (!isConnection2010OrLater(connection)) {
            webServiceURI = rewriteInternalHostToConnectionHost(webServiceURI, connectionBaseURI, registrationClient);
        }
        webServiceURI = getSafeURI(webServiceURI);

        final _IntegrationServiceSoap service =
            (_IntegrationServiceSoap) metadata.getInstantiator().newWebServiceImplementation(
                httpClient,
                webServiceURI,
                metadata.getPortQName());

        ((SOAPService) service).setAcceptLanguage(acceptLangaugeLocale);
        ((SOAPService) service).addTransportRequestHandler(transportRequestHandler);

        return service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public _ListsSoap newWSSWebService(
        final TFSConnection connection,
        final ProjectInfo projectInfo,
        final URI connectionBaseURI,
        final HttpClient httpClient,
        final RegistrationClient registrationClient) throws URISyntaxException {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.isTrue(connection instanceof TFSTeamProjectCollection, "connection is TeamProjectCollection"); //$NON-NLS-1$
        Check.notNull(connectionBaseURI, "connectionBaseURI"); //$NON-NLS-1$
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$
        Check.notNull(httpClient, "httpClient"); //$NON-NLS-1$
        Check.notNull(registrationClient, "registrationClient"); //$NON-NLS-1$

        final WebServiceMetadata metadata = getWebServiceMetadata(_ListsSoap.class);
        Check.notNull(metadata, "metadata"); //$NON-NLS-1$

        /*
         * The server data provider is not used to resolve the endpoint, the
         * custom WSS method is used instead.
         */

        final String endpointPath = WSSUtils.getWSSURL((TFSTeamProjectCollection) connection, projectInfo);

        URI webServiceURI = new URI(endpointPath);
        webServiceURI = URIUtils.ensurePathHasTrailingSlash(webServiceURI);
        webServiceURI = URIUtils.resolve(webServiceURI, "_vti_bin/Lists.asmx"); //$NON-NLS-1$
        if (!isConnection2010OrLater(connection)) {
            webServiceURI = rewriteInternalHostToConnectionHost(webServiceURI, connectionBaseURI, registrationClient);
        }
        webServiceURI = getSafeURI(webServiceURI);

        final _ListsSoap service = (_ListsSoap) metadata.getInstantiator().newWebServiceImplementation(
            httpClient,
            webServiceURI,
            metadata.getPortQName());

        ((SOAPService) service).setAcceptLanguage(acceptLangaugeLocale);
        ((SOAPService) service).addTransportRequestHandler(transportRequestHandler);

        return service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getWebServiceURI(final Object webService)
        throws IllegalArgumentException,
            ClassCastException,
            URISyntaxException {
        final SOAPService stub = (SOAPService) webService;
        return stub.getEndpoint();
    }

    /**
     * Resolve a given endpoint path to the server URI. Will remove any leading
     * slash at the start of the endpoint path as they are stored as absolute
     * paths but want to be applied onto the end of the server URI (in TFS2010)
     */
    private URI resolveEndpointURI(final URI connectionBaseURI, String endpointPath) {
        if (endpointPath.startsWith("/")) //$NON-NLS-1$
        {
            endpointPath = endpointPath.substring(1);
        }
        return connectionBaseURI.resolve(endpointPath);
    }

    /**
     * Convert the given {@link URI} to a new {@link URI} by converting to a
     * US-ASCII string and then constructing a {@link URI} from this string.
     * This avoids I18n issues with the Apache Commons URI class when accepting
     * UTF-8 text.
     */
    private URI getSafeURI(final URI input) throws URISyntaxException {
        return new URI(input.toASCIIString());
    }

    private boolean isConnection2010OrLater(final TFSConnection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        if (connection instanceof TFSConfigurationServer) {
            return true;
        }

        if (connection instanceof TFSTeamProjectCollection
            && ((TFSTeamProjectCollection) connection).getConfigurationServer() != null) {
            return true;
        }

        return false;
    }

    /**
     * This method should not be used for TFS 2010 and later servers because the
     * location service always gives us usable endpoint URIs for all services.
     * This method would mangle those URIs.
     * <p>
     * VS 2005 and VS 2008 can return service endpoints for services like SQL
     * reports that use an "internal" host name of the AT, which is often
     * missing the full domain name and may not even be resolvable by the client
     * (because the AT is behind a reverse proxy and the client used an
     * "external" name to connect to the AT).
     * <p>
     * This method checks a service endpoint URI (SQL reports, for example), and
     * if its host part differs from the connection base URI's host part (the
     * "external" name that we know we can connect with), the registration
     * service is queried to get the canonical AT host name. If this canonical
     * AT host name matches the service endpoint URI host name, we've detected
     * an "internal" host name leaking out and we should return a URI that uses
     * the "external" (base connection) host.
     * <p>
     * If the given {@link RegistrationClient} is <code>null</code> or we cannot
     * do the AT queries to check the names, the input URI is returned
     * unaltered.
     */
    private URI rewriteInternalHostToConnectionHost(
        final URI serviceEndpointURI,
        final URI connectionBaseURI,
        final RegistrationClient registrationClient) throws URISyntaxException {
        Check.notNull(serviceEndpointURI, "input"); //$NON-NLS-1$

        if (registrationClient == null) {
            return serviceEndpointURI;
        }

        final String serviceEndpointHost = URIUtils.safeGetHost(serviceEndpointURI);
        final String connectionBaseHost = URIUtils.safeGetHost(connectionBaseURI);

        /*
         * If the service endpoint didn't have a host (shouldn't happen) or the
         * host is the same as the connection base (a good "external" host
         * name), just return the endpoint because it should be reachable.
         */
        if (serviceEndpointHost == null || serviceEndpointHost.equalsIgnoreCase(connectionBaseHost)) {
            return serviceEndpointURI;
        }

        final String atMachineName = registrationClient.getExtendedAttributeValue(
            ToolNames.TEAM_FOUNDATION,
            ServerAttributes.APP_TIER_MACHINE_NAME_PROPERTY);

        /*
         * If the AT's machine name doesn't match the endpoint host name, the
         * host difference isn't due to leaking an "internal" name (perhaps
         * intentionally configured other host for this service?), so return the
         * original service endpoint URI.
         */
        if (atMachineName == null || !serviceEndpointHost.equalsIgnoreCase(atMachineName)) {
            return serviceEndpointURI;
        }

        /*
         * Replace that "internal" hostname with the "external" one.
         */
        return new URI(
            serviceEndpointURI.getScheme(),
            serviceEndpointURI.getUserInfo(),
            connectionBaseURI.getHost(),
            serviceEndpointURI.getPort(),
            serviceEndpointURI.getPath(),
            serviceEndpointURI.getQuery(),
            serviceEndpointURI.getFragment());
    }

    private String getServerDataProviderEndpointPath(
        final ServerDataProvider serverDataProvider,
        final String serviceInterfaceName,
        final GUID serviceInterfaceIdentifier) {
        Check.notNull(serverDataProvider, "serverDataProvider"); //$NON-NLS-1$
        Check.notNull(serviceInterfaceName, "serviceInterfaceName"); //$NON-NLS-1$

        return serverDataProvider.locationForCurrentConnection(serviceInterfaceName, serviceInterfaceIdentifier);
    }

    private WebServiceMetadata getWebServiceMetadata(final Class<?> webServiceInterfaceType) {
        synchronized (initLock) {
            if (initialized == false) {
                initialize();
                initialized = true;
            }

            return serviceInterfaceClassToMetadata.get(webServiceInterfaceType);
        }
    }

    private void initialize() {
        /*
         * Registration
         */
        serviceInterfaceClassToMetadata.put(
            _RegistrationSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.REGISTRATION,
                ServiceInterfaceIdentifiers.REGISTRATION,
                SpecialURLs.DEFAULT_REGISTRATION_ENDPOINT,
                _RegistrationSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _RegistrationSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Location
         *
         * Note the null endpoint. The full location service URI is always
         * passed to newLocationWebService().
         */
        serviceInterfaceClassToMetadata.put(
            _LocationWebServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.LOCATION,
                ServiceInterfaceIdentifiers.LOCATION,
                null,
                _LocationWebServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _LocationWebServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Common Structure (a.k.a. Classification)
         */
        serviceInterfaceClassToMetadata.put(
            _ClassificationSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.COMMON_STRUCTURE,
                ServiceInterfaceIdentifiers.COMMON_STRUCTURE,
                _ClassificationSoap12Service.getEndpointPath(),
                _ClassificationSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _ClassificationSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Common Structure (a.k.a. Classification) V4
         */
        serviceInterfaceClassToMetadata.put(
            _Classification4Soap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.COMMON_STRUCTURE_4,
                ServiceInterfaceIdentifiers.COMMON_STRUCTURE_4,
                _Classification4Soap12Service.getEndpointPath(),
                _Classification4Soap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _Classification4Soap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Reporting
         *
         * Note the lack of service identifier and default endpoint. This
         * service isn't located via the ServerDataProvider, so it doesn't need
         * these.
         */
        serviceInterfaceClassToMetadata.put(
            _ReportingService2005Soap.class,
            new WebServiceMetadata(
                null,
                null,
                _ReportingService2005SoapService.getEndpointPath(),
                _ReportingService2005SoapService.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        final _ReportingService2005SoapService service =
                            new _ReportingService2005SoapService(httpClient, endpoint, port);

                        // Reporting services are likely to have authentication
                        // misconfigured, don't prompt.
                        service.setPromptForCredentials(false);

                        return service;
                    }
                }));

        /*
         * Server Status
         */
        serviceInterfaceClassToMetadata.put(
            _ServerStatusSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.SERVER_STATUS,
                ServiceInterfaceIdentifiers.SERVER_STATUS,
                _ServerStatusSoap12Service.getEndpointPath(),
                _ServerStatusSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _ServerStatusSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Version Control
         */
        serviceInterfaceClassToMetadata.put(
            _RepositorySoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.VERSION_CONTROL,
                ServiceInterfaceIdentifiers.VERSION_CONTROL,
                _RepositorySoap12Service.getEndpointPath(),
                _RepositorySoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _RepositorySoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Version Control Extensions (TFS 2010 / Dev 10)
         */
        serviceInterfaceClassToMetadata.put(
            _RepositoryExtensionsSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.VERSION_CONTROL_3,
                ServiceInterfaceIdentifiers.VERSION_CONTROL_3,
                _RepositoryExtensionsSoap12Service.getEndpointPath(),
                _RepositoryExtensionsSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _RepositoryExtensionsSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Version Control Extensions (TFS 2012 / Dev 11)
         */
        serviceInterfaceClassToMetadata.put(
            _Repository4Soap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.VERSION_CONTROL_4,
                ServiceInterfaceIdentifiers.VERSION_CONTROL_4,
                _Repository4Soap12Service.getEndpointPath(),
                _Repository4Soap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _Repository4Soap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Version Control Extensions (TFS 2012 QU1 / Dev 11)
         */
        serviceInterfaceClassToMetadata.put(
            _Repository5Soap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.VERSION_CONTROL_5,
                ServiceInterfaceIdentifiers.VERSION_CONTROL_5,
                _Repository5Soap12Service.getEndpointPath(),
                _Repository5Soap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _Repository5Soap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Version Control Administration
         */
        serviceInterfaceClassToMetadata.put(
            _AdminSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.VERSION_CONTROL_ADMIN,
                ServiceInterfaceIdentifiers.VERSION_CONTROL_ADMIN,
                _AdminSoap12Service.getEndpointPath(),
                _AdminSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _AdminSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Security Service
         */
        serviceInterfaceClassToMetadata.put(
            _SecurityWebServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.SECURITY,
                ServiceInterfaceIdentifiers.SECURITY,
                _SecurityWebServiceSoap12Service.getEndpointPath(),
                _SecurityWebServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _SecurityWebServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Work Item Configuration Settings
         */
        serviceInterfaceClassToMetadata.put(
            _ConfigurationSettingsServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.WORK_ITEM_CONFIG,
                ServiceInterfaceIdentifiers.WORK_ITEM_CONFIG,
                _ConfigurationSettingsServiceSoap12Service.getEndpointPath(),
                _ConfigurationSettingsServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _ConfigurationSettingsServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Work Item Tracking
         */
        serviceInterfaceClassToMetadata.put(
            _ClientService2Soap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.WORK_ITEM,
                ServiceInterfaceIdentifiers.WORK_ITEM,
                _ClientService2Soap12Service.getEndpointPath(),
                _ClientService2Soap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        final _ClientService2Soap12Service service =
                            new _ClientService2Soap12Service(httpClient, endpoint, port);

                        service.setSOAPHeaderProvider(new WITRequestIDHeaderProvider());

                        return service;
                    }
                }));

        /*
         * Work Item Tracking (TFS 2010)
         */
        serviceInterfaceClassToMetadata.put(
            _ClientService3Soap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.WORK_ITEM_3,
                ServiceInterfaceIdentifiers.WORK_ITEM_3,
                _ClientService3Soap12Service.getEndpointPath(),
                _ClientService3Soap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        final _ClientService3Soap12Service service =
                            new _ClientService3Soap12Service(httpClient, endpoint, port);

                        service.setSOAPHeaderProvider(new WITRequestIDHeaderProvider());

                        return service;
                    }
                }));

        /*
         * Work Item Tracking (Dev11)
         */
        serviceInterfaceClassToMetadata.put(
            _ClientService5Soap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.WORK_ITEM_5,
                ServiceInterfaceIdentifiers.WORK_ITEM_5,
                _ClientService5Soap12Service.getEndpointPath(),
                _ClientService5Soap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        final _ClientService5Soap12Service service =
                            new _ClientService5Soap12Service(httpClient, endpoint, port);

                        service.setSOAPHeaderProvider(new WITRequestIDHeaderProvider());

                        return service;
                    }
                }));

        /*
         * Linking
         *
         * Note the lack of service identifier and default endpoint. This
         * service isn't located via the ServerDataProvider, so it doesn't need
         * these.
         */
        serviceInterfaceClassToMetadata.put(
            _IntegrationServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.LINKING,
                null,
                null,
                new QName(_IntegrationServiceSoapService.getPortQName().getNamespaceURI(), ""), //$NON-NLS-1$
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _IntegrationServiceSoapService(httpClient, endpoint, port);
                    }
                }));

        /*
         * Group Security Service
         */
        serviceInterfaceClassToMetadata.put(
            _GroupSecurityServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.GROUP_SECURITY,
                ServiceInterfaceIdentifiers.GROUP_SECURITY,
                _GroupSecurityServiceSoap12Service.getEndpointPath(),
                _GroupSecurityServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _GroupSecurityServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Windows Sharepoint Services
         *
         * Note the lack of interface ID. This service isn't located via the
         * ServerDataProvider, so it doesn't need one.
         */
        serviceInterfaceClassToMetadata.put(
            _ListsSoap.class,
            new WebServiceMetadata(
                null,
                null,
                _ListsSoapService.getEndpointPath(),
                _ListsSoapService.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        final _ListsSoapService service = new _ListsSoapService(httpClient, endpoint, port);

                        // Sharepoint services are likely to have authentication
                        // misconfigured, don't prompt.
                        service.setPromptForCredentials(false);

                        return service;
                    }
                }));

        /*
         * Build Service
         */
        serviceInterfaceClassToMetadata.put(
            _BuildServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.BUILD,
                ServiceInterfaceIdentifiers.BUILD,
                _BuildServiceSoap12Service.getEndpointPath(),
                _BuildServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _BuildServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Build Service v3 (BuildWebServiceSoap)
         */
        serviceInterfaceClassToMetadata.put(
            _BuildWebServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.BUILD_3,
                ServiceInterfaceIdentifiers.BUILD_3,
                _BuildWebServiceSoap12Service.getEndpointPath(),
                _BuildWebServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _BuildWebServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Build Service v4 (BuildServiceSoap)
         */
        serviceInterfaceClassToMetadata.put(
            ms.tfs.build.buildservice._04._BuildServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.BUILD_4,
                ServiceInterfaceIdentifiers.BUILD_4,
                ms.tfs.build.buildservice._04._BuildServiceSoap12Service.getEndpointPath(),
                ms.tfs.build.buildservice._04._BuildServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new ms.tfs.build.buildservice._04._BuildServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Build Queue Service (BuildQueueWebServiceSoap)
         */
        serviceInterfaceClassToMetadata.put(
            _BuildQueueWebServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.BUILD_QUEUE_SERVICE,
                ServiceInterfaceIdentifiers.BUILD_QUEUE_SERVICE,
                _BuildQueueWebServiceSoap12Service.getEndpointPath(),
                _BuildQueueWebServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _BuildQueueWebServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Build Queue Service v4 (BuildQueueServiceSoap)
         */
        serviceInterfaceClassToMetadata.put(
            _BuildQueueServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.BUILD_QUEUE_SERVICE_4,
                ServiceInterfaceIdentifiers.BUILD_QUEUE_SERVICE_4,
                _BuildQueueServiceSoap12Service.getEndpointPath(),
                _BuildQueueServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _BuildQueueServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Build Administration Service (AdministrationWebService)
         */
        serviceInterfaceClassToMetadata.put(
            _AdministrationWebServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.BUILD_ADMINISTRATION_SERVICE,
                ServiceInterfaceIdentifiers.BUILD_ADMINISTRATION_SERVICE,
                _AdministrationWebServiceSoap12Service.getEndpointPath(),
                _AdministrationWebServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _AdministrationWebServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Build Administration Service 4 (AdministrationWebService4)
         */
        serviceInterfaceClassToMetadata.put(
            _AdministrationServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.BUILD_ADMINISTRATION_SERVICE_4,
                ServiceInterfaceIdentifiers.BUILD_ADMINISTRATION_SERVICE_4,
                _AdministrationServiceSoap12Service.getEndpointPath(),
                _AdministrationServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _AdministrationServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Build Controller
         */
        serviceInterfaceClassToMetadata.put(
            _BuildControllerWebServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.BUILD_CONTROLLER,
                ServiceInterfaceIdentifiers.BUILD_CONTROLLER,
                _BuildControllerWebServiceSoap12Service.getEndpointPath(),
                _BuildControllerWebServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _BuildControllerWebServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Build Store
         */
        serviceInterfaceClassToMetadata.put(
            _BuildStoreWebServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.BUILD_STORE,
                ServiceInterfaceIdentifiers.BUILD_STORE,
                _BuildStoreWebServiceSoap12Service.getEndpointPath(),
                _BuildStoreWebServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _BuildStoreWebServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Catalog
         */
        serviceInterfaceClassToMetadata.put(
            _CatalogWebServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.CATALOG,
                ServiceInterfaceIdentifiers.CATALOG,
                _CatalogWebServiceSoap12Service.getEndpointPath(),
                _CatalogWebServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _CatalogWebServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Collection Identity Management v1
         */
        serviceInterfaceClassToMetadata.put(
            _IdentityManagementWebServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.IDENTITY_MANAGEMENT,
                ServiceInterfaceIdentifiers.COLLECTION_IDENTITY_MANAGEMENT,
                _IdentityManagementWebServiceSoap12Service.getEndpointPath(),
                _IdentityManagementWebServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _IdentityManagementWebServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Collection Identity Management v2
         */
        serviceInterfaceClassToMetadata.put(
            _IdentityManagementWebService2Soap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.IDENTITY_MANAGEMENT_2,
                ServiceInterfaceIdentifiers.COLLECTION_IDENTITY_MANAGEMENT_2,
                _IdentityManagementWebService2Soap12Service.getEndpointPath(),
                _IdentityManagementWebService2Soap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _IdentityManagementWebService2Soap12Service(httpClient, endpoint, port);
                    }
                }));

        /*
         * Team Configuration
         */
        serviceInterfaceClassToMetadata.put(
            _TeamConfigurationServiceSoap.class,
            new WebServiceMetadata(
                ServiceInterfaceNames.TEAM_CONFIGURATION,
                ServiceInterfaceIdentifiers.TEAM_CONFIGURATION,
                _TeamConfigurationServiceSoap12Service.getEndpointPath(),
                _TeamConfigurationServiceSoap12Service.getPortQName(),
                new WebServiceInstantiator() {
                    @Override
                    public Object newWebServiceImplementation(
                        final HttpClient httpClient,
                        final URI endpoint,
                        final QName port) {
                        return new _TeamConfigurationServiceSoap12Service(httpClient, endpoint, port);
                    }
                }));
    }

    private static interface WebServiceInstantiator {
        public Object newWebServiceImplementation(HttpClient httpClient, URI endpoint, QName port);
    }

    private static class WebServiceMetadata {
        private final String serviceInterfaceName;
        private final GUID serviceInterfaceIdentifier;
        private final String defaultEndpointPath;
        private final QName port;
        private final WebServiceInstantiator instantiator;

        public WebServiceMetadata(
            final String serviceInterfaceName,
            final GUID serviceInterfaceIdentifier,
            final String defaultEndpointPath,
            final QName port,
            final WebServiceInstantiator instantiator) {
            this.serviceInterfaceName = serviceInterfaceName;
            this.serviceInterfaceIdentifier = serviceInterfaceIdentifier;
            this.defaultEndpointPath = defaultEndpointPath;
            this.port = port;
            this.instantiator = instantiator;
        }

        public String getServiceInterfaceName() {
            return serviceInterfaceName;
        }

        public GUID getServiceInterfaceIdentifier() {
            return serviceInterfaceIdentifier;
        }

        public String getDefaultEndpointPath() {
            return defaultEndpointPath;
        }

        public QName getPortQName() {
            return port;
        }

        public WebServiceInstantiator getInstantiator() {
            return instantiator;
        }
    }
}
