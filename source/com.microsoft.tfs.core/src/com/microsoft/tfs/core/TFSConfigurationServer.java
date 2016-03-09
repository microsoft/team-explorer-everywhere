// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.catalog.ICatalogService;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntitySession;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamFoundationServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSEntitySessionFactory;
import com.microsoft.tfs.core.clients.framework.location.ConnectOptions;
import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.core.clients.framework.location.LocationServiceConstants;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.DefaultConnectionAdvisor;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * A connection to the configuration server area of a TFS 2010 or later
 * installation.
 *
 * @see TFSConnection
 *
 * @since TEE-SDK-10.1
 * @since TFS 2010
 * @threadsafety thread-safe
 */
public class TFSConfigurationServer extends TFSConnection {
    private static final Log log = LogFactory.getLog(TFSConfigurationServer.class);

    /**
     * Provide information about the server we're connected to.
     */
    private ServerDataProvider serverDataProvider;
    private final Object serverDataProviderLock = new Object();

    /**
     * The catalog service based configuration session ("entity session" in .NET
     * OM) for this {@link TFSconfigurationServer}. Initialized lazily by
     * {@link #getConfigurationSession(boolean)}.
     */
    private TFSEntitySession configurationSession;
    private final Object configurationSessionLock = new Object();

    /**
     * A cache of the {@link TFSTeamProjectCollection}s we have loaded. Key is
     * {@link String} (the collection location) and value is
     * {@link TFSTeamProjectCollection}. Synchronize on this object.
     */
    private final Map<String, TFSTeamProjectCollection> collections = new HashMap<String, TFSTeamProjectCollection>();
    private final Object collectionsLock = new Object();

