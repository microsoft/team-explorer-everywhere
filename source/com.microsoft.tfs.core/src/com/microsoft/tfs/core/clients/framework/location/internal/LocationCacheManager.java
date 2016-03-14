// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.framework.location.AccessMapping;
import com.microsoft.tfs.core.clients.framework.location.LocationMapping;
import com.microsoft.tfs.core.clients.framework.location.LocationServiceData;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.clients.registration.ServerMap;
import com.microsoft.tfs.core.persistence.LockMode;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;

/**
 * The in-memory cache of data returned by the TFS location service.
 *
 * @threadsafety thread-safe
 */
public class LocationCacheManager {
    private int lastChangeID;
    private final URI connectionBaseUri;
    private String webApplicationRelativeDirectory;
    private Map<String, AccessMapping> mapAccessMappings;
    private Map<String, Map<GUID, ServiceDefinition>> mapServices;
    private AccessMapping clientAccessMapping;
    private AccessMapping defaultAccessMapping;
    private final ReentrantReadWriteLock accessLock;
    private String defaultAccessMappingMoniker;

    private PersistenceStore locationDiskStore;
    private boolean cacheAvailable;
    private boolean cacheLocallyFresh;

    private static final Log log = LogFactory.getLog(LocationCacheManager.class);
    private static final String CACHE_DIRECTORY_NAME = "TEE-Location"; //$NON-NLS-1$
    private static final String CACHE_FILE_NAME = "LocationServiceData.xml"; //$NON-NLS-1$

    public LocationCacheManager(final PersistenceStore cacheStore, GUID serverGuid, final URI serverUrl) {
        final ServerMap serverMap = ServerMap.load(cacheStore);

        /*
         * If the given server GUID was empty, try to look it up in the
         * registration server map using the given URI.
         */
        if (serverGuid.equals(GUID.EMPTY)) {
            final String instanceID = serverMap.getServerID(serverUrl.toString());

            if (instanceID != null) {
                serverGuid = new GUID(instanceID);
            }
        }

        cacheAvailable = serverGuid.equals(GUID.EMPTY) ? false : true;
        cacheLocallyFresh = false;

        accessLock = new ReentrantReadWriteLock();
        lastChangeID = -1;

        mapAccessMappings = new HashMap<String, AccessMapping>();
        mapServices = new HashMap<String, Map<GUID, ServiceDefinition>>();
        connectionBaseUri = serverUrl;

        // Get the store for the location cache.
        if (cacheAvailable) {
            if (serverGuid != null) {
                final String directoryName =
                    serverGuid.getGUIDString().toLowerCase() + "_" + connectionBaseUri.getScheme(); //$NON-NLS-1$
                locationDiskStore = cacheStore.getChildStore(CACHE_DIRECTORY_NAME).getChildStore(directoryName);
            }
        }
    }

    public boolean isLocalCacheAvailable() {
        ensureDiskCacheLoaded();
        return cacheAvailable;
    }

    public AccessMapping getClientAccessMapping() {
        try {
            accessLock.readLock().lock();
            return clientAccessMapping;
        } finally {
            accessLock.readLock().unlock();
        }
    }

    public AccessMapping getDefaultAccessMapping() {
        try {
            accessLock.readLock().lock();
            return defaultAccessMapping;
        } finally {
            accessLock.readLock().unlock();
        }
    }

    public String getWebApplicationRelativeDirectory() {
        return webApplicationRelativeDirectory;
    }

    /**
     * Sets the WebApplicationRelativeDirectory to the specified value.
     */
    public void setWebApplicationRelativeDirectory(final String value) {
        webApplicationRelativeDirectory = value;
    }

    /**
     * Returns the id of the last server change that this cache is aware of.
     */
    public int getLastChangeID() {
        ensureDiskCacheLoaded();

        try {
            accessLock.readLock().lock();
            return lastChangeID;
        } finally {
            accessLock.readLock().unlock();
        }
    }

