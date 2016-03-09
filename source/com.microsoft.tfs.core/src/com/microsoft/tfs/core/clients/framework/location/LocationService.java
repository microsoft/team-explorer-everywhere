// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.microsoft.tfs.core.ServerCapabilities;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.framework.location.exceptions.AccessPointIsMalformedURLException;
import com.microsoft.tfs.core.clients.framework.location.exceptions.InvalidAccessPointException;
import com.microsoft.tfs.core.clients.framework.location.exceptions.InvalidServiceDefinitionException;
import com.microsoft.tfs.core.clients.framework.location.exceptions.LocationMethodNotImplementedException;
import com.microsoft.tfs.core.clients.framework.location.exceptions.ServiceDefinitionDoesNotExistException;
import com.microsoft.tfs.core.clients.framework.location.internal.LocationCacheManager;
import com.microsoft.tfs.core.clients.framework.location.internal.LocationWebServiceProxy;
import com.microsoft.tfs.core.clients.framework.location.internal.ServiceTypeFilter;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * The TFS location service. Implements the {@link ILocationService} interface
 * via the {@link ServerDataProvider} (which extends {@link ILocationService} to
 * provide some more convenient methods).
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class LocationService implements ServerDataProvider {
    private final PersistenceStoreProvider persistenceProvider;
    private final URI connectionBaseURI;
    private final LocationWebServiceProxy webServiceProxy;

    private LocationCacheManager locationCacheManager;
    private final Object locationCacheManagerLock = new Object();

    private final Object connectionLock = new Object();
    private boolean connectionMade = false;
    private ConnectOptions validConnectionData = ConnectOptions.NONE;

    // Members to supply ServerDataProvider methods.

    private TeamFoundationIdentity authorizedIdentity;
    private TeamFoundationIdentity authenticatedIdentity;
    private GUID instanceId;
    private GUID catalogResourceId;
    private ServerCapabilities serverCapabilities;

    // Covers the four ServerDataProvider fields (above)
    private final Object serverDataLock = new Object();

    /**
     * Construct the TFS Location Service.
     *
     * @param connection
     *        A TFS connection (must not be <code>null</code>)
     */
    public LocationService(final TFSConnection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        webServiceProxy = new LocationWebServiceProxy(connection);

        /*
         * Cache these here (they won't change in the connection) to avoid
         * contention for locks on the connection when we need to use them to
         * refresh our data from the server.
         */
        persistenceProvider = connection.getPersistenceStoreProvider();
        connectionBaseURI = connection.getBaseURI();

        /*
         * We need the server's instance GUID to load the cache data, but we
         * can't ask the TFSConnection object for its GUID directly, because it
         * must construct a ServerDataProvider to get the GUID, and that
         * ServerDataProvider may be a LocationService (this class; infinite
         * loop!).
         *
         * Pass GUID.EMPTY and the location cache manager will try to find the
         * correct GUID in the registration service's map cache.
         */

        locationCacheManager =
            new LocationCacheManager(persistenceProvider.getCachePersistenceStore(), GUID.EMPTY, connectionBaseURI);
    }

    protected LocationCacheManager getLocationCacheManager() {
        synchronized (locationCacheManagerLock) {
            return locationCacheManager;
        }
    }

    /**
     * Configures the AccessMapping with the provided moniker to have the
     * provided display name and access point. This function also allows for
     * this AccessMapping to be made the default AccessMapping.
     *
     * @param moniker
     *        A string that uniquely identifies this AccessMapping. This value
     *        cannot be null or empty.
     *
     * @param displayName
     *        Display name for this AccessMapping. This value cannot be null or
     *        empty.
     *
     * @param accessPoint
     *        This is the base URL for the server that will map to this
     *        AccessMapping. This value cannot be null or empty.
     *
     *        The access point should consist of the scheme, authority, port and
     *        web application virtual path of the target-able server address.
     *        For example, an access point will most commonly look like this:
     *        http://server:8080/tfs/
     *
     * @param makeDefault
     *        If true, this AccessMapping will be made the default
     *        AccessMapping. If false, the default AccessMapping will not
     *        change.
     *
     * @return The AccessMapping object that was just configured.
     *
     * @exception InvalidAccessPointException
     *            Thrown if the access point for this AccessMapping is invalid
     *            or if it conflicts with an already registered access point.
     */
    @Override
    public AccessMapping configureAccessMapping(
        final String moniker,
        final String displayName,
        final String accessPoint,
        final boolean makeDefault) {
        throw new LocationMethodNotImplementedException("configureAccessMapping"); //$NON-NLS-1$
    }

    /**
     * Finds the ServiceDefinition with the specified service type and service
     * identifier. If no matching ServiceDefinition exists, null is returned.
     *
     * @param serviceType
     *        The service type of the ServiceDefinition to find.
     *
     * @param serviceIdentifier
     *        The service identifier of the ServiceDefinition to find.
     *
     * @return The ServiceDefinition with the specified service type and service
     *         identifier. If no matching ServiceDefinition exists, null is
     *         returned.
     */
    @Override
    public ServiceDefinition findServiceDefinition(final String serviceType, final GUID serviceIdentifier) {
        Check.notNull(serviceType, "serviceType"); //$NON-NLS-1$

        // Look in the cache.
        final ServiceDefinition definition = getLocationCacheManager().findService(serviceType, serviceIdentifier);

        // If we had a cache miss, go to the server to see if our cache is
        // current
        if (definition == null) {
            checkForServerUpdates();

            // Try again to see if we can find it now in case that something has
            // updated.
            return getLocationCacheManager().findService(serviceType, serviceIdentifier);
        }

        return definition;
    }

    /**
     * Finds the ServiceDefinitions for all of the services with the specified
     * service type. If no ServiceDefinitions of this type exist, an empty
     * enumeration will be returned.
     *
     * @param serviceType
     *        The case-insensitive string that identifies what type of service
     *        is being requested. If this value is null, ServiceDefinitions for
     *        all services registered with this location service will be
     *        returned.
     *
     * @return ServiceDefinitions for all of the services with the specified
     *         service type. If no ServiceDefinitions of this type exist, an
     *         empty enumeration will be returned.
     */
    @Override
    public ServiceDefinition[] findServiceDefinitions(final String serviceType) {
        // Look in the cache.
        final ServiceDefinition[] definitions = getLocationCacheManager().findServices(serviceType);

        // If we had a cache miss, go to the server to see if our cache is
        // current
        if (definitions == null) {
            checkForServerUpdates();

            // Try again to see if we can find it now in case that something has
            // updated.
            return getLocationCacheManager().findServices(serviceType);
        }

        return definitions;
    }

    /**
     * Finds the ServiceDefinitions for all of the services with the specified
     * tool type. If no services exist for this tool type, an empty enumeration
     * will be returned.
     *
     * @param toolType
     *        The case-insensitive string that will match the tool type of a set
     *        of ServiceDefinitions. If null or empty is passed in for this
     *        value then all of the ServiceDefinitions will be returned.
     *
     * @return ServiceDefinitions for all of the services with the specified
     *         tool type. If no services exist for this tool type, an empty
     *         enumeration will be returned.
     */
    @Override
    public ServiceDefinition[] findServiceDefinitionsByToolType(final String toolType) {
        // Look in the cache.
        final ServiceDefinition[] definitions = getLocationCacheManager().findServicesByToolID(toolType);

        // If we had a cache miss, go to the server to see if our cache is
        // current
        if (definitions == null) {
            checkForServerUpdates();

            // Try again to see if we can find it now in case that something has
            // updated.
            return getLocationCacheManager().findServicesByToolID(toolType);
        }

        // Return an empty array instead of null for this method.
        return definitions == null ? new ServiceDefinition[0] : definitions;
    }

    /**
     * Gets the AccessMapping with the specified moniker. Returns null if an
     * AccessMapping with the supplied moniker does not exist.
     *
     * @param moniker
     *        The moniker for the desired AccessMapping. This value cannot be
     *        null or empty.
     *
     * @return The AccessMapping with the supplied moniker or null if one does
     *         not exist.
     */
    @Override
    public AccessMapping getAccessMapping(final String moniker) {
        Check.notNull(moniker, "moniker"); //$NON-NLS-1$

        ensureConnected(ConnectOptions.INCLUDE_SERVICES);
        return locationCacheManager.getAccessMapping(moniker);
    }

    /**
     * The AccessMapping for the current connection to the server. Note, it is
     * possible that the current ClientAccessMapping is not a member of the
     * ConfiguredAccessMappings if the access point this client used to connect
     * to the server has not been configured on it. This will never be null.
     *
     * @return The AccessMapping for the current connection to the server.
     */
    @Override
    public AccessMapping getClientAccessMapping() {
        AccessMapping clientAccessMapping = getLocationCacheManager().getClientAccessMapping();

        if (clientAccessMapping == null) {
            ensureConnected(ConnectOptions.INCLUDE_SERVICES);
            clientAccessMapping = getLocationCacheManager().getClientAccessMapping();
        }

        return clientAccessMapping;
    }

    /**
     * All of the AccessMappings that this location service knows about. Because
     * a given location service can inherit AccessMappings from its parent these
     * AccessMappings may exist on this location service or its parent.
     *
     * @return All of the AccessMappings that this location service knows about.
     */
    @Override
    public AccessMapping[] getConfiguredAccessMappings() {
        ensureConnected(ConnectOptions.INCLUDE_SERVICES);
        return getLocationCacheManager().getAccessMappings();
    }

    /**
     * All of the AccessMappings that this location service knows about. Because
     * a given location service can inherit AccessMappings from its parent these
     * AccessMappings may exist on this location service or its parent.
     *
     * @return All of the AccessMappings that this location service knows about.
     */
    @Override
    public AccessMapping getDefaultAccessMapping() {
        AccessMapping defaultAccessMapping = locationCacheManager.getDefaultAccessMapping();

        if (defaultAccessMapping == null) {
            ensureConnected(ConnectOptions.INCLUDE_SERVICES);
            defaultAccessMapping = getLocationCacheManager().getDefaultAccessMapping();
        }

        return defaultAccessMapping;
    }

    /**
     * Returns the location for the ServiceDefinition that has the specified
     * service type and service identifier for the provided AccessMapping. If
     * this ServiceDefinition is FullyQualified and no LocationMapping exists
     * for this AccessMapping then null will be returned.
     *
     * @param serviceType
     *        The service type of the ServiceDefinition to find the location
     *        for.
     *
     * @param serviceIdentifier
     *        The service identifier of the ServiceDefinition to find the
     *        location for.
     *
     * @param accessMapping
     *        The AccessMapping to find the location for.
     *
     * @return The location for the ServiceDefinition for the provided
     *         AccessMapping. If this ServiceDefinition is FullyQualified and no
     *         LocationMapping exists for this AccessMapping then null will be
     *         returned.
     *
     * @exception InvalidServiceDefinitionException
     *            The associated ServiceDefinition is not valid and no location
     *            can be found.
     *
     * @exception ServiceDefinitionDoesNotExistException
     *            A ServiceDefinition with the provided service type and
     *            identifier does not exist.
     *
     * @exception InvalidAccessPointException
     *            The AccessMapping passed in does not have a valid access
     *            point.
     */
    @Override
    public String locationForAccessMapping(
        final String serviceType,
        final GUID serviceIdentifier,
        final AccessMapping accessMapping) {
        final ServiceDefinition serviceDefinition = findServiceDefinition(serviceType, serviceIdentifier);

        if (serviceDefinition == null) {
            throw new ServiceDefinitionDoesNotExistException(serviceType, serviceIdentifier);
        }

        return locationForAccessMapping(serviceDefinition, accessMapping);
    }

    /**
     * Returns the location for the ServiceDefinition for the provided
     * AccessMapping. If this ServiceDefinition is FullyQualified and no
     * LocationMapping exists for this AccessMapping then null will be returned.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to find the location for.
     *
     * @param accessMapping
     *        The AccessMapping to find the location for.
     *
     * @return The location for the ServiceDefinition for the provided
     *         AccessMapping. If this ServiceDefinition is FullyQualified and no
     *         LocationMapping exists for this AccessMapping then null will be
     *         returned.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition passed in is not valid.
     *
     * @exception InvalidAccessPointException
     *            The AccessMapping passed in does not have a valid access
     *            point.
     */
    @Override
    public String locationForAccessMapping(
        final ServiceDefinition serviceDefinition,
        final AccessMapping accessMapping) {
        return locationForAccessMapping(serviceDefinition, accessMapping, true);
    }

    /**
     * Returns the location for the ServiceDefinition for the provided
     * AccessMapping. If this ServiceDefinition is FullyQualified and no
     * LocationMapping exists for this AccessMapping then null will be returned.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to find the location for.
     *
     * @param accessMapping
     *        The AccessMapping to find the location for.
     *
     * @param encodeRelativeComponents
     *        If true, URI-encode any relative URI path components before
     *        appending to the root URI.
     *
     * @return The location for the ServiceDefinition for the provided
     *         AccessMapping. If this ServiceDefinition is FullyQualified and no
     *         LocationMapping exists for this AccessMapping then null will be
     *         returned.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition passed in is not valid.
     *
     * @exception InvalidAccessPointException
     *            The AccessMapping passed in does not have a valid access
     *            point.
     */
    @Override
    public String locationForAccessMapping(
        final ServiceDefinition serviceDefinition,
        final AccessMapping accessMapping,
        final boolean encodeRelativeComponents) {
        Check.notNull(serviceDefinition, "serviceDefinition"); //$NON-NLS-1$
        Check.notNull(accessMapping, "accessMapping"); //$NON-NLS-1$

        if (serviceDefinition.getRelativeToSetting().toInt() == RelativeToSetting.FULLY_QUALIFIED.toInt()) {
            final LocationMapping locationMapping = serviceDefinition.getLocationMapping(accessMapping);
            if (locationMapping != null) {
                return locationMapping.getLocation();
            } else {
                return null;
            }
        } else {
            final String accessPoint = accessMapping.getAccessPoint();
            if (accessPoint == null || accessPoint.length() == 0) {
                throw new InvalidAccessPointException();
            }

            URL url;
            try {
                url = new URL(accessPoint);
            } catch (final MalformedURLException e) {
                throw new AccessPointIsMalformedURLException(accessPoint);
            }

            String properRoot = ""; //$NON-NLS-1$
            final int setting = serviceDefinition.getRelativeToSetting().toInt();
            if (setting == RelativeToSetting.AUTHORITY.toInt()) {
                properRoot = url.getProtocol() + "://" + url.getHost(); //$NON-NLS-1$
            } else if (setting == RelativeToSetting.PORT.toInt()) {
                properRoot = url.getProtocol() + "://" + url.getAuthority(); //$NON-NLS-1$
            } else if (setting == RelativeToSetting.CONTEXT.toInt()) {
                properRoot = URIUtils.combinePaths(
                    url.toString(),
                    getLocationCacheManager().getWebApplicationRelativeDirectory(),
                    encodeRelativeComponents);
            } else if (setting == RelativeToSetting.WEB_APPLICATION.toInt()) {
                properRoot = url.toString();
            } else {
                Check.isTrue(false, "unknown RelativeToString"); //$NON-NLS-1$
            }

            return URIUtils.combinePaths(properRoot, serviceDefinition.getRelativePath(), encodeRelativeComponents);
        }
    }

    /**
     * Returns the location for the ServiceDefintion associated with the
     * ServiceType and ServiceIdentifier that should be used based on the
     * current connection. If a ServiceDefinition with the ServiceType and
     * ServiceIdentifier does not exist then null will be returned. If a
     * ServiceDefinition with the ServiceType and ServiceIdentifier is found
     * then a location will be returned if the ServiceDefinition is well formed
     * (otherwise an exception will be thrown).
     *
     * When determining what location to return for the ServiceDefinition and
     * current connection the following rules will be applied:
     *
     * 1. Try to find a location for the ClientAccessMapping. 2. Try to find a
     * location for the DefaultAccessMapping. 3. Use the first location in the
     * LocationMappings list.
     *
     * @param serviceType
     *        The service type of the ServiceDefinition to find the location
     *        for.
     *
     * @param serviceIdentifier
     *        The service identifier of the ServiceDefinition to find the
     *        location for.
     *
     * @return The location for the ServiceDefinition with the provided service
     *         type and identifier that should be used based on the current
     *         connection.
     *
     * @exception InvalidServiceDefinitionException
     *            The associated ServiceDefinition is not valid and no location
     *            can be found.
     */
    @Override
    public String locationForCurrentConnection(final String serviceType, final GUID serviceIdentifier) {
        final ServiceDefinition serviceDefinition = findServiceDefinition(serviceType, serviceIdentifier);

        if (serviceDefinition == null) {
            // This method should not throw if a ServiceDefinition could not be
            // found.
            return null;
        }

        return locationForCurrentConnection(serviceDefinition);
    }

    /**
     * Returns the location for the ServiceDefintion that should be used based
     * on the current connection. This method will never return null or empty.
     * If it succeeds it will return a target-able location for the provided
     * ServiceDefinition.
     *
     * When determining what location to return for the ServiceDefinition and
     * current connection the following rules will be applied:
     *
     * 1. Try to find a location for the ClientAccessMapping. 2. Try to find a
     * location for the DefaultAccessMapping. 3. Use the first location in the
     * LocationMappings list.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to find the location for.
     *
     * @return The location for the given ServiceDefinition that should be used
     *         based on the current connection.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition passed in is not valid and no location
     *            can be found.
     */
    @Override
    public String locationForCurrentConnection(final ServiceDefinition serviceDefinition) {
        String location = locationForAccessMapping(serviceDefinition, getClientAccessMapping());

        if (location == null) {
            location = locationForAccessMapping(serviceDefinition, getDefaultAccessMapping());
            if (location == null) {
                // Use the location of the first mapping.
                final LocationMapping[] locationMappings = serviceDefinition.getLocationMappings();
                if (locationMappings == null || locationMappings.length == 0) {
                    throw new InvalidServiceDefinitionException(serviceDefinition.getServiceType());
                }

                location = locationMappings[0].getLocation();
            }
        }

        return location;
    }

    /**
     * Removes an AccessMapping and all of the locations that are mapped to it
     * within ServiceDefinitions.
     *
     * @param moniker
     *        The moniker for the AccessMapping to remove.
     *
     * @exception RemoveAccessMappingException
     *            Thrown if the caller tries to remove the default AccessMapping
     *            and this location service cannot inherit its default
     *            AccessMapping from a parent.
     */
    @Override
    public void removeAccessMapping(final String moniker) {
        throw new LocationMethodNotImplementedException("removeAccessMapping"); //$NON-NLS-1$
    }

    /**
     * Removes the ServiceDefinition with the specified service type and service
     * identifier from the location service.
     *
     * @param serviceType
     *        The service type of the ServiceDefinition to remove.
     *
     * @param serviceIdentifier
     *        The service identifier of the ServiceDefinition to remove.
     *
     * @exception IllegalDeleteSelfReferenceServiceDefinitionExceptio
     *            Thrown if the caller tries to delete the self-reference
     *            (location service) ServiceDefinition.
     */
    @Override
    public void removeServiceDefinition(final String serviceType, final GUID serviceIdentifier) {
        throw new LocationMethodNotImplementedException("removeServiceDefinition"); //$NON-NLS-1$
    }

    /**
     * Removes the specified ServiceDefinition from the location service.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to remove. This must be a ServiceDefinition
     *        that is already registered in the location service. Equality is
     *        decided by matching the service type and the identifier.
     *
     * @exception IllegalDeleteSelfReferenceServiceDefinitionException
     *            Thrown if the caller tries to delete the self-reference
     *            (location service) ServiceDefinition.
     */
    @Override
    public void removeServiceDefinition(final ServiceDefinition serviceDefinition) {
        throw new LocationMethodNotImplementedException("removeServiceDefinition"); //$NON-NLS-1$
    }

    /**
     * Removes the specified ServiceDefinitions from the location service.
     *
     * @param serviceDefinitions
     *        The ServiceDefinitions to remove. These must be ServiceDefinitions
     *        that are already registered in the location service. Equality is
     *        decided by matching the service type and the identifier.
     *
     * @exception IllegalDeleteSelfReferenceServiceDefinitionException
     *            Thrown if the caller tries to delete the self-reference
     *            (location service) ServiceDefinition.
     */
    @Override
    public void removeServiceDefinitions(final ServiceDefinition[] serviceDefinitions) {
        throw new LocationMethodNotImplementedException("removeServiceDefinitions"); //$NON-NLS-1$
    }

    /**
     * Saves the provided ServiceDefinition within the location service. This
     * operation will assign the Identifier property on the ServiceDefinition
     * object if one is not already assigned. Any AccessMappings referenced in
     * the LocationMappings property must already be configured with the
     * location service.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to save. This object will be updated with a
     *        new Identifier if one is not already assigned.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition being saved is not valid.
     *
     * @exception AccessMappingNotRegisteredException
     *            The ServiceDefinition references an AccessMapping that has not
     *            been registered.
     *
     * @exception DuplicateLocationMappingException
     *            Thrown if a given AccessMapping has two or more
     *            LocationMappings on a ServiceDefinition.
     */
    @Override
    public void saveServiceDefinition(final ServiceDefinition serviceDefinition) {
        throw new LocationMethodNotImplementedException("saveServiceDefinition"); //$NON-NLS-1$
    }

    /**
     * Saves the provided ServiceDefinitions within the location service. This
     * operation will assign the Identifier property on the ServiceDefinition
     * objects if one is not already assigned. Any AccessMappings referenced in
     * the LocationMappings property must already be configured with the
     * location service.
     *
     * @param serviceDefinitions
     *        The ServiceDefinitions to save. These objects will be updated with
     *        a new Identifier if one is not already assigned.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition being saved is not valid.
     *
     * @exception AccessMappingNotRegisteredException
     *            The ServiceDefinition references an AccessMapping that has not
     *            been registered.
     *
     * @exception DuplicateLocationMappingException
     *            Thrown if a given AccessMapping has two or more
     *            LocationMappings on a ServiceDefinition
     */
    @Override
    public void saveServiceDefinitions(final ServiceDefinition[] serviceDefinitions) {
        throw new LocationMethodNotImplementedException("saveServiceDefinitions"); //$NON-NLS-1$
    }

    /**
     * Sets the default AccessMapping to the AccessMapping passed in.
     *
     * @param accessMapping
     *        The AccessMapping that should become the default AccessMapping.
     *        This AccessMapping must already be configured with this location
     *        service.
     *
     * @exception AccessMappingNotRegisteredException
     *            The AccessMapping being set to the default has not been
     *            registered.
     */
    @Override
    public void setDefaultAccessMapping(final AccessMapping accessMapping) {
        throw new LocationMethodNotImplementedException("setDefaultAccessMapping"); //$NON-NLS-1$
    }

    /**
     * Performs all of the steps that are necessary for setting up a connection
     * with a TeamFoundationServer. This method will use the
     * ICredentialsProvider specified in the constructor to prompt for
     * credentials if authentication fails.
     *
     * @param connectOptions
     *        Specifies what information that should be returned from the
     *        server.
     */
    @Override
    public void connect(ConnectOptions connectOptions) {
        // We want to force ourselves to includes services if our location
        // service cache has no access mappings.
        // This means that this is our first time connecting.
        if (getLocationCacheManager().getAccessMappings().length == 0) {
            connectOptions = connectOptions.combine(ConnectOptions.INCLUDE_SERVICES);
        }

        final boolean includeServices = connectOptions.contains(ConnectOptions.INCLUDE_SERVICES);

        final int lastChangeId = getLocationCacheManager().getLastChangeID();

        // Perform the connection
        final ConnectionData connectionData = webServiceProxy.connect(connectOptions.toIntFlags(), lastChangeId);
        final LocationServiceData locationServiceData = connectionData.getLocationServiceData();

        synchronized (serverDataLock) {
            authenticatedIdentity = connectionData.getAuthenticatedUser();
            authorizedIdentity = connectionData.getAuthorizedUser();
            instanceId = connectionData.getInstanceID();
            catalogResourceId = connectionData.getCatalogResourceID();
            serverCapabilities = new ServerCapabilities(connectionData.getServerCapabilities());
        }

        /*
         * Verify that we are storing the correct guid for this server in the
         * cached data. If we are, this is essentially a no-op.
         */

        /*
         * Create a temporary cache object for the instance and URL we're using,
         * and see if there's data to read.
         */
        final LocationCacheManager tempLocationCache =
            new LocationCacheManager(persistenceProvider.getCachePersistenceStore(), instanceId, connectionBaseURI);

        final LocationCacheManager newLocationCacheManager;

        if (tempLocationCache.isLocalCacheAvailable()) {
            /*
             * Update the field to use the new location since it may contain
             * more information. Specifically, this is useful when the server
             * GUID was unknown when this LocationService was created, so the
             * LocationCacheManager has no cache available (because it wouldn't
             * know where/how to name the cache files). Now the temp cache has a
             * GUID, so replace the field and the query to update the contents
             * is done later in this method.
             */
            newLocationCacheManager = tempLocationCache;
        } else {
            /*
             * Assign a new cache manager for the new instance at this URL.
             */
            newLocationCacheManager =
                new LocationCacheManager(persistenceProvider.getCachePersistenceStore(), instanceId, connectionBaseURI);

            validConnectionData = ConnectOptions.NONE;
        }

        synchronized (locationCacheManagerLock) {
            this.locationCacheManager = newLocationCacheManager;
        }

        // update the location service cache if we tried to retrieve location
        // service data
        newLocationCacheManager.setWebApplicationRelativeDirectory(connectionData.getWebApplicationRelativeDirectory());

        if (locationServiceData != null) {
            newLocationCacheManager.loadServicesData(locationServiceData, includeServices);
        }

        // Set the connection data that we have retrieved
        validConnectionData = validConnectionData.combine(connectOptions);

        connectionMade = true;
    }

    /**
     * Consults the server to see if any services from the filter array have
     * changed. It updates the cache with the new values.
     */
    private void checkForServerUpdates() {
        boolean checkedForUpdates = false;
        if (needToConnect(ConnectOptions.INCLUDE_SERVICES)) {
            synchronized (connectionLock) {
                if (needToConnect(ConnectOptions.INCLUDE_SERVICES)) {
                    // If we haven't made a connection, do more than just query
                    // services
                    connect(ConnectOptions.INCLUDE_SERVICES);
                    checkedForUpdates = true;
                }
            }
        }

        if (!checkedForUpdates) {
            // Check the server to make sure we have up-to-date information
            final LocationServiceData data =
                webServiceProxy.queryServices(ServiceTypeFilter.ALL, getLocationCacheManager().getLastChangeID());
            getLocationCacheManager().loadServicesData(data, true);
        }
    }

    /**
     * Returns true if we need to connect to the server.
     */
    private boolean needToConnect(final ConnectOptions optionsNeeded) {
        // Make sure we refresh the information if the impersonated user has
        // changed.
        final boolean identitySame = true; // TODO:
        // !IdentityDescriptorComparer.Instance.Equals(m_impersonatedDescriptorOnConnect,
        // TeamFoundationImpersonationContext.ImpersonatedDescriptor)

        return connectionMade == false
            || validConnectionData.containsAll(optionsNeeded) == false
            || identitySame == false;
    }

    /**
     * This function ensures that the connection data that is needed by the
     * caller has been retrieved from the server. This function does not use the
     * credentials provider if authentication fails.
     *
     * @param optionsNeeded
     *        The options that designate the information the caller needs from
     *        the server.
     */
    private void ensureConnected(final ConnectOptions optionsNeeded) {
        synchronized (connectionLock) {
            if (needToConnect(optionsNeeded)) {
                connect(optionsNeeded);
            }
        }
    }

    // The methods which follow are for ServerDataProvider

    @Override
    public TeamFoundationIdentity getAuthorizedIdentity() {
        ensureAuthenticated();

        synchronized (serverDataLock) {
            Check.notNull(authorizedIdentity, "this.authorizedIdentity"); //$NON-NLS-1$
            return authorizedIdentity;
        }
    }

    @Override
    public TeamFoundationIdentity getAuthenticatedIdentity() {
        ensureConnected(ConnectOptions.NONE);

        synchronized (serverDataLock) {
            Check.notNull(authenticatedIdentity, "authenticatedIdentity"); //$NON-NLS-1$
            return authenticatedIdentity;
        }
    }

    @Override
    public boolean hasAuthenticated() {
        synchronized (serverDataLock) {
            return authenticatedIdentity != null;
        }
    }

    @Override
    public void ensureAuthenticated() {
        ensureConnected(ConnectOptions.NONE);
    }

    @Override
    public void authenticate() {
        connect(ConnectOptions.NONE);
    }

    @Override
    public GUID getCatalogResourceID() {
        ensureConnected(ConnectOptions.NONE);

        synchronized (serverDataLock) {
            return catalogResourceId;
        }
    }

    @Override
    public GUID getInstanceID() {
        ensureConnected(ConnectOptions.NONE);

        synchronized (serverDataLock) {
            return instanceId;
        }
    }

    @Override
    public ServerCapabilities getServerCapabilities() {
        ensureConnected(ConnectOptions.NONE);

        synchronized (serverDataLock) {
            return serverCapabilities;
        }
    }

    /**
     * The function finds the location of the server that has the guid passed.
     * Note that the server in question must be a "child" server of the server
     * this object is providing data for.
     *
     * {@inheritDoc}
     *
     * @return the location for the server with the provided guid or null if
     *         this server does not have a child with the provided guid
     */
    @Override
    public String findServerLocation(final GUID serverGuid) {
        return locationForCurrentConnection(ServiceInterfaceNames.LOCATION, serverGuid);
    }

    @Override
    public void reactToPossibleServerUpdate(final int serverLastChangeId) {
        getLocationCacheManager().clearIfCacheNotFresh(serverLastChangeId);
    }
}