    /**
     * A convenience constructor to create a {@link TFSConfigurationServer} from
     * a {@link URI} and {@link Credentials}. A default
     * {@link ConnectionAdvisor} is used.
     *
     * @param serverURI
     *        the {@link URI} to connect to (must not be <code>null</code>)
     * @param credentials
     *        the {@link Credentials} to connect with
     */
    public TFSConfigurationServer(final URI serverURI, final Credentials credentials) {
        this(serverURI, credentials, new DefaultConnectionAdvisor(Locale.getDefault(), TimeZone.getDefault()));

        /*
         * TODO Remove this when we have an SDK so they don't get warnings? For
         * now it helps with the persistence transition.
         */
        final String messageFormat =
            "Using {0} for TFSConfigurationServer, which is undesirable for Team Foundation Server client applications"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, DefaultConnectionAdvisor.class.getName());
        log.warn(message);
    }

    /**
     * The most complete way of creating a {@link TFSConfigurationServer}. A
     * {@link URI}, {@link Credentials} and a {@link ConnectionAdvisor} are
     * specified.
     *
     * @param serverURI
     *        the {@link URI} to connect to (must not be <code>null</code>)
     * @param credentials
     *        the {@link Credentials} to connect with
     * @param advisor
     *        the {@link ConnectionAdvisor} to use (must not be
     *        <code>null</code>)
     */
    public TFSConfigurationServer(final URI serverURI, final Credentials credentials, final ConnectionAdvisor advisor) {
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
    protected TFSConfigurationServer(
        final URI serverURI,
        final AtomicReference<Credentials> credentialsHolder,
        final ConnectionAdvisor advisor) {
        super(
            serverURI,
            credentialsHolder,
            advisor,
            LocationServiceConstants.APPLICATION_LOCATION_SERVICE_RELATIVE_PATH);
    }

    /**
     * Gets the {@link TFSTeamProjectCollection} for the specified ID.
     *
     * @param collectionID
     *        the collection's ID (must not be <code>null</code>)
     * @return the {@link TFSTeamProjectCollection} that matches the ID, or
     *         <code>null</code> if no matching collection was found
     */
    public TFSTeamProjectCollection getTeamProjectCollection(final GUID collectionID) {
        checkNotClosed();

        Check.notNull(collectionID, "collectionID"); //$NON-NLS-1$

        final String collectionLocation = getServerDataProvider().findServerLocation(collectionID);

        TFSTeamProjectCollection ret = null;

        synchronized (collectionsLock) {
            if (collectionLocation != null) {
                if (collections.containsKey(collectionLocation)) {
                    ret = collections.get(collectionLocation);
                } else {
                    /*
                     * Collection location comes from the server data provider
                     * as a properly formed (escaped) URI. Do not use helper
                     * methods that would re-escape.
                     */
                    final URI collectionLocationURI;

                    try {
                        collectionLocationURI = new URI(collectionLocation);
                    } catch (final URISyntaxException e) {
                        throw new IllegalArgumentException(e.getLocalizedMessage(), e);
                    }

                    ret = new TFSTeamProjectCollection(
                        collectionLocationURI,
                        getCredentialsHolder(),
                        getConnectionAdvisor());
                    ret.setHTTPClientReference(getHTTPClientReference());

                    collections.put(collectionLocation, ret);
                }
            }
        }

        return ret;
    }

    /**
     * Gets the catalog service {@link TFSEntitySession} for this configuration
     * server.
     *
     * @param refresh
     *        <code>true</code> to force a refresh of the data from the server,
     *        <code>false</code> to use cached data
     * @return The {@link TFSEntitySession} for this configuration server
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
     * Gets the team {@link ProjectCollectionEntity} for this configuration
     * server.
     *
     * @param refresh
     *        <code>true</code> to force a refresh of the data from the server,
     *        <code>false</code> to use cached data
     * @return the catalog-service based server entity for this configuration
     *         server project collection, or <code>null</code> if none could be
     *         found (ie, pre-framework server.)
     */
    public TeamFoundationServerEntity getTeamFoundationServerEntity(final boolean refresh) {
        checkNotClosed();

        final TFSEntitySession configurationSession = getConfigurationSession(refresh);

        if (configurationSession == null || configurationSession.getOrganizationalRoot() == null) {
            log.warn(MessageFormat.format("Could not load configuration session for instance id {0}", getInstanceID())); //$NON-NLS-1$
            return null;
        }

        final TeamFoundationServerEntity serverEntity =
            configurationSession.getOrganizationalRoot().getTeamFoundationServer();

        if (serverEntity == null) {
            log.warn(MessageFormat.format(
                "Could not load Team Foundation Server entity for instance id {0}", //$NON-NLS-1$
                getInstanceID()));
        }

        return serverEntity;
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
                     * cached data for it. If there is no cached data, connect.
                     */
                    final FrameworkServerDataProvider tempFrameworkProvider = new FrameworkServerDataProvider(this);

                    if (tempFrameworkProvider.hasLocalCacheDataForConnection() == false) {
                        tempFrameworkProvider.connect(ConnectOptions.INCLUDE_SERVICES);
                    }

                    serverDataProvider = tempFrameworkProvider;
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
    public ICatalogService getCatalogService() {
        checkNotClosed();

        return (ICatalogService) getClient(ICatalogService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAuthenticated() {
        checkNotClosed();

        final ServerDataProvider serverDataProvider;

        synchronized (serverDataProviderLock) {
            serverDataProvider = this.serverDataProvider;
        }

        return (serverDataProvider != null) ? serverDataProvider.hasAuthenticated() : false;
    }

    public void reactToPossibleServerUpdate(final int locationServiceLastChangeId) {
        checkNotClosed();

        getServerDataProvider().reactToPossibleServerUpdate(locationServiceLastChangeId);
    }

    /**
     * The registration service is not available for a configuration server
     * instance.
     *
     * {@inheritDoc}
     */
    @Override
    public RegistrationClient getRegistrationClient() {
        checkNotClosed();

        return null;
    }

    public ILocationService getLocationService() {
        checkNotClosed();

        return (ILocationService) getClient(ILocationService.class);
    }
}