    /**
     * Clear the cache if it is out of date relative to the specified change id.
     * The cache is unaltered if the cache is considered to be up to date.
     *
     * @param serverLastChangeId
     *        The last known server change id.
     */
    public void clearIfCacheNotFresh(final int serverLastChangeId) {
        if (serverLastChangeId == lastChangeID) {
            return;
        }

        try {
            accessLock.writeLock().lock();
            if (serverLastChangeId == lastChangeID) {
                return;
            }

            mapAccessMappings.clear();
            mapServices.clear();
            lastChangeID = -1;
        } finally {
            if (accessLock.isWriteLockedByCurrentThread()) {
                accessLock.writeLock().unlock();
            }
        }
    }

    /**
     * Returns the AccessMappings that this location service cache knows about.
     * Note that each time this property is accessed, the list is copied and
     * returned.
     *
     * @return Array of AccessMappings.
     */
    public AccessMapping[] getAccessMappings() {
        try {
            ensureDiskCacheLoaded();
            accessLock.readLock().lock();

            return accessMappingMapToArray(mapAccessMappings);
        } finally {
            accessLock.readLock().unlock();
        }
    }

    /**
     * Removes the access mapping with the provided access mapping moniker and
     * all of the location mapping entries that have this access zone.
     *
     * @param moniker
     *        The moniker of the access mapping to remove.
     */
    public void removeAccessMapping(final String moniker) {
        try {
            ensureDiskCacheLoaded();
            accessLock.writeLock().lock();

            if (mapAccessMappings.containsKey(moniker)) {
                // Remove it from the access mappings
                mapAccessMappings.remove(moniker);

                // Remove each instance from the service definitions
                for (final Map<GUID, ServiceDefinition> m : mapServices.values()) {
                    for (final ServiceDefinition definition : m.values()) {
                        // We know that it is illegal to delete an access
                        // mapping that is the default access mapping of
                        // a service definition so we don't have to update any
                        // of those values.
                        final LocationMapping[] locationMappings = definition.getLocationMappings();

                        // Remove the mapping that has the removed access
                        // mapping.
                        for (int i = 0; i < locationMappings.length; i++) {
                            final String accessMappingMoniker = locationMappings[i].getAccessMappingMoniker();
                            if (moniker.equalsIgnoreCase(accessMappingMoniker)) {
                                definition.internalRemoveLocationMappingAt(i);
                            }
                        }
                    }
                }
            }

            writeCacheToDisk();
        } finally {
            if (accessLock.isWriteLockedByCurrentThread()) {
                accessLock.writeLock().unlock();
            }
        }
    }

    /**
     * Removes services from both the in-memory cache and the disk cache.
     *
     * @param serviceDefinitions
     *        The service definitions to remove.
     *
     * @param lastChangeId
     *        The lastChangeId the server returned when it performed this
     *        operation.
     */
    public void removeServices(final ServiceDefinition[] serviceDefinitions, final int lastChangeId) {
        try {
            ensureDiskCacheLoaded();
            accessLock.writeLock().lock();

            // Iterate the service definitions that should be removed.
            for (int i = 0; i < serviceDefinitions.length; i++) {
                // Move to the next definition if there are no service
                // definitions of this type.
                final ServiceDefinition serviceDefinition = serviceDefinitions[i];
                if (!mapServices.containsKey(serviceDefinition.getServiceType())) {
                    continue;
                }

                // Remove the service definition from the map of all services of
                // this type.
                final Map<GUID, ServiceDefinition> serviceDefinitionInstances =
                    mapServices.get(serviceDefinition.getServiceType());

                if (serviceDefinitionInstances.containsKey(serviceDefinition.getIdentifier())) {
                    serviceDefinitionInstances.remove(serviceDefinition.getIdentifier());
                }

                // If the entry is removed and there are no more definitions of
                // this type, remove that entry from the services structure
                if (serviceDefinitionInstances.size() == 0) {
                    mapServices.remove(serviceDefinition.getServiceType());
                }
            }

            // Update the change id on the cache and persist it to disk.
            setLastChangeID(lastChangeId, false);
            writeCacheToDisk();
        } finally {
            if (accessLock.isWriteLockedByCurrentThread()) {
                accessLock.writeLock().unlock();
            }
        }
    }

