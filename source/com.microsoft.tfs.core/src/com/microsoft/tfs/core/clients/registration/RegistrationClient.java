// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import java.net.URI;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.tfs.services.registration._03._RegistrationSoap;

/**
 * <p>
 * A client for accessing the registration data available from a Team Foundation
 * server.
 * </p>
 *
 * <p>
 * This client provides several services on top of the Registration web service:
 * <ul>
 * <li>Convenience methods to quickly access specific parts of the registration
 * data</li>
 * <li>In-memory (per session) cache of the registration data</li>
 * <li>On-disk (across sessions) cache of the registration data</li>
 * <li>A strategy for transparently refreshing the cached data at appropriate
 * intervals</li>
 * </ul>
 * </p>
 *
 * <p>
 * A {@link RegistrationClient} should normally be obtained from a
 * {@link TFSTeamProjectCollection} instead of being manually constructed.
 * </p>
 *
 * <p>
 * Note: RegistrationClient is unlike other clients. TFSConnection calls into
 * this client, and this calls back. Thus you need to be very careful of locking
 * between TFSConnection and this.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class RegistrationClient {
    /**
     * The default registration data refresh interval time in milliseconds
     * (equal to 7200 seconds or 2 hours).
     */
    private static final long DEFAULT_REFRESH_INTERVAL_MILLIS = 7200000;

    /**
     * The {@link TFSTeamProjectCollection} that this {@link RegistrationClient}
     * uses (never <code>null</code>).
     */
    private final TFSTeamProjectCollection connection;

    /**
     * The amount of time (in milliseconds) to wait before refreshing the
     * registration data with fresh data from the server.
     */
    private final long refreshIntervalMillis;

    /**
     * <code>true</code> if we are caching and reading registration data from
     * disk, or <code>false</code> to disable disk caching for this
     * {@link RegistrationClient}.
     */
    private final boolean enableDiskCache;

    /**
     * The current {@link RegistrationData}. <code>null</code> until the first
     * time the data is needed.
     */
    private volatile RegistrationData registrationData;

    /**
     * The web service proxy created during construction.
     */
    private final _RegistrationSoap webService;

    /**
     * Creates a new {@link RegistrationClient} with a default refresh interval
     * and disk caching enabled.
     *
     * @param connection
     *        the {@link TFSTeamProjectCollection} to use (must not be
     *        <code>null</code>)
     */
    public RegistrationClient(final TFSTeamProjectCollection connection) {
        this(connection, DEFAULT_REFRESH_INTERVAL_MILLIS, true);
    }

    /**
     * Creates a new {@link RegistrationClient}, specifying the refresh interval
     * and disk caching policy.
     *
     * @param connection
     *        the {@link TFSTeamProjectCollection} to use (must not be
     *        <code>null</code>)
     * @param refreshIntervalMillis
     *        specifies the amount of time between refreshes of the registration
     *        data from the server
     * @param enableDiskCache
     *        <code>true</code> to enable caching of registration data to disk
     */
    public RegistrationClient(
        final TFSTeamProjectCollection connection,
        final long refreshIntervalMillis,
        final boolean enableDiskCache) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
        this.refreshIntervalMillis = refreshIntervalMillis;
        this.enableDiskCache = enableDiskCache;
        webService = (_RegistrationSoap) connection.getWebService(_RegistrationSoap.class);
    }

    /**
     * Gets all of the {@link RegistrationEntry}s. The returned array is safe -
     * no references to the returned {@link RegistrationEntry}s are held by this
     * class.
     *
     * @return all of the {@link RegistrationEntry}s (never <code>null</code>)
     */
    public RegistrationEntry[] getRegistrationEntries() {
        return getRegistrationEntries(false);
    }

    /**
     * Gets all of the {@link RegistrationEntry}s, specifying whether to force a
     * refresh of the registration data from the TF server. The returned array
     * is safe - no references to the returned {@link RegistrationEntry}s are
     * held by this class.
     *
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return all of the {@link RegistrationEntry}s (never <code>null</code>)
     */
    public RegistrationEntry[] getRegistrationEntries(final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getRegistrationEntries(true);
    }

    /**
     * Gets the {@link RegistrationEntry} for the specified tool. The returned
     * RegistrationEntry is safe - no reference to it is held by this class.
     *
     * @param toolId
     *        specifies the tool to get the {@link RegistrationEntry} for (case
     *        insensitive) (must not be <code>null</code>)
     * @return the {@link RegistrationEntry} for the specified tool, or
     *         <code>null</code> if the specified tool does not have a
     *         registration entry
     */
    public RegistrationEntry getRegistrationEntry(final String toolId) {
        return getRegistrationEntry(toolId, false);
    }

    /**
     * Gets the {@link RegistrationEntry} for the specified tool, specifying
     * whether to force a refresh of the registration data from the TF server.
     * The returned RegistrationEntry is safe - no reference to it is held by
     * this class.
     *
     * @param toolId
     *        specifies the tool to get the {@link RegistrationEntry} for (case
     *        insensitive) (must not be <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return the {@link RegistrationEntry} for the specified tool, or
     *         <code>null</code> if the specified tool does not have a
     *         registration entry
     */
    public RegistrationEntry getRegistrationEntry(final String toolId, final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getRegistrationEntry(toolId, true);
    }

    /**
     * Gets all of the {@link ServiceInterface}s for the specified tool. The
     * returned array is safe - no references to the {@link ServiceInterface}s
     * are held by this class.
     *
     * @param toolId
     *        specifies the tool to get {@link ServiceInterface}s for (case
     *        insensitive) (must not be <code>null</code>)
     * @return all of the {@link ServiceInterface}s for the specified tool, or
     *         <code>null</code> if the specified tool does not have a
     *         registration entry
     */
    public ServiceInterface[] getServiceInterfaces(final String toolId) {
        return getServiceInterfaces(toolId, false);
    }

    /**
     * Gets all of the {@link ServiceInterface}s for the specified tool,
     * specifying whether to force a refresh of the registration data from the
     * TF server. The returned array is safe - no references to the
     * {@link ServiceInterface}s are held by this class.
     *
     * @param toolId
     *        specifies the tool to get {@link ServiceInterface}s for (case
     *        insensitive) (must not be <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return all of the {@link ServiceInterface}s for the specified tool, or
     *         <code>null</code> if the specified tool does not have a
     *         registration entry
     */
    public ServiceInterface[] getServiceInterfaces(final String toolId, final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getServiceInterfaces(toolId, true);
    }

    /**
     * Gets the specified {@link ServiceInterface} for the specified tool. The
     * returned {@link ServiceInterface} is safe - no reference to it is held by
     * this class.
     *
     * @param toolId
     *        specifies the tool to get the {@link ServiceInterface} for (case
     *        insensitive) (must not be <code>null</code>)
     * @param serviceInterfaceName
     *        specifies the service interface to get (case insensitive) (must
     *        not be <code>null</code>)
     * @return the specified {@link ServiceInterface} for the specified tool, or
     *         <code>null</code> if the requested service interface does not
     *         exist
     */
    public ServiceInterface getServiceInterface(final String toolId, final String serviceInterfaceName) {
        return getServiceInterface(toolId, serviceInterfaceName, false);
    }

    /**
     * Gets the specified {@link ServiceInterface} for the specified tool,
     * specifying whether to force a refresh of the registration data from the
     * TF server. The returned {@link ServiceInterface} is safe - no reference
     * to it is held by this class.
     *
     * @param toolId
     *        specifies the tool to get the {@link ServiceInterface} for (case
     *        insensitive) (must not be <code>null</code>)
     * @param serviceInterfaceName
     *        specifies the service interface to get (case insensitive) (must
     *        not be <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return the specified {@link ServiceInterface} for the specified tool, or
     *         <code>null</code> if the requested service interface does not
     *         exist
     */
    public ServiceInterface getServiceInterface(
        final String toolId,
        final String serviceInterfaceName,
        final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getServiceInterface(toolId, serviceInterfaceName, true);
    }

    /**
     * Gets the specified service interface URL for the specified tool.
     *
     * @param toolId
     *        specifies the tool to get the {@link ServiceInterface} for (case
     *        insensitive) (must not be <code>null</code>)
     * @param serviceInterfaceName
     *        specifies the service interface to get (case insensitive) (must
     *        not be <code>null</code>)
     * @return the service interface url, or <code>null</code> if no such
     *         service interface exists
     */
    public String getServiceInterfaceURL(final String toolId, final String serviceInterfaceName) {
        return getServiceInterfaceURL(toolId, serviceInterfaceName, false, false);
    }

    /**
     * Gets the specified service interface URL for the specified tool,
     * specifying whether to force a refresh of the registration data from the
     * TF server.
     *
     * @param toolId
     *        specifies the tool to get the {@link ServiceInterface} for (case
     *        insensitive) (must not be <code>null</code>)
     * @param serviceInterfaceName
     *        specifies the service interface to get (case insensitive) (must
     *        not be <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return the service interface url, or <code>null</code> if no such
     *         service interface exists
     */
    public String getServiceInterfaceURL(
        final String toolId,
        final String serviceInterfaceName,
        final boolean forceRefresh,
        final boolean relative) {
        return getRegistrationData(forceRefresh).getServiceInterfaceURL(toolId, serviceInterfaceName, relative);
    }

    /**
     * Gets all of the {@link ArtifactType}s for the specified tool. The
     * returned array is safe - no references to the {@link ArtifactType}s are
     * held by this class.
     *
     * @param toolId
     *        specifies the tool to get {@link ArtifactType}s for (case
     *        insensitive) (must not be <code>null</code>)
     * @return all of the {@link ArtifactType}s for the specified tool, or
     *         <code>null</code> if the specified tool does not have a
     *         registration entry
     */
    public ArtifactType[] getArtifactTypes(final String toolId) {
        return getArtifactTypes(toolId, false);
    }

    /**
     * Gets all of the {@link ArtifactType}s for the specified tool, specifying
     * whether to force a refresh of the registration data from the TF server.
     * The returned array is safe - no references to the {@link ArtifactType}s
     * are held by this class.
     *
     * @param toolId
     *        specifies the tool to get {@link ArtifactType}s for (case
     *        insensitive) (must not be <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return all of the {@link ArtifactType}s for the specified tool, or
     *         <code>null</code> if the specified tool does not have a
     *         registration entry
     */
    public ArtifactType[] getArtifactTypes(final String toolId, final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getArtifactTypes(toolId, true);
    }

    /**
     * Gets the specified {@link ArtifactType} for the specified tool. The
     * returned {@link ArtifactType} is safe - no reference to it is held by
     * this class.
     *
     * @param toolId
     *        specifies the tool to get the {@link ArtifactType} for (case
     *        insensitive) (must not be <code>null</code>)
     * @param artifactTypeName
     *        specifies the artifact type to get (case insensitive) (must not be
     *        <code>null</code>)
     * @return the specified {@link ArtifactType} for the specified tool, or
     *         <code>null</code> if the requested artifact type does not exist
     */
    public ArtifactType getArtifactType(final String toolId, final String artifactTypeName) {
        return getArtifactType(toolId, artifactTypeName, false);
    }

    /**
     * Gets the specified {@link ArtifactType} for the specified tool,
     * specifying whether to force a refresh of the registration data from the
     * TF server. The returned {@link ArtifactType} is safe - no reference to it
     * is held by this class.
     *
     * @param toolId
     *        specifies the tool to get the {@link ArtifactType} for (case
     *        insensitive) (must not be <code>null</code>)
     * @param artifactTypeName
     *        specifies the artifact type to get (case insensitive) (must not be
     *        <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return the specified {@link ArtifactType} for the specified tool, or
     *         <code>null</code> if the requested artifact type does not exist
     */
    public ArtifactType getArtifactType(
        final String toolId,
        final String artifactTypeName,
        final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getArtifactType(toolId, artifactTypeName, true);
    }

    /**
     * Gets all of the {@link OutboundLinkType}s for the specified tool and
     * artifact type. The returned array is safe - no references to the
     * {@link OutboundLinkType}s are held by this class.
     *
     * @param toolId
     *        specifies the tool to get {@link OutboundLinkType}s for (case
     *        insensitive) (must not be <code>null</code>)
     * @param artifactTypeName
     *        specifies the artifact type to get (case insensitive) (must not be
     *        <code>null</code>)
     * @return all of the {@link OutboundLinkType}s for the specified tool and
     *         artifact type, or <code>null</code> if there is no such tool and
     *         artifact type
     */
    public OutboundLinkType[] getOutboundLinkTypes(final String toolId, final String artifactTypeName) {
        return getOutboundLinkTypes(toolId, artifactTypeName, false);
    }

    /**
     * Gets all of the {@link OutboundLinkType}s for the specified tool and
     * artifact type, specifying whether to force a refresh of the registration
     * data from the TF server. The returned array is safe - no references to
     * the {@link OutboundLinkType}s are held by this class.
     *
     * @param toolId
     *        specifies the tool to get {@link OutboundLinkType}s for (case
     *        insensitive) (must not be <code>null</code>)
     * @param artifactTypeName
     *        specifies the artifact type to get (case insensitive) (must not be
     *        <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return all of the {@link OutboundLinkType}s for the specified tool and
     *         artifact type, or <code>null</code> if there is no such tool and
     *         artifact type
     */
    public OutboundLinkType[] getOutboundLinkTypes(
        final String toolId,
        final String artifactTypeName,
        final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getOutboundLinkTypes(toolId, artifactTypeName, true);
    }

    /**
     * Gets all of the {@link RegistrationExtendedAttribute}s for the specified
     * tool. The returned array is safe - no references to the
     * {@link RegistrationExtendedAttribute}s are held by this class.
     *
     * @param toolId
     *        specifies the tool to get {@link RegistrationExtendedAttribute}s
     *        for (case insensitive) (must not be <code>null</code>)
     * @return all of the {@link RegistrationExtendedAttribute}s for the
     *         specified tool, or <code>null</code> if the specified tool does
     *         not have a registration entry
     */
    public RegistrationExtendedAttribute[] getExtendedAttributes(final String toolId) {
        return getExtendedAttributes(toolId, false);
    }

    /**
     * Gets all of the {@link RegistrationExtendedAttribute}s for the specified
     * tool, specifying whether to force a refresh of the registration data from
     * the TF server. The returned array is safe - no references to the
     * {@link RegistrationExtendedAttribute}s are held by this class.
     *
     * @param toolId
     *        specifies the tool to get {@link RegistrationExtendedAttribute}s
     *        for (case insensitive) (must not be <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return all of the {@link RegistrationExtendedAttribute}s for the
     *         specified tool, or <code>null</code> if the specified tool does
     *         not have a registration entry
     */
    public RegistrationExtendedAttribute[] getExtendedAttributes(final String toolId, final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getExtendedAttributes(toolId, true);
    }

    /**
     * Gets the specified {@link RegistrationExtendedAttribute} for the
     * specified tool. The returned {@link RegistrationExtendedAttribute} is
     * safe - no reference to it is held by this class.
     *
     * @param toolId
     *        specifies the tool to get the
     *        {@link RegistrationExtendedAttribute} for (case insensitive) (must
     *        not be <code>null</code>)
     * @param attributeName
     *        specifies the attribute to get (case sensitive) (must not be
     *        <code>null</code>)
     * @return the specified {@link RegistrationExtendedAttribute} for the
     *         specified tool, or <code>null</code> if no such attribute exists
     */
    public RegistrationExtendedAttribute getExtendedAttribute(final String toolId, final String attributeName) {
        return getExtendedAttribute(toolId, attributeName, false);
    }

    /**
     * Gets the specified {@link RegistrationExtendedAttribute} for the
     * specified tool, specifying whether to force a refresh of the registration
     * data from the TF server. The returned
     * {@link RegistrationExtendedAttribute} is safe - no reference to it is
     * held by this class.
     *
     * @param toolId
     *        specifies the tool to get the
     *        {@link RegistrationExtendedAttribute} for (case insensitive) (must
     *        not be <code>null</code>)
     * @param attributeName
     *        specifies the attribute to get (case sensitive) (must not be
     *        <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return the specified {@link RegistrationExtendedAttribute} for the
     *         specified tool, or <code>null</code> if no such attribute exists
     */
    public RegistrationExtendedAttribute getExtendedAttribute(
        final String toolId,
        final String attributeName,
        final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getExtendedAttribute(toolId, attributeName, true);
    }

    /**
     * Gets the specified extended attribute value for the specified tool.
     *
     * @param toolId
     *        specifies the tool to get the
     *        {@link RegistrationExtendedAttribute} for (case insensitive) (must
     *        not be <code>null</code>)
     * @param attributeName
     *        specifies the attribute to get (case sensitive) (must not be
     *        <code>null</code>)
     * @return the extended attribute value, or <code>null</code> if no such
     *         value exists
     */
    public String getExtendedAttributeValue(final String toolId, final String attributeName) {
        return getExtendedAttributeValue(toolId, attributeName, false);
    }

    /**
     * Gets the specified extended attribute value for the specified tool,
     * specifying whether to force a refresh of the registration data from the
     * TF server.
     *
     * @param toolId
     *        specifies the tool to get the
     *        {@link RegistrationExtendedAttribute} for (case insensitive) (must
     *        not be <code>null</code>)
     * @param attributeName
     *        specifies the attribute to get (case sensitive) (must not be
     *        <code>null</code>)
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return the extended attribute value, or <code>null</code> if no such
     *         value exists
     */
    public String getExtendedAttributeValue(
        final String toolId,
        final String attributeName,
        final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getExtendedAttributeValue(toolId, attributeName);
    }

    /**
     * Obtains the instance ID (server GUID) from the registration data.
     *
     * @return the server instance ID, or <code>null</code> if the instance ID
     *         could not be determined
     */
    public GUID getInstanceID() {
        return getInstanceID(false);
    }

    /**
     * Obtains the instance ID (server GUID) from the registration data,
     * specifying whether to force a refresh of the registration data from the
     * TF server.
     *
     * @param forceRefresh
     *        <code>true</code> to force a call to the server to refresh the
     *        registration data
     * @return the server instance ID, or <code>null</code> if the instance ID
     *         could not be determined
     */
    public GUID getInstanceID(final boolean forceRefresh) {
        return getRegistrationData(forceRefresh).getInstanceIDExtendedAttributeValue();
    }

    /**
     * Called to ensure that the registration data is up to date, specifying
     * whether to force a refresh of the data from the TF server. If force is
     * <code>false</code> and the cached data not stale, this method does
     * nothing. Otherwise, the TF server is contacted to get the latest
     * registration data.
     *
     * @param force
     *        <code>true</code> to force a server refresh even if the cached
     *        data is not stale
     */
    public void refresh(final boolean force) {
        getRegistrationData(force);
    }

    private RegistrationData getRegistrationData(final boolean forceRefresh) {
        /*
         * Get this here so that we don't hold a lock while calling
         * TFSConnection methods. TFSConnection often calls into Registration
         * Data, while holding a lock. This is deadlock prevention.
         */
        final PersistenceStore cacheStore = connection.getPersistenceStoreProvider().getCachePersistenceStore();
        final URI serverUri = connection.getBaseURI();

        /*
         * Use a temporary variable to avoid other threads clobbering our data
         * on the volatile thread.
         */
        RegistrationData registrationDataRef = this.registrationData;

        if (!forceRefresh) {
            if (registrationDataRef == null && enableDiskCache) {
                registrationDataRef = RegistrationData.load(cacheStore, serverUri);
            }

            if (registrationDataRef != null && !registrationDataRef.isDataStale(refreshIntervalMillis)) {
                return registrationDataRef;
            }
        }

        registrationDataRef = RegistrationData.newFromServer(webService, serverUri);

        if (enableDiskCache) {
            writeDiskCache(cacheStore, serverUri, registrationDataRef);
        }

        this.registrationData = registrationDataRef;
        return registrationDataRef;
    }

    private void writeDiskCache(
        final PersistenceStore store,
        final URI serverUri,
        final RegistrationData registrationData) {
        Check.notNull(store, "store"); //$NON-NLS-1$
        Check.notNull(serverUri, "serverUri"); //$NON-NLS-1$

        final GUID instanceId = registrationData.getInstanceIDExtendedAttributeValue();

        if (instanceId == null) {
            /*
             * if the instance ID can't be determined, we can't cache anything
             */
            return;
        }

        /*
         * server map
         */
        final ServerMap serverMap = ServerMap.load(store);
        serverMap.addServerID(serverUri.toString(), instanceId);
        serverMap.save(store);

        /*
         * registration entry cache
         */
        final String childLocationName = RegistrationData.makeChildLocationName(serverUri, instanceId.getGUIDString());
        registrationData.save(store, childLocationName);
    }

    public String getRosarioURLForTeamProject(final String interfaceName, final String projectName) {
        // For back-compat the TFS 2010 server will return some useful URL's for
        // us under the TeamProjects tool. This saves the client needing a full
        // LocationService/CatalogService mechanism. The URL's for the Report
        // Service and Sharepoint services are provided in the format
        // projectName:interfaceName
        Check.notNullOrEmpty(interfaceName, "interfaceName"); //$NON-NLS-1$
        Check.notNullOrEmpty(projectName, "projectName"); //$NON-NLS-1$

        return getServiceInterfaceURL("TeamProjects", projectName + ":" + interfaceName); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
