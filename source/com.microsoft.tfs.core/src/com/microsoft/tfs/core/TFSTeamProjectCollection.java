// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.catalog.ICatalogService;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntitySession;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.ProjectCollectionCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.TeamProjectCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSEntitySessionFactory;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.framework.location.ConnectOptions;
import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.core.clients.framework.location.LocationServiceConstants;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.DefaultConnectionAdvisor;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.GUID;

/**
 * <p>
 * A connection to a team project collection in a TFS 2010 or later server, or
 * to the entire service area of a TFS 2005 or TFS 2008 server.
 * </p>
 *
 * @see TFSConnection
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class TFSTeamProjectCollection extends TFSConnection {
    private static final Log log = LogFactory.getLog(TFSTeamProjectCollection.class);

    /**
     * The single configuration server for this project collection.
     *
     * Initialized lazily by {@link #getConfigurationServer()}.
     */
    private TFSConfigurationServer configurationServer;
    private final Object configurationServerLock = new Object();

    /**
     * Provide information about the server we're connected to. Initialized
     * lazily by {@link #getServerDataProvider()}.
     */
    private ServerDataProvider serverDataProvider;
    private final Object serverDataProviderLock = new Object();

    /**
     * The catalog service based configuration session ("entity session" in .NET
     * OM) for this {@link TFSTeamProjectCollection}. Initialized lazily by
     * {@link #getConfigurationSession(boolean)}.
     */
    private TFSEntitySession configurationSession;
    private final Object configurationSessionLock = new Object();

    /**
     * A convenience constructor to create a {@link TFSTeamProjectCollection}
     * from a {@link URI}. A default {@link ConnectionAdvisor} is used.
     *
     * @param uri
     *        the {@link URI} to use to connect (must not be <code>null</code>)
     * @param credentials
     *        the {@link Credentials} to connect with (or <code>null</code> to
     *        attempt to use the best available credentials)
     */
    public TFSTeamProjectCollection(final URI serverURI, final Credentials credentials) {
        this(serverURI, credentials, new DefaultConnectionAdvisor(Locale.getDefault(), TimeZone.getDefault()));
    }

    /**
     * The most complete way of creating a {@link TFSTeamProjectCollection}. A
     * {@link URI}, {@link Credentials} and a {@link ConnectionAdvisor} are
     * specified.
     *
     * @param serverURI
     *        the {@link URI} to connect to (must not be <code>null</code>)
     * @param credentials
     *        the {@link Credentials} to connect with (or <code>null</code> to
     *        attempt to use the best available credentials)
     * @param advisor
     *        the {@link ConnectionAdvisor} to use (must not be
     *        <code>null</code>)
     */
    public TFSTeamProjectCollection(
        final URI serverURI,
        final Credentials credentials,
        final ConnectionAdvisor advisor) {
        this(serverURI, new AtomicReference<Credentials>(credentials), advisor);
    }

    /**
     * Package-protected constructor that allows {@link TFSConfigurationServer}
     * and {@link TFSTeamProjectCollection}s to share credentials (that may be
     * updated at any time) by way of an {@link AtomicReference}.
     *
     * @param serverURI
     *        the {@link URI} to connect to (must not be <code>null</code>)
     * @param credentialsHolder
     *        an {@link AtomicReference} to the {@link Credentials} to connect
     *        with (must not be <code>null</code>)
     * @param advisor
     *        the {@link ConnectionAdvisor} to use (must not be
     *        <code>null</code>)
     */
    protected TFSTeamProjectCollection(
        final URI serverURI,
        final AtomicReference<Credentials> credentialsHolder,
        final ConnectionAdvisor advisor) {
        super(
            serverURI,
            credentialsHolder,
            advisor,
            LocationServiceConstants.COLLECTION_LOCATION_SERVICE_RELATIVE_PATH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerDataProvider getServerDataProvider() {
        checkNotClosed();
        synchronized (serverDataProviderLock) {
            if (serverDataProvider == null) {
                try {
                    /*
                     * Create a FrameworkServerDataProvider and see if there's
                     * cached data for it.
                     */
                    final FrameworkServerDataProvider tempFrameworkProvider = new FrameworkServerDataProvider(this);

                    if (tempFrameworkProvider.hasLocalCacheDataForConnection()) {
                        /*
                         * Cached data, use the FrameworkServerDataProvider
                         * provider.
                         */
                        serverDataProvider = tempFrameworkProvider;
                    } else {
                        /*
                         * No cache data, we have to contact the server. Use a
                         * PreFrameworkServerDataProvider, so we can talk with
                         * old servers.
                         */
                        final PreFrameworkServerDataProvider tempPreFrameworkProvider =
                            new PreFrameworkServerDataProvider(this);

                        /*
                         * Query for the location service (a TFS 2010
                         * Framework-era service).
                         */
                        final String locationServiceLocation = tempPreFrameworkProvider.locationForCurrentConnection(
                            ServiceInterfaceNames.LOCATION,
                            LocationServiceConstants.SELF_REFERENCE_LOCATION_SERVICE_IDENTIFIER);

                        /*
                         * If the Location service was not found, the server is
                         * a pre-framework server (< 2010), so use that kind of
                         * provider. Otherwise, use the Framework provider and
                         * connect it immediately.
                         */
                        if (locationServiceLocation == null) {
                            log.error(
                                MessageFormat.format(
                                    "You cannot connect to {0} because it is running a version of Team Foundation Server that is not supported by your version of Visual Studio. Upgrade your server to Team Foundation Server 2010 (or a newer version), or use Visual Studio 2010, Visual Studio 2012 or Visual Studio 2015.", //$NON-NLS-1$
                                    getBaseURI()));

                            throw new NotSupportedException(MessageFormat.format(
                                Messages.getString("TFSTeamProjectCollection.NotSupportedTfsVersionFormat"), //$NON-NLS-1$
                                getBaseURI()));
                        } else {
                            tempFrameworkProvider.connect(ConnectOptions.INCLUDE_SERVICES);
                            serverDataProvider = tempFrameworkProvider;
                        }
                    }

                } catch (final RuntimeException e) {
                    log.warn("Error getting data provider", e); //$NON-NLS-1$

                    // TODO catch the right kind of connection exception and
                    // make a
                    // good error message

                    // WebException webException = ex as WebException;
                    // if (webException != null)
                    // {
                    // HttpWebResponse webResponse = webException.Response as
                    // HttpWebResponse;
                    // if (webResponse != null && webResponse.StatusCode ==
                    // HttpStatusCode.NotFound)
                    // {
                    // throw new
                    // TeamFoundationServiceUnavailableException(ClientResources.ConnectToTfs_AddServer_UnableToConnect_WithTechnicalInfo(Uri.ToString(),
                    // Uri.ToString(), ex.Message), ex);
                    // }
                    // }

                    throw e;
                }
            }

            return serverDataProvider;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ICatalogService getCatalogService() {
        checkNotClosed();
        /*
         * Use the catalog service from our configuration server.
         */
        return getConfigurationServer().getCatalogService();
    }

    /**
     * @return this team project collection's configuration server connection.
     */
    public TFSConfigurationServer getConfigurationServer() {
        checkNotClosed();
        synchronized (configurationServerLock) {
            if (configurationServer == null) {
                final String location = getServerDataProvider().locationForCurrentConnection(
                    ServiceInterfaceNames.LOCATION,
                    LocationServiceConstants.APPLICATION_LOCATION_SERVICE_IDENTIFIER);

                if (location != null && location.length() > 0) {
                    final URI locationURI = URIUtils.newURI(location);

                    configurationServer =
                        new TFSConfigurationServer(locationURI, getCredentialsHolder(), getConnectionAdvisor());
                    configurationServer.setHTTPClientReference(getHTTPClientReference());
                }
            }
            return configurationServer;
        }
    }

    @Override
    public void close() {
        if (isClosed()) {
            warnClosed();
            return;
        }

        super.close();

        synchronized (configurationServerLock) {
            if (configurationServer != null && !configurationServer.isClosed()) {
                configurationServer.close();
            }
            configurationServer = null;
        }
    }

    public ILocationService getCollectionLocationService() {
        checkNotClosed();
        return (ILocationService) getClient(ILocationService.class);
    }

    /**
     * Gets the catalog service {@link TFSEntitySession} for this team project
     * collection.
     *
     * @param refresh
     *        <code>true</code> to force a refresh of the data from the server,
     *        <code>false</code> to use cached data
     * @return The {@link TFSEntitySession} for this project collection
     */
    public TFSEntitySession getConfigurationSession(final boolean refresh) {
        checkNotClosed();
        synchronized (configurationSessionLock) {
            if (configurationSession == null || refresh) {
                configurationSession = TFSEntitySessionFactory.newEntitySession(this);
            }

            return configurationSession;
        }
    }

    /**
     * Gets the team {@link ProjectCollectionEntity} for this collection.
     *
     * @param refresh
     *        <code>true</code> to force a refresh of the data from the server,
     *        <code>false</code> to use cached data
     * @return the catalog-service based project collection entity for this team
     *         project collection, or <code>null</code> if none could be found
     *         (ie, pre-framework server.)
     */
    public ProjectCollectionEntity getTeamProjectCollectionEntity(final boolean refresh) {
        checkNotClosed();
        final TFSEntitySession configurationSession = getConfigurationSession(refresh);

        if (configurationSession == null || configurationSession.getOrganizationalRoot() == null) {
            return null;
        }

        if (configurationSession.getOrganizationalRoot().getTeamFoundationServer() == null) {
            log.warn("Could not load Team Foundation Server entity from organizational root"); //$NON-NLS-1$
            return null;
        }

        final ProjectCollectionEntity collectionEntity =
            configurationSession.getOrganizationalRoot().getTeamFoundationServer().getProjectCollection(
                getInstanceID());

        if (collectionEntity == null) {
            log.warn(MessageFormat.format(
                "Could not load Team Project Collection entity for instance id {0}", //$NON-NLS-1$
                getInstanceID()));
        }

        return collectionEntity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean hasAuthenticated() {
        checkNotClosed();
        final ServerDataProvider serverDataProvider;

        synchronized (serverDataProviderLock) {
            serverDataProvider = this.serverDataProvider;
        }

        return (serverDataProvider != null) ? serverDataProvider.hasAuthenticated() : false;
    }

    /*
     * Convenience methods for client access (so the user doesn't have to guess
     * the class type to pass to getClient()).
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public RegistrationClient getRegistrationClient() {
        checkNotClosed();
        return (RegistrationClient) getClient(RegistrationClient.class);
    }

    /**
     * A convenience method to get the version control client from this
     * {@link TFSConnection}.
     *
     * @return the {@link VersionControlClient}
     */
    public VersionControlClient getVersionControlClient() {
        checkNotClosed();
        return (VersionControlClient) getClient(VersionControlClient.class);
    }

    /**
     * A convenience method to get the WIT client from this
     * {@link TFSConnection}.
     *
     * @return the {@link WorkItemClient}
     */
    public WorkItemClient getWorkItemClient() {
        checkNotClosed();
        return (WorkItemClient) getClient(WorkItemClient.class);
    }

    /**
     * A convenience method to get the Build service client from this
     * {@link TFSConnection}.
     *
     * @return the {@link BuildClient} which implements {@link IBuildServer}
     */
    public IBuildServer getBuildServer() {
        checkNotClosed();
        return (IBuildServer) getClient(IBuildServer.class);
    }

    /**
     * A convenience method to get the common structure service client from this
     * {@link TFSConnection}.
     *
     * @return the {@link CommonStructureClient}
     */
    public CommonStructureClient getCommonStructureClient() {
        checkNotClosed();
        return (CommonStructureClient) getClient(CommonStructureClient.class);
    }

    public SourceControlCapabilityFlags getSourceControlCapability(final ProjectInfo project) {
        checkNotClosed();
        if (getVersionControlClient().getWebServiceLayer().getServiceLevel().getValue() < WebServiceLevel.TFS_2012_QU1.getValue()) {
            return SourceControlCapabilityFlags.TFS;
        } else {
            try {
                ProjectCollectionCatalogEntity collectionCatalogEntry =
                    (ProjectCollectionCatalogEntity) getTeamProjectCollectionEntity(false);
                if (collectionCatalogEntry == null) {
                    // Refresh if the collection entry is stale
                    collectionCatalogEntry = (ProjectCollectionCatalogEntity) getTeamProjectCollectionEntity(true);
                }

                TeamProjectCatalogEntity projectCatalogEntry =
                    (TeamProjectCatalogEntity) collectionCatalogEntry.getTeamProject(new GUID(project.getGUID()));
                if (projectCatalogEntry == null) {
                    // Refresh if the collection catalog entry is stale
                    collectionCatalogEntry = (ProjectCollectionCatalogEntity) getTeamProjectCollectionEntity(true);
                    projectCatalogEntry =
                        (TeamProjectCatalogEntity) collectionCatalogEntry.getTeamProject(new GUID(project.getGUID()));
                }

                SourceControlCapabilityFlags sourceControlCapabilityFlag = SourceControlCapabilityFlags.NONE;

                if (projectCatalogEntry.isGitSupported()) {
                    sourceControlCapabilityFlag = sourceControlCapabilityFlag.combine(SourceControlCapabilityFlags.GIT);
                }

                if (projectCatalogEntry.isTfvcSupported()) {
                    sourceControlCapabilityFlag = sourceControlCapabilityFlag.combine(SourceControlCapabilityFlags.TFS);
                }

                return sourceControlCapabilityFlag;
            } catch (final Exception e) {
                log.error("Unexpected error accessing Catalog Entry: ", e); //$NON-NLS-1$
                return SourceControlCapabilityFlags.TFS;
            }
        }
    }

}