    /**
     * Returns the access mapping for the provided moniker.
     *
     * @param moniker
     *        The moniker of the access mapping to return.
     *
     * @return The access mapping for the provided moniker or null /// if an
     *         access mapping for the moniker doesn't exist
     */
    public AccessMapping getAccessMapping(final String moniker) {
        Check.notNullOrEmpty(moniker, "moniker"); //$NON-NLS-1$

        try {
            ensureDiskCacheLoaded();
            accessLock.readLock().lock();

            if (mapAccessMappings.containsKey(moniker)) {
                return mapAccessMappings.get(moniker);
            } else {
                return null;
            }
        } finally {
            accessLock.readLock().unlock();
        }
    }

    /**
     * Returns the service definition for the service with the provided service
     * type and identifier. Null will be returned if there is no entry in the
     * cache for this service.
     *
     * @param serviceType
     *        The service type we are looking for.
     *
     * @param serviceIdentifier
     *        The identifier for the specific service instance we are looking
     *        for.
     *
     * @return The service definition for the service with the provided service
     *         type and identifier. Null will be returned if there is no entry
     *         in the cache for this service.
     */
    public ServiceDefinition findService(final String serviceType, final GUID serviceIdentifier) {
        try {
            ensureDiskCacheLoaded();
            accessLock.readLock().lock();

            ServiceDefinition serviceDefinition = null;
            if (mapServices.containsKey(serviceType)) {
                final Map<GUID, ServiceDefinition> services = mapServices.get(serviceType);
                if (services.containsKey(serviceIdentifier)) {
                    serviceDefinition = services.get(serviceIdentifier);
                }
            }

            return (ServiceDefinition) (serviceDefinition == null ? null : serviceDefinition.clone());
        } finally {
            accessLock.readLock().unlock();
        }
    }

    /**
     * Finds all services with the provided service type.
     *
     * @param serviceType
     *        The service type we are looking for.
     *
     * @return All of the service definitions with the serviceType that are in
     *         the cache or null if none are in the cache
     */
    public ServiceDefinition[] findServices(final String serviceType) {
        try {
            ensureDiskCacheLoaded();
            accessLock.readLock().lock();

            Check.isTrue(lastChangeID == -1 || mapServices.size() > 0, "lastChangeID == -1 || mapServices.size() > 0"); //$NON-NLS-1$

            // Bail immediately if there are no known services.
            if (mapServices.size() == 0) {
                return null;
            }

            // Build an array of ServiceDefinitions of the requested service
            // type or all services if a service type was not specified.
            final List<ServiceDefinition> list = new ArrayList<ServiceDefinition>();

            if (serviceType == null || serviceType.length() == 0) {
                // Return all services.
                for (final Map<GUID, ServiceDefinition> mapServiceInstances : mapServices.values()) {
                    addClonedServiceInstancesToList(mapServiceInstances, list);
                }
            } else {
                // The caller has requested services of a specific type.
                if (!mapServices.containsKey(serviceType)) {
                    return null;
                }

                final Map<GUID, ServiceDefinition> mapServiceInstances = mapServices.get(serviceType);
                addClonedServiceInstancesToList(mapServiceInstances, list);
            }

            return list.toArray(new ServiceDefinition[list.size()]);
        } finally {
            accessLock.readLock().unlock();
        }
    }

    /**
     * Finds the service definitions for all of the available services for the
     * specified tool id. If no services exist for this tool id, null will be
     * returned.
     *
     * @param toolId
     *        The case-insensitive tool id whose services we are looking for. If
     *        this is null or empty, all services will be returned.
     *
     * @return Service definitions for all of the existing services that are of
     *         the supplied tool id. If no services of this type exist, null
     *         will be returned
     */
    public ServiceDefinition[] findServicesByToolID(final String toolId) {
        try {
            ensureDiskCacheLoaded();
            accessLock.readLock().lock();

            final boolean returnAll = toolId == null || toolId.length() == 0;
            final List<ServiceDefinition> requestedDefinitions = new ArrayList<ServiceDefinition>();

            // Iterate the map of service types. Values are a map of service
            // instances.
            for (final Map<GUID, ServiceDefinition> mapServiceInstances : mapServices.values()) {
                for (final ServiceDefinition definition : mapServiceInstances.values()) {
                    /*
                     * NB! some service definitions in Dev12 QU1 may have null
                     * ToolID
                     */
                    if (returnAll || toolId.equalsIgnoreCase(definition.getToolID())) {
                        requestedDefinitions.add((ServiceDefinition) definition.clone());
                    }
                }
            }

            // Return null if no matching definitions were found.
            if (requestedDefinitions.size() == 0) {
                return null;
            }

            // Return an array of the matching service definitions.
            return requestedDefinitions.toArray(new ServiceDefinition[requestedDefinitions.size()]);
        } finally {
            accessLock.readLock().unlock();
        }
    }

    /**
     * Loads the service data into the in-memory cache and writes the values to
     * disk.
     *
     * @param locationServiceData
     *        The data to write to the cache.
     *
     * @param allServicesIncluded
     *        True if the data contains information for all services.
     */
    public void loadServicesData(final LocationServiceData locationServiceData, final boolean allServicesIncluded) {
        try {
            accessLock.writeLock().lock();

            // If the server is telling us our client cache isn't fresh and we
            // agree with it, clear the storage. The reason we check to see if
            // we agree with it is that because of the way we cache based on
            // filters, we may sometimes tell the server that our last change id
            // is -1 because we don't have a given filter cached. In this case,
            // the server will tell us our cache is out of date even though it
            // isn't.
            if (!locationServiceData.isClientCacheFresh() && locationServiceData.getLastChangeID() != lastChangeID) {
                mapAccessMappings = new HashMap<String, AccessMapping>();
                mapServices = new HashMap<String, Map<GUID, ServiceDefinition>>();
                lastChangeID = -1;
            } else {
                ensureDiskCacheLoaded();
            }

            // We have to update the lastChangeId outside of the above if check
            // because there are cases such as a register service where we cause
            // the lastChangeId to be incremented and our cache isn't out of
            // date.
            setLastChangeID(locationServiceData.getLastChangeID(), allServicesIncluded);

            defaultAccessMappingMoniker = locationServiceData.getDefaultAccessMappingMoniker();
            final AccessMapping[] accessMappings = locationServiceData.getAccessMappings();
            if (accessMappings != null && accessMappings.length > 0) {
                // Get all of the access mappings
                for (int i = 0; i < accessMappings.length; i++) {
                    final AccessMapping accessMapping = accessMappings[i];
                    if (!locationServiceData.isAccessPointsDoNotIncludeWebAppRelativeDirectory()) {
                        final String relativeDirectory = getWebApplicationRelativeDirectory();
                        if (relativeDirectory != null && relativeDirectory.length() > 0) {
                            // TODO: Which encoding?
                            final String unescapedAbsoluteUriTrimmed =
                                URLDecoder.decode(StringUtil.trimEnd(accessMapping.getAccessPoint(), '/'));

                            // TODO: Which encoding?
                            final String unescapedRelativeDirectoryTrimmed =
                                URLDecoder.decode(StringUtil.trimEnd(relativeDirectory, '/'));

                            if (unescapedAbsoluteUriTrimmed.toLowerCase().endsWith(
                                unescapedRelativeDirectoryTrimmed.toLowerCase())) {
                                final String accessPoint = unescapedAbsoluteUriTrimmed.substring(
                                    0,
                                    unescapedAbsoluteUriTrimmed.length() - unescapedRelativeDirectoryTrimmed.length());
                                accessMapping.setAccessPoint(accessPoint);
                            }
                        }
                    }

                    // if we can find it, update the values so the objects that
                    // reference this access mapping are updated as well
                    if (mapAccessMappings.containsKey(accessMapping.getMoniker())) {
                        final AccessMapping existingAccessMapping = mapAccessMappings.get(accessMapping.getMoniker());
                        existingAccessMapping.setDisplayName(accessMapping.getDisplayName());
                        existingAccessMapping.setAccessPoint(accessMapping.getAccessPoint());
                    } else {
                        mapAccessMappings.put(accessMapping.getMoniker(), accessMapping);
                    }
                }

                determineClientAndDefaultZones(locationServiceData.getDefaultAccessMappingMoniker());
            }

            connectionBaseUri.getScheme();
            connectionBaseUri.getAuthority();
            connectionBaseUri.getScheme();
            connectionBaseUri.getHost();

            final ServiceDefinition[] serviceDefinitions = locationServiceData.getServiceDefinitions();
            if (serviceDefinitions != null) {
                // Get all of the services
                for (int i = 0; i < serviceDefinitions.length; i++) {
                    final ServiceDefinition definition = serviceDefinitions[i];

                    //
                    // TODO:
                    // definition.reactToWebServiceDeserialization(mapAccessMappings);
                    //

                    if (!mapServices.containsKey(definition.getServiceType())) {
                        mapServices.put(definition.getServiceType(), new HashMap<GUID, ServiceDefinition>());
                    }

                    final Map<GUID, ServiceDefinition> services = mapServices.get(definition.getServiceType());
                    services.put(definition.getIdentifier(), definition);
                }
            }

            writeCacheToDisk();
        } finally {
            if (accessLock.isWriteLockedByCurrentThread()) {
                accessLock.writeLock().unlock();
            }
        }
    }

    private void addClonedServiceInstancesToList(
        final Map<GUID, ServiceDefinition> mapServiceInstances,
        final List<ServiceDefinition> listOfClones) {
        for (final ServiceDefinition serviceDefinition : mapServiceInstances.values()) {
            listOfClones.add((ServiceDefinition) serviceDefinition.clone());
        }
    }

    private void determineClientAndDefaultZones(final String defaultAccessMappingMoniker) {
        defaultAccessMapping = null;
        clientAccessMapping = null;

        for (final AccessMapping accessMapping : mapAccessMappings.values()) {
            final String trimmedAccessPoint = StringUtil.trimEnd(accessMapping.getAccessPoint(), '/');
            final String absoluteUri = connectionBaseUri.toString();

            if (absoluteUri.toLowerCase().startsWith(trimmedAccessPoint.toLowerCase())) {
                clientAccessMapping = accessMapping;
            }
        }

        defaultAccessMapping = mapAccessMappings.get(defaultAccessMappingMoniker);

        if (clientAccessMapping == null) {
            String accessPoint = StringUtil.trimEnd(connectionBaseUri.toString(), '/');
            getWebApplicationRelativeDirectory();

            if (webApplicationRelativeDirectory != null && webApplicationRelativeDirectory.length() > 0) {
                final String trimmedRealtiveDirectory =
                    StringUtil.trimEnd(getWebApplicationRelativeDirectory(), '/');
                final String unescapedAbsoluteUriTrimmed = URLDecoder.decode(accessPoint); // TODO:
                // Which
                // encoding?
                final String unescapedRelativeDirectoryTrimmed = URLDecoder.decode(trimmedRealtiveDirectory); // TODO:
                // Which
                // encoding?

                if (unescapedAbsoluteUriTrimmed.toLowerCase().endsWith(
                    unescapedRelativeDirectoryTrimmed.toLowerCase())) {
                    accessPoint = unescapedAbsoluteUriTrimmed.substring(
                        0,
                        unescapedAbsoluteUriTrimmed.length() - unescapedRelativeDirectoryTrimmed.length());
                }
            }

            // Looks like we are in an unregistered zone, make up our own.
            clientAccessMapping = new AccessMapping(accessPoint, accessPoint, accessPoint);
        }
    }

    /**
     * Update the last server change id if all services in the cache have been
     * updated.
     *
     * @param value
     *        The new server change id to set.
     *
     * @param allServicesUpdated
     *        True if all services have been updated in the cache.
     */
    private void setLastChangeID(final int value, final boolean allServicesUpdated) {
        Check.isTrue(accessLock.isWriteLockedByCurrentThread(), "accessLock.isWriteLockedByCurrentThread()"); //$NON-NLS-1$

        if (lastChangeID != -1 || allServicesUpdated) {
            // We only update our last change id if the last change id was valid
            // before and this is an incremental update or this data includes
            // all services.
            lastChangeID = value;
        }
    }

    /**
     * @return <code>true</code> if data for this connection can be loaded from
     *         disk successfully, <code>false</code> otherwise
     */
    public boolean hasLocalCacheDataForConnection() {
        if (cacheAvailable == false) {
            return false;
        }

        try {
            accessLock.writeLock().lock();

            final LocationServiceCacheData data = loadLocationServiceCacheData();

            return data != null;
        } catch (final Exception ex) {
            /*
             * Don't change any state in this object for this failure. Just log
             * it.
             */
            log.info(MessageFormat.format(
                "unable to load location data (while testing) from {0}:{1}", //$NON-NLS-1$
                locationDiskStore.toString(),
                CACHE_FILE_NAME), ex);
        } finally {
            if (accessLock.isWriteLockedByCurrentThread()) {
                accessLock.writeLock().unlock();
            }
        }

        return false;
    }

    private void ensureDiskCacheLoaded() {
        if (cacheLocallyFresh || !cacheAvailable) {
            return;
        }

        try {
            accessLock.writeLock().lock();

            final LocationServiceCacheData data = loadLocationServiceCacheData();

            // Load maps with data.
            if (data != null) {
                lastChangeID = data.getLastChangeID();
                webApplicationRelativeDirectory = data.getVirtualDirectory();
                mapAccessMappings = accessMappingArrayToMap(data.getAccessMappings());
                determineClientAndDefaultZones(data.getDefaultMappingMoniker());
                mapServices = serviceDefinitionArrayToMap(data.getServiceDefinitions());
            }
        } catch (final Exception ex) {
            // We don't have access to the cache file. Eat this exception and
            // mark the cache as unavailable so we don't repeatedly try to
            // access it.
            log.warn(MessageFormat.format(
                "unable to load location data from {0}:{1}", //$NON-NLS-1$
                locationDiskStore.toString(),
                CACHE_FILE_NAME), ex);

            cacheAvailable = false;
            lastChangeID = -1;
        } finally {
            cacheLocallyFresh = true;
            if (accessLock.isWriteLockedByCurrentThread()) {
                accessLock.writeLock().unlock();
            }
        }
    }

    private LocationServiceCacheData loadLocationServiceCacheData() throws IOException, InterruptedException {
        final LocationDataSerializer serializer = new LocationDataSerializer();

        return (LocationServiceCacheData) locationDiskStore.retrieveItem(
            CACHE_FILE_NAME,
            LockMode.WAIT_FOREVER,
            null,
            serializer);
    }

    private void writeCacheToDisk() {
        if (!cacheAvailable) {
            return;
        }

        final AccessMapping[] accessMappings = accessMappingMapToArray(mapAccessMappings);
        final ServiceDefinition[] serviceDefinitions = serviceDefinitionMapToArray(mapServices);

        // There are cases where the web service returns a value for
        // defaultAccessMappingMoniker even though there are no access mappings.
        // If we were not able to match a default access mapping with the
        // supplied moniker, then we'll persist the value passed in by the web
        // service.
        String defaultMoniker;
        if (defaultAccessMapping != null) {
            defaultMoniker = defaultAccessMapping.getMoniker();
        } else {
            defaultMoniker = defaultAccessMappingMoniker;
        }

        final LocationServiceCacheData data = new LocationServiceCacheData(
            lastChangeID,
            defaultMoniker,
            webApplicationRelativeDirectory,
            accessMappings,
            serviceDefinitions);

        try {
            final LocationDataSerializer serializer = new LocationDataSerializer();
            locationDiskStore.storeItem(CACHE_FILE_NAME, data, LockMode.WAIT_FOREVER, null, serializer);
        } catch (final Exception e) {
            log.warn(MessageFormat.format(
                "unable to save registration data to {0}:{1}", //$NON-NLS-1$
                locationDiskStore.toString(),
                CACHE_FILE_NAME), e);
        }
    }

    /**
     * Build a HashMap from the array of ServiceDefinitions. The keys for the
     * map are the ServiceDefinitions service type. The values are another
     * HashMap which contains all service instances of that type. The secondary
     * HashMap key is the service identifier. The secondary HashMap value is the
     * ServiceDefinition.
     *
     * @param serviceDefinitions
     *        The array of ServiceDefinitions to be placed in a HashMap.
     *
     * @return A two-level HashMap containing the ServiceDefinitions.
     */
    private static Map<String, Map<GUID, ServiceDefinition>> serviceDefinitionArrayToMap(
        final ServiceDefinition[] serviceDefinitions) {
        final Map<String, Map<GUID, ServiceDefinition>> map = new HashMap<String, Map<GUID, ServiceDefinition>>();
        for (final ServiceDefinition definition : serviceDefinitions) {
            if (!map.containsKey(definition.getServiceType())) {
                map.put(definition.getServiceType(), new HashMap<GUID, ServiceDefinition>());
            }

            final Map<GUID, ServiceDefinition> serviceDefinitionInstances = map.get(definition.getServiceType());
            serviceDefinitionInstances.put(definition.getIdentifier(), definition);
        }
        return map;
    }

    /**
     * Convert the specified ServiceDefinition HashMap to an array of
     * ServiceDefinitions.
     *
     * @param servicesMap
     *        The HashMap containing the ServiceDefions.
     *
     * @return An array containing each ServiceDefinition value from the map.
     */
    private static ServiceDefinition[] serviceDefinitionMapToArray(
        final Map<String, Map<GUID, ServiceDefinition>> servicesMap) {
        final List<ServiceDefinition> list = new ArrayList<ServiceDefinition>();

        for (final Map<GUID, ServiceDefinition> serviceDefinitionInstances : servicesMap.values()) {
            for (final ServiceDefinition serviceDefinition : serviceDefinitionInstances.values()) {
                list.add(serviceDefinition);
            }
        }

        return list.toArray(new ServiceDefinition[list.size()]);
    }

    /**
     * Build a HashMap from the array of AccessMappigns. The keys for the map
     * are the AccessMapping moniker. The values are the AccessMapping objects.
     *
     * @param accessMappings
     *        The array of AccessMappings to be placed in a HashMap.
     *
     * @return A HashMap containing the AccessMappings.
     */
    private static Map<String, AccessMapping> accessMappingArrayToMap(final AccessMapping[] accessMappings) {
        final Map<String, AccessMapping> map = new HashMap<String, AccessMapping>();
        for (final AccessMapping mapping : accessMappings) {
            map.put(mapping.getMoniker(), mapping);
        }
        return map;
    }

    /**
     * Convert the specified AccessMapping HashMap to an array of
     * AccessMappings.
     *
     * @param accessMappingsMap
     *        The HashMap containing the AccessMappings.
     *
     * @return An array containing each AccessMapping value from the map.
     */
    private static AccessMapping[] accessMappingMapToArray(final Map<String, AccessMapping> accessMappingsMap) {
        final Collection<AccessMapping> values = accessMappingsMap.values();
        return values.toArray(new AccessMapping[values.size()]);
    }
}
