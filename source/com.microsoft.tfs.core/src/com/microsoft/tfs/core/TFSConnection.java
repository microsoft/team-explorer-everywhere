// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogQueryOptions;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogResource;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogRoots;
import com.microsoft.tfs.core.clients.framework.catalog.ICatalogService;
import com.microsoft.tfs.core.clients.framework.location.ConnectOptions;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.sharepoint.WSSClient;
import com.microsoft.tfs.core.clients.webservices.IdentityAttributeTags;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.IllegalConfigurationException;
import com.microsoft.tfs.core.config.client.ClientFactory;
import com.microsoft.tfs.core.config.client.UnknownClientException;
import com.microsoft.tfs.core.config.httpclient.HTTPClientFactory;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.config.serveruri.ServerURIProvider;
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettings;
import com.microsoft.tfs.core.config.tfproxy.TFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.webservice.UnknownWebServiceException;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;
import com.microsoft.tfs.core.util.TFSUser;
import com.microsoft.tfs.core.util.TFSUsernameParseException;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Closable;
import com.microsoft.tfs.util.CollatorFactory;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.URLEncode;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;
import com.microsoft.tfs.util.shutdown.ShutdownEventListener;
import com.microsoft.tfs.util.shutdown.ShutdownManager;
import com.microsoft.tfs.util.shutdown.ShutdownManager.Priority;

import ms.tfs.services.linking._03._IntegrationServiceSoap;
import ms.tfs.services.registration._03._RegistrationSoap;
import ms.ws._LocationWebServiceSoap;
import ms.wss._ListsSoap;

/**
 * <p>
 * A generic connection to a Team Foundation Server: subclasses will provide
 * access to either TFS Configuration Server or a Team Project Collection.
 * </p>
 *
 * <p>
 * A {@link TFSConnection} instance is ultimately created by specifying two
 * things: a {@link URI} that identifies the URI, and a
 * {@link ConnectionAdvisor} that supplies environmental settings (and can
 * influence how some profile data is interpreted).
 * </p>
 *
 * <p>
 * A {@link TFSConnection} provides the following services:
 * <ul>
 * <li>A session ID ({@link #getSessionID()}), which is a unique GUID associated
 * with every {@link TFSConnection} instance. This ID may be sent to the TFS in
 * HTTP headers.</li>
 * <li>An {@link HttpClient} ({@link #getHTTPClient()}), which is configured for
 * use with a Team Foundation Server (credentials, proxy settings, etc. are
 * automatically configured from profile information).</li>
 * <li>A Team Foundation Server URI ({@link #getBaseURI()}), which is the fully
 * qualified URI to the team project collection <em>or</em> configuration server
 * instance this {@link TFSConnection} is connected to.</li>
 * <li>Information about the currently authenticated and authorized user(s).
 * </li>
 * <li>Client instances ({@link #getClient(Class)}) for high-level access to TFS
 * services (version control, work items, build, etc.).</li>
 * <li>A factory for web service proxy classes ({@link #getWebService(Class)})
 * for low-level access to TFS services. This is for internal use by client
 * classes. In almost all cases you should use the client classes instead of
 * using the web proxy classes directly.</li>
 * <li>Team Foundation Proxy (file download proxy) server settings (
 * {@link #getTFProxyServerSettings()}).</li>
 * <li>Locale ({@link #getLocale()}) and time zone ({@link #getTimeZone()})
 * settings associated with this {@link TFSConnection}.</li>
 * <li>The instance ID (server GUID) of the connected TFS configuration server
 * or project collection( {@link #getInstanceID()}).</li>
 * </ul>
 * </p>
 *
 * <p>
 * {@link TFSConnection} implements the {@link Closable} interface. Clients that
 * instantiate {@link TFSConnection} instances must call {@link #close()} on
 * those instances when they are no longer needed. This allows the
 * {@link TFSConnection} instance and all allocated client classes to perform
 * cleanup operations (delete temporary files, close cache files, etc.).
 * {@link TFSConnection} instances register JVM shutdown event listeners which
 * call {@link #close()}, but it's better for clients to manually invoke
 * {@link #close()} as soon as possible. Warnings will be logged by the JVM
 * shutdown event listeners when the closing the {@link TFSConnection}. A closed
 * {@link TFSConnection} will throw {@link TFSConnectionClosedException}s and
 * therefore should not be accessed.
 * </p>
 *
 * <p>
 * {@link TFSConnection} is thread safe. Objects obtained from a
 * {@link TFSConnection} document their threading policy individually.
 * </p>
 *
 * <p>
 * For both {@link TFSTeamProjectCollection} and {@link TFSConfigurationServer}
 * the server URL string provided in the constructor may be either:
 * <ol>
 * <li>A "base" URL, which points to where the TFS configuration server is
 * installed or to any team project collection</li>
 * <li>The full URL to the location service inside either a TFS configuration
 * server or any project collection</li>
 * </ol>
 * A user normally provides the first type when a URI is created via UI input or
 * command-line arguments. The second type is occasionally convenient for
 * programmatic construction (this method is used internally).
 * {@link TFSConnection}'s constructor determines which type was provided by
 * matching against a known location service endpoint suffix, then computes
 * {@link #getBaseURI()} and {@link #getLocationServiceURI()} from the URL.
 * </p>
 *
 * @see ConnectionAdvisor
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public abstract class TFSConnection implements Closable {
    private static final Log log = LogFactory.getLog(TFSConnection.class);

    /**
     * The {@link ConnectionAdvisor} (constant) in use by this
     * {@link TFSConnection} (never <code>null</code>).
     */
    private final ConnectionAdvisor advisor;

    /**
     * The session ID (constant) associated with this {@link TFSConnection}
     * (never <code>null</code>). A client-side identifier to identify unique
     * {@link TFSConnection} instances. It may be sent to the TFS in HTTP
     * headers.
     */
    private final GUID sessionId;

    /**
     * Encapsulates the current connection information that we pass to
     * {@link ConnectionAdvisor} methods (never <code>null</code>). Constant
     * after initialization.
     */
    private final ConnectionInstanceData instanceData;

    /**
     * The {@link ShutdownEventListener} (constant) that we register with the
     * JVM to call {@link #close()} when the JVM shuts down (never
     * <code>null</code>).
     */
    private final ShutdownEventListener shutdownEventListener;

    /**
     * A cache of WSS clients (initially empty). Maps from {@link String}
     * (project guid) -> {@link WSSClient} instance.
     */
    private final Map<String, WSSClient> wssClients = new HashMap<String, WSSClient>();
    private final Object wssClientsLock = new Object();

    /**
     * A cache of non-WSS clients (initially empty). Maps from {@link Class}
     * (client type) -> {@link Object} (client instance).
     */
    private final Map<Class<?>, Object> clients = new HashMap<Class<?>, Object>();
    private final Object clientsLock = new Object();

    /**
     * A holder for an {@link HttpClient} that does reference counting (so that
     * different types of {@link TFSConnection}s to the same server can share a
     * single {@link HttpClient} instance.
     */
    private HTTPClientReference httpClientReference;

    /**
     * Creates {@link HttpClient}s on demand.
     */
    private HTTPClientFactory httpClientFactory;

    /** A lock for {@link httpClientReference} and {@link httpClientFactory}. */
    private final Object httpClientLock = new Object();

    /**
     * The path to the location service, relative to the scheme, host, and port
     * part of the URI returned by {@link #getBaseURI()}. This will almost
     * always be a constant string from the specialized location service
     * required by the extending class, and never exposed outside the OM.
     * Constant.
     */
    private final String locationServiceRelativePath;

    /**
     * The base URI where this {@link TFSConnection} connects. Lazily
     * initialized by {@link #getBaseURI()}. Initially <code>null</code> .
     *
     * @see #getBaseURI()
     */
    private URI baseURI;
    private final Object baseURILock = new Object();

    /**
     * The human-readable name for this server (the decoded URI string). Lazily
     * initialized by {@link #getName()}. Initially <code>null</code>.
     *
     * @see #getName()
     */
    private String displayName;
    private final Object displayNameLock = new Object();

    /**
     * The URI which points to the location service this {@link TFSConnection}
     * uses. Lazily initialized by {@link #getLocationServiceURI()}. Initially
     * <code>null</code>.
     *
     * @see #getLocationServiceURI()
     */
    private URI locationServiceURI;
    private final Object locationServiceURILock = new Object();

    /**
     * The cached catalog node for this connection.
     */
    private CatalogNode catalogNode;
    private final Object catalogNodeLock = new Object();

    /**
     * The lazily created {@link TFProxyServerSettings} instance. Initially
     * <code>null</code>.
     */
    private TFProxyServerSettings tfProxyServerSettings;

    /**
     * The lazily created {@link TFProxyServerSettingsFactory} instance.
     * Initially <code>null</code>.
     */
    private TFProxyServerSettingsFactory tfProxyServerSettingsFactory;

    /**
     * A lock for {@link tfProxyServerSettings} and
     * {@link tfProxyServerSettingsFactory}.
     */
    private final Object tfProxyServerSettingsLock = new Object();

    /**
     * <code>true</code> if the lazily created {@link TFProxyServerSettings}
     * needs to be disposed.
     */
    private boolean needToDisposeTfProxyServerSettings;

    /**
     * The lazily created {@link TimeZone} instance. Initially <code>null</code>
     * .
     */
    private TimeZone timeZone;
    private final Object timeZoneLock = new Object();

    /**
     * The lazily created {@link Locale} instance. Initially <code>null</code>.
     */
    private Locale locale;
    private final Object localeLock = new Object();

    /**
     * The lazily created {@link Collator} for case-sensitive collation in this
     * connection's {@link Locale}. Initially <code>null</code>.
     */
    private Collator caseSensitiveCollator;

    /**
     * The lazily created {@link Collator} for case-insensitive collation in
     * this connection's {@link Locale}. Initially <code>null</code>.
     */
    private Collator caseInsensitiveCollator;

    /** A lock for the collators above. */
    private final Object collatorLock = new Object();

    /**
     * The lazily created {@link PersistenceStoreProvider} for cache data.
     * Initially <code>null</code>.
     */
    private PersistenceStoreProvider persistenceStoreProvider;
    private final Object persistenceStoreProviderLock = new Object();

    /**
     * The lazily created {@link WebServiceFactory} instance. Initially
     * <code>null</code>.
     */
    private WebServiceFactory webServiceFactory;
    private final Object webServiceFactoryLock = new Object();

    /**
     * The lazily created {@link ClientFactory} instance. Initially
     * <code>null</code>.
     */
    private ClientFactory clientFactory;
    private final Object clientFactoryLock = new Object();

    /**
     * If the last web service call failed due to a connectivity error, we store
     * that here.
     */
    private final AtomicBoolean connectivityFailureOnLastWebServiceCall = new AtomicBoolean(false);

    private final SingleListenerFacade connectivityFailureListeners =
        new SingleListenerFacade(ConnectivityFailureStatusChangeListener.class);

    private boolean connectionClosed = false;

    /**
     * Creates a {@link TFSConnection}. Both a {@link URI} and a
     * {@link ConnectionAdvisor} are specified.
     * <p>
     * The URI in the profile must be the fully qualified URI to the server; it
     * should not point to a project collection.
     *
     * @param serverURI
     *        the {@link URI} to connect to (must not be <code>null</code>).
     *        This URI must be properly URI encoded.
     * @param credentialsHolder
     *        an {@link AtomicReference} to the {@link Credentials} to connect
     *        with (must not be <code>null</code>)
     * @param advisor
     *        the {@link ConnectionAdvisor} to use (must not be
     *        <code>null</code>)
     * @param locationServiceRelativePath
     *        the URI path (relative to the scheme, host, and port of the main
     *        URI computed from the profile data) where the location service can
     *        be reached for this connection (must not be <code>null</code> or
     *        empty)
     */
    protected TFSConnection(
        URI serverURI,
        final AtomicReference<Credentials> credentialsHolder,
        final ConnectionAdvisor advisor,
        final String locationServiceRelativePath) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(advisor, "advisor"); //$NON-NLS-1$
        Check.notNullOrEmpty(locationServiceRelativePath, "locationServiceRelativePath"); //$NON-NLS-1$

        this.locationServiceRelativePath = locationServiceRelativePath;

        /*
         * Build a new server URL in the short format.
         *
         * getBaseURI() must use a similar rewrite rule because it defers to the
         * ConnectionAdvisor's URI provider, which may decide to return a
         * different URI even though the server URL has already been shortened.
         */
        final String serverURIShortFormat = serverURI.toString();
        if (serverURIShortFormat != null
            && serverURIShortFormat.toLowerCase().endsWith(this.locationServiceRelativePath.toLowerCase())) {
            try {
                /*
                 * Note that the input URI is literal (already encoded, if
                 * necessary) so we use the URI ctor instead of URIUtils#newURI
                 * method (which would doubly encode).
                 */
                serverURI = new URI(
                    serverURIShortFormat.substring(
                        0,
                        serverURIShortFormat.length() - this.locationServiceRelativePath.length()));
            } catch (final URISyntaxException e) {
                throw new IllegalArgumentException(e.getLocalizedMessage(), e);
            }
        }

        this.advisor = advisor;

        this.sessionId = GUID.newGUID();
        this.instanceData = new ConnectionInstanceData(serverURI, credentialsHolder, sessionId);

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format(
                "Created a new TFSConnection, sessionId: [{0}], server URI: [{1}]", //$NON-NLS-1$
                sessionId,
                serverURI));
        }

        shutdownEventListener = new ShutdownEventListener() {
            final Throwable creationStack = isSDK() ? new Throwable(
                "The TFSConnection was closed during JVM shutdown because it was not properly closed by the creator.  The creator of the TFSConnection should close the connection when finished with it.") //$NON-NLS-1$
                : null;

            @Override
            public void onShutdown() {
                if (isClosed()) {
                    /**
                     * During shutdown it is possible for
                     * TFSConfigurationServers to be closed by their owning
                     * TFSTeamProjectCollections, but the shutdown listener is
                     * already iterating, so it will attempt to close it again;
                     * we don't want this.
                     */
                    return;
                }
                if (creationStack != null) {
                    /**
                     * Warn SDK users that they should have closed the
                     * connection.
                     */
                    log.warn(creationStack.getMessage(), creationStack);
                }
                log.debug("Shutdown: close TFSConnection."); //$NON-NLS-1$
                close();
                log.debug("Shutdown: TFSCconnection closed."); //$NON-NLS-1$
            }
        };
        ShutdownManager.getInstance().addShutdownEventListener(shutdownEventListener, Priority.MIDDLE);

        /* Preview expiration enforcement */
        // checkPreviewExpiration();
    }

    /**
     * Checks to see if the environment is Eclipse or CommandLineClient
     */
    private static boolean isSDK() {
        return ProductInformation.getCurrent() == ProductName.SDK;
    }

    /**
     * The session ID is a unique {@link GUID} that is created when
     * {@link TFSConnection} is constructed. It is useful only for identifying
     * {@link TFSConnection} instances and it may be sent to the TFS in HTTP
     * headers.
     *
     * @return session ID of this {@link TFSConnection} instance (never
     *         <code>null</code>)
     */
    public GUID getSessionID() {
        checkNotClosed();
        return sessionId;
    }

    /**
     * Gets the catalog identifier from this connection's
     * {@link ServerDataProvider}.
     *
     * @return the {@link GUID} used to identify the service this
     *         {@link TFSConnection} is connected to in the TFS catalog (never
     *         <code>null</code>)
     */
    public GUID getCatalogResourceID() {
        checkNotClosed();
        return getServerDataProvider().getCatalogResourceID();
    }

    /**
     * @return the {@link CatalogNode} for the service this
     *         {@link TFSConnection} is connected to. Will be <code>null</code>
     *         if no catalog service is present on the server.
     */
    public CatalogNode getCatalogNode() {
        checkNotClosed();
        synchronized (catalogNodeLock) {
            /*
             * If the catalog resource ID is empty, we don't have a catalog
             * entry, so no node.
             */
            if (catalogNode == null && getCatalogResourceID() != GUID.EMPTY) {
                final ICatalogService catalogService = getCatalogService();

                final CatalogResource[] resources = catalogService.queryResources(new GUID[] {
                    getCatalogResourceID()
                }, CatalogQueryOptions.NONE);

                if (resources.length > 1) {
                    log.error(MessageFormat.format(
                        "Found multiple catalog resources with the ID {0}", //$NON-NLS-1$
                        getCatalogResourceID()));
                } else {
                    final CatalogNode[] references = resources[0].getNodeReferences();

                    for (int i = 0; i < references.length; i++) {
                        final CatalogNode node = references[i];

                        if (node.getFullPath() != null
                            && node.getFullPath().startsWith(CatalogRoots.ORGANIZATIONAL_PATH)) {
                            catalogNode = node;
                        }
                    }
                }

                if (catalogNode == null) {
                    log.error("Found a catalog resource ID but no node under the organizational tree"); //$NON-NLS-1$
                }
            }

            return catalogNode;
        }
    }

    /**
     * Gets the {@link ICatalogService} for this connection. Derived classes
     * must implement this to support {@link #getCatalogNode()}.
     */
    protected abstract ICatalogService getCatalogService();

    /**
     * Gets the instance identifier from this connection's
     * {@link ServerDataProvider}.
     *
     * @return the {@link GUID} used to identify the TFS instance
     */
    public GUID getInstanceID() {
        checkNotClosed();
        return getServerDataProvider().getInstanceID();
    }

    /**
     * Gets the {@link ServerDataProvider} for this connection.
     * <p>
     * Service discovery is often a slow process. Derived classes should cache
     * the result of the first call to ensure subsequent calls to this method
     * finish quickly.
     *
     * @return the {@link ServerDataProvider}, which provides information about
     *         the server this {@link TFSConnection} is connected to (never
     *         <code>null</code>)
     */
    protected abstract ServerDataProvider getServerDataProvider();

    /**
     * Tests whether the connection has authenticated. This is left for derived
     * classes to implement so they can use their knowledge of whether the
     * {@link ServerDataProvider} has been initialized to make the test more
     * efficient.
     *
     * @return <code>true</code> if this connection has authenticated to the
     *         server, <code>false</code> if it has not
     */
    public abstract boolean hasAuthenticated();

    protected void checkNotClosed() {
        if (connectionClosed) {
            throw new TFSConnectionClosedException();
        }
    }

    protected void warnClosed() {
        final TFSConnectionClosedException stack = new TFSConnectionClosedException();
        log.warn(stack.getMessage(), stack);
    }

    /**
     * Returns <code>true</code> if this {@link TFSConnection} has been closed.
     * See {@link #close()}.
     *
     * This method may be called on a closed {@link TFSConnection}.
     *
     * @return
     */
    public boolean isClosed() {
        return connectionClosed;
    }

    /**
     * <p>
     * This {@link Closable} interface method must be called when this
     * {@link TFSConnection} instance is no longer needed. This method cleans up
     * resources associated with this instance. After calling this method, this
     * instance should be discarded.
     * </p>
     *
     * <p>
     * After calling {@link #close()}, no methods on a {@link TFSConnection}
     * instance should be subsequently called. The {@link #close()} method
     * should only be called once; subsequent calls to {@link #close()} will log
     * a warning and do nothing. See {@link #isClosed()}. Subsequent calls to
     * other methods will throw {@link TFSConnectionClosedException}s.
     * </p>
     *
     * This method <em>should not</em> be called on a closed
     * {@link TFSConnection} .
     *
     */
    @Override
    public void close() {
        if (isClosed()) {
            warnClosed();
            return;
        }

        log.debug("closing connection."); //$NON-NLS-1$

        log.debug("Removing connection's shutdown listener from the ShutdownManager."); //$NON-NLS-1$
        ShutdownManager.getInstance().removeShutdownEventListener(shutdownEventListener, Priority.MIDDLE);

        /*
         * Clean up the clients - for each client instance, see if it implements
         * Closable.
         */
        synchronized (clientsLock) {
            log.debug("Close all clients."); //$NON-NLS-1$
            for (final Iterator<Object> it = clients.values().iterator(); it.hasNext();) {
                final Object obj = it.next();
                if (obj instanceof Closable) {
                    ((Closable) obj).close();
                }
            }
            clients.clear();
        }

        /*
         * Clean up the WSS clients in the same way.
         */
        synchronized (wssClientsLock) {
            log.debug("Close all SharePoint clients."); //$NON-NLS-1$
            for (final Iterator<WSSClient> it = wssClients.values().iterator(); it.hasNext();) {
                final Object obj = it.next();
                if (obj instanceof Closable) {
                    ((Closable) obj).close();
                }
            }
            wssClients.clear();
        }

        /*
         * Dispose of any resources associated with the http client (if this is
         * the last {@link TFSConnection} using this {@link HttpClient}.)
         */
        synchronized (httpClientLock) {
            log.debug("Close HTTP client."); //$NON-NLS-1$
            if (httpClientReference != null) {
                if (httpClientReference.decrementUseCount() == 0) {
                    log.debug("Disposing HTTP factory and client for the TFS connection."); //$NON-NLS-1$
                    /*
                     * Dispose the connection using the factory that originally
                     * created it.
                     */
                    httpClientReference.getFactory().dispose(httpClientReference.getHTTPClient());
                    httpClientReference = null;

                    /*
                     * Ensure this client's factory is set to null, even if it
                     * wasn't the one used by the reference (perhaps a client
                     * reference was set on this class after this class built a
                     * factory).
                     */
                    httpClientFactory = null;

                    log.debug("Has disposed HTTP factory and client for the TFS connection."); //$NON-NLS-1$
                } else {
                    log.debug("httpClientReference.useCount > 0"); //$NON-NLS-1$
                }
            } else {
                log.warn("httpClientReference is null"); //$NON-NLS-1$
            }
        }

        /*
         * Dispose of any resources associated with the TFS proxy server
         * settings.
         */
        synchronized (tfProxyServerSettingsLock) {
            if (needToDisposeTfProxyServerSettings) {
                tfProxyServerSettingsFactory.dispose(tfProxyServerSettings);
            }
            tfProxyServerSettings = null;
            tfProxyServerSettingsFactory = null;
        }

        connectionClosed = true;
    }

    /**
     * Returns the identity that authenticated with the server. When using TFS
     * Impersonation this will be the identity doing the impersonation, not the
     * user the call is being made on behalf of. Because of this,
     * {@link #getAuthorizedIdentity()} is usually the correct identity to use
     * from this API.
     *
     * TEE does not currently support impersonation, so the identies for
     * authorized vs. authenticated should always be the same.
     */
    public TeamFoundationIdentity getAuthenticatedIdentity() {
        checkNotClosed();
        return getServerDataProvider().getAuthenticatedIdentity();
    }

    /**
     * The identity who the calls to the server are being made for.
     *
     * TEE does not currently support impersonation, so the identies for
     * authorized vs. authenticated should always be the same.
     */
    public TeamFoundationIdentity getAuthorizedIdentity() {
        checkNotClosed();
        return getServerDataProvider().getAuthorizedIdentity();
    }

    /**
     * Gets the authorized user name in the form of a {@link TFSUser} object,
     * which includes the account name and domain. If this {@link TFSConnection}
     * has not already authorized, an authorization is done to retrieve the
     * authorized user name.
     *
     * @return the authorized user name as a {@link TFSUser}
     */
    public TFSUser getAuthorizedTFSUser() {
        checkNotClosed();
        final TeamFoundationIdentity identity = getAuthorizedIdentity();

        try {
            return new TFSUser(
                identity.getAttribute(IdentityAttributeTags.ACCOUNT_NAME, null),
                identity.getAttribute(IdentityAttributeTags.DOMAIN, null));
        } catch (final TFSUsernameParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the authorized identity's account name, which does not include a
     * domain name. If the authorized user's full domain identity is
     * "MYDOMAIN\\myuser" (single backslash), then the account name is just
     * "myuser". If this {@link TFSConnection} has not already authenticated the
     * user, an authenticated is done to retrieve the authorized user name.
     *
     * @return the authorized user's account name
     */
    public String getAuthorizedAccountName() {
        checkNotClosed();
        return getAuthorizedIdentity().getAttribute(IdentityAttributeTags.ACCOUNT_NAME, null);
    }

    /**
     * Gets the authorized identity's domain name, which does not include an
     * account name. If the authorized user's full domain identity is
     * "MYDOMAIN\\myuser" (single backslash), then the domain name is just
     * "MYDOMAIN". If this {@link TFSConnection} has not already authenticated
     * the user, an authenticated is done to retrieve the authorized domain
     * name.
     *
     * @return the authorized user's domain
     */
    public String getAuthorizedDomainName() {
        checkNotClosed();
        return getAuthorizedIdentity().getAttribute(IdentityAttributeTags.DOMAIN, null);
    }

    /**
     * This method will authenticate the connection if not previously
     * authenticated.
     */
    public void ensureAuthenticated() {
        checkNotClosed();
        getServerDataProvider().ensureAuthenticated();
    }

    /**
     * This method actually authenticates with the server, using the specified
     * provider to get credentials if necessary.
     */
    public void authenticate() {
        checkNotClosed();
        getServerDataProvider().authenticate();
    }

    /**
     * Performs all of the steps that are necessary for setting up a connection
     * with a TeamFoundationServer. Specify what information should be returned
     * in the connectOptions parameter. </summary>
     * <param name="connectOptions">Specifies what information that should be
     * returned from the server.</param> <param name="serviceTypeFilters">The
     * service types to get information for. If service information is not
     * requested in the connectOptions value, this parameter must be null. To
     * request all service definitions, pass "*" as the first and only entry in
     * the array. To only request service information about the possible client
     * zones, pass in null or an empty array.
     */
    public void connect(final ConnectOptions connectOptions) {
        checkNotClosed();
        getServerDataProvider().connect(connectOptions);
    }

    /**
     * Called to obtain the {@link URI} that a web service is connected to. The
     * specified web service must have been previously returned from a call to
     * {@link #getWebService(Class)}, {@link #getWSSWebService(ProjectInfo)}, or
     * {@link #getLinkingWebService(String)}.
     *
     * @param webService
     *        the web service instance (not type) to get the {@link URI} for
     *        (must not be <code>null</code>)
     * @return the {@link URI} the web service is connected to (must not be
     *         <code>null</code>)
     */
    public URI getWebServiceURI(final Object webService) {
        checkNotClosed();
        try {
            return getWebServiceFactory().getWebServiceURI(webService);
        } catch (final URISyntaxException e) {
            throw new IllegalConfigurationException(
                MessageFormat.format(
                    "uri syntax error getting web service uri for: {0}", //$NON-NLS-1$
                    webService));
        }
    }

    /**
     * Obtains a new web service from this {@link TFSConnection}. Web service
     * instances are not cached - every call to this method will return a new
     * instance. This method can only create a few kinds of services, the basic
     * internal TFS services. Classes which extend {@link TFSConnection} should
     * handle the more specialized types of services.
     *
     * @param webServiceType
     *        the web service type (must not be <code>null</code>)
     * @return a web service of the requested type or <code>null</code> if the
     *         web service type is known but no endpoint is appropriate for the
     *         current connection (for example, old server which doesn't support
     *         the requested service)
     * @throws UnknownWebServiceException
     *         if the kind of web service is not known or can't be created
     */
    public Object getWebService(final Class<?> webServiceType) throws UnknownWebServiceException {
        checkNotClosed();
        Check.notNull(webServiceType, "webServiceType"); //$NON-NLS-1$

        /*
         * Check for incompatible service types.
         */

        if (_ListsSoap.class.equals(webServiceType)) {
            throw new IllegalArgumentException("To get a _ListsSoap web service, call getWssWebService()"); //$NON-NLS-1$
        }

        if (_IntegrationServiceSoap.class.equals(webServiceType)) {
            throw new IllegalArgumentException("To get a _LinkingService web service, call getLinkingWebService()"); //$NON-NLS-1$
        }

        final WebServiceFactory factory = getWebServiceFactory();
        Object webService;

        /*
         * Registration services require a special construction call, because of
         * how the factory is designed.
         */
        if (_RegistrationSoap.class.equals(webServiceType)) {
            try {
                webService = factory.newRegistrationWebService(getBaseURI(), getHTTPClient());
            } catch (final URISyntaxException e) {
                throw new IllegalConfigurationException(
                    "uri syntax error when creating the registration web service", //$NON-NLS-1$
                    e);
            }

            if (webService == null) {
                throw new IllegalConfigurationException(
                    "WebServiceFactory returned null from newRegistrationWebService()"); //$NON-NLS-1$
            }
        } else if (_LocationWebServiceSoap.class.equals(webServiceType)) {
            try {
                webService = factory.newLocationWebService(getLocationServiceURI(), getHTTPClient());
            } catch (final URISyntaxException e) {
                throw new IllegalConfigurationException("uri syntax error when creating the location web service", e); //$NON-NLS-1$
            }

            if (webService == null) {
                throw new IllegalConfigurationException("WebServiceFactory returned null from newLocationWebService()"); //$NON-NLS-1$
            }
        } else {
            try {
                webService = factory.newWebService(
                    this,
                    webServiceType,
                    getBaseURI(),
                    getHTTPClient(),
                    getServerDataProvider(),
                    getRegistrationClient());
            } catch (final URISyntaxException e) {
                throw new IllegalConfigurationException(
                    MessageFormat.format(
                        "uri syntax error when creating the [{0}] web service", //$NON-NLS-1$
                        webServiceType.getName()),
                    e);
            }
        }

        /*
         * Add a request handler that will listen for connection failures and
         * update our failure state accordingly.
         */
        if (webService != null && webService instanceof SOAPService) {
            ((SOAPService) webService).addTransportRequestHandler(new ConnectivityFailureRequestHandler(this));
        }

        return webService;
    }

    /**
     * Obtains a new sharepoint web service from this {@link TFSConnection}.
     * Sharepoint web services are not cached - every call to this method
     * returns a new instance.
     *
     * @param projectInfo
     *        the team project to get a sharepoint web service for
     * @return the sharepoint web service (never <code>null</code>)
     */
    public _ListsSoap getWSSWebService(final ProjectInfo projectInfo) {
        checkNotClosed();
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$

        final WebServiceFactory factory = getWebServiceFactory();

        _ListsSoap webService;
        try {
            webService =
                factory.newWSSWebService(this, projectInfo, getBaseURI(), getHTTPClient(), getRegistrationClient());
        } catch (final URISyntaxException e) {
            throw new IllegalConfigurationException("uri syntax error when creating the WSS web service", e); //$NON-NLS-1$
        }

        if (webService == null) {
            throw new IllegalConfigurationException("WebServiceFactory returned null from newWssWebService"); //$NON-NLS-1$
        }

        return webService;
    }

    /**
     * Obtains a new linking web service from this {@link TFSConnection}.
     * Linking web services are not cached - every call to this method returns a
     * new instance.
     *
     * @param endpoint
     *        the endpoint for the linking web service to use (must not be
     *        <code>null</code>)
     * @return the linking web service (never <code>null</code>)
     */
    public _IntegrationServiceSoap getLinkingWebService(final String endpoint) {
        checkNotClosed();
        Check.notNull(endpoint, "endpoint"); //$NON-NLS-1$

        final WebServiceFactory factory = getWebServiceFactory();

        _IntegrationServiceSoap webService;
        try {
            webService = factory.newLinkingWebService(
                this,
                URLEncode.encode(endpoint),
                getBaseURI(),
                getHTTPClient(),
                getRegistrationClient());
        } catch (final URISyntaxException e) {
            throw new IllegalConfigurationException("uri syntax error when creating the linking web service", e); //$NON-NLS-1$
        }

        if (webService == null) {
            throw new IllegalConfigurationException("WebServiceFactory returned null from newLinkingWebService"); //$NON-NLS-1$
        }

        return webService;
    }

    /**
     * Sets the {@link HTTPClientReference} for this {@link TFSConnection}, and
     * increments the use count by one. Subsequent calls to
     * {@link #getHTTPClientReference()} and {@link #getHTTPClient()} will
     * return this client.
     *
     * @param httpClientReference
     *        the {@link HTTPClientReference} instance to return (or
     *        <code>null</code>) to generate a new one.
     */
    protected void setHTTPClientReference(final HTTPClientReference httpClientReference) {
        checkNotClosed();
        /*
         * This is sort of an obnoxious hack - we need to hold a single
         * HttpClient across multiple TFSConnection objects to handle
         * authentication. This can be undone if we ever have improved
         * authentication handling.
         */
        synchronized (httpClientLock) {
            this.httpClientReference = httpClientReference;
        }

        if (httpClientReference != null) {
            httpClientReference.incrementUseCount();
        }
    }

    /**
     * Gets the {@link HTTPClientReference} instance from this
     * {@link TFSConnection}. If the {@link HTTPClientReference} instance has
     * not yet been created, returns <code>null</code>.
     *
     * @return the {@link HTTPClientReference} instance or <code>null</code>.
     */
    protected HTTPClientReference getHTTPClientReference() {
        checkNotClosed();
        synchronized (httpClientLock) {
            return httpClientReference;
        }
    }

    /**
     * Gets the {@link HttpClient} instance from this {@link TFSConnection}. If
     * the {@link HttpClient} instance has not yet been created, creates it. The
     * {@link HttpClient} instance is cached by this {@link TFSConnection}.
     *
     * @return the {@link HttpClient} instance (never <code>null</code>)
     */
    public HttpClient getHTTPClient() {
        checkNotClosed();
        synchronized (httpClientLock) {
            if (httpClientReference != null) {
                return httpClientReference.getHTTPClient();
            }

            httpClientFactory = advisor.getHTTPClientFactory(instanceData);

            if (httpClientFactory == null) {
                throw new IllegalStateException(
                    MessageFormat.format(
                        "TFSConnectionSettings [{0}] provided a null HttpClientFactory", //$NON-NLS-1$
                        advisor.getClass().getName()));
            }

            final HttpClient httpClient = httpClientFactory.newHTTPClient();

            if (httpClient == null) {
                throw new IllegalStateException(
                    MessageFormat.format(
                        "HttpClientFactory [{0}] provided a null HttpClient", //$NON-NLS-1$
                        httpClientFactory.getClass().getName()));
            }

            httpClientReference = new HTTPClientReference(httpClient, httpClientFactory);
            return httpClient;
        }
    }

    /**
     * Gets a new {@link HttpClient} whose configuration (SSL restrictions,
     * proxy settings, credentials, etc.) are similar to those in use by this
     * {@link TFSConnection}'s {@link HttpClient}. The target URI is required so
     * the correct proxy settings for the scheme and host can be used.
     *
     * @param proxyScopeURI
     *        the URI to use to scope proxy settings for (maybe to any type of
     *        resource; really only the scheme, host, and port will matter)
     *        (must not be <code>null</code> )
     * @return a new {@link HttpClient} which the caller can modify
     */
    public HttpClient newHTTPClient(final URI proxyScopeURI) {
        checkNotClosed();
        Check.notNull(proxyScopeURI, "proxySettingsURI"); //$NON-NLS-1$

        /*
         * Create a new connection instance data for the proxy server.
         */
        final ConnectionInstanceData proxyInstanceData = new ConnectionInstanceData(proxyScopeURI, GUID.newGUID());

        return getConnectionAdvisor().getHTTPClientFactory(proxyInstanceData).newHTTPClient();
    }

    /**
     * Gets the TF proxy server settings from this {@link TFSConnection}. For a
     * given {@link TFSConnection}, this method will always return the same
     * {@link TFProxyServerSettings} object (but that object's state may change
     * in between calls).
     *
     * @return the {@link TFProxyServerSettings} for this {@link TFSConnection}
     *         (never <code>null</code>)
     */
    public TFProxyServerSettings getTFProxyServerSettings() {
        checkNotClosed();
        synchronized (tfProxyServerSettingsLock) {
            if (tfProxyServerSettings != null) {
                return tfProxyServerSettings;
            }

            tfProxyServerSettingsFactory = advisor.getTFProxyServerSettingsFactory(instanceData);

            if (tfProxyServerSettingsFactory != null) {
                tfProxyServerSettings = tfProxyServerSettingsFactory.newProxyServerSettings();
            }

            needToDisposeTfProxyServerSettings = (tfProxyServerSettings != null);

            if (tfProxyServerSettings == null) {
                tfProxyServerSettings = new DefaultTFProxyServerSettings(null);
            }

            return tfProxyServerSettings;
        }
    }

    /**
     * Obtains the base TF server URL that this {@link TFSConnection} connects
     * to (for example, <code>http://server.mycompany.com:8080/</code>). The
     * returned {@link URL} is guaranteed to have a path component that ends in
     * a slash (<code>/</code>) character.
     *
     * @return the TF server {@link URL} (never <code>null</code>)
     * @deprecated use {@link #getBaseURI()} instead
     */
    @Deprecated
    public URL getURL() {
        checkNotClosed();
        try {
            return getBaseURI().toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a string suitable for user display which identifies this TFS
     * connection. Currently this is the base URI ({@link #getBaseURI()}) with
     * escaped characters (including spaces and non-ASCII chars) fully decoded
     * into Unicode.
     */
    public String getName() {
        checkNotClosed();
        synchronized (displayNameLock) {
            if (displayName == null) {
                /*
                 * The base URI will have non-ASCII characters encoded, and
                 * these can be common in a team project collection name. Decode
                 * them into Unicode before displaying.
                 */
                displayName = URIUtils.decodeForDisplay(getBaseURI());
            }

            return displayName;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (isClosed()) {
            return super.toString();
        }
        return getName();
    }

    /**
     * Obtains the base Team Foundation Server {@link URI} that this
     * {@link TFSConnection} connects to (for example,
     * <code>http://server.mycompany.com:8080/</code> for TFS 2005-2008, posibly
     * more like
     * <code>http://server.mycompany.com:8080/tfs/TeamProjectCollection</code>
     * for TFS 2010). The returned {@link URI} is guaranteed to have a path
     * component that ends in a slash ( <code>/</code>) character.
     *
     * @return the base Team Foundation Server {@link URI} for this connection
     *         (never <code>null</code>)
     */
    public URI getBaseURI() {
        checkNotClosed();
        synchronized (baseURILock) {
            if (baseURI == null) {
                final ServerURIProvider provider = advisor.getServerURIProvider(instanceData);

                if (provider == null) {
                    throw new IllegalConfigurationException("ConnectionAdvisor returned a null ServerURIProvider"); //$NON-NLS-1$
                }

                try {
                    baseURI = provider.getServerURI();
                } catch (final URISyntaxException e) {
                    throw new IllegalConfigurationException("Unable to produce a server URI", e); //$NON-NLS-1$
                }

                if (baseURI == null) {
                    throw new IllegalConfigurationException("ServerURIProvider returned a null URI"); //$NON-NLS-1$
                }

                if (!baseURI.isAbsolute() || baseURI.isOpaque()) {
                    throw new IllegalConfigurationException(
                        MessageFormat.format(
                            "ServerURIProvider returned an invalid URI: [{0}]", //$NON-NLS-1$
                            baseURI.toString()));
                }

                /*
                 * Remove any location service relative path suffix if the URI
                 * contains one. This helps in the case where the Profile used
                 * to construct this TFSConnection was created programmatically
                 * (or maybe the user pasted in a full location service URI?),
                 * so the ServerURIProvider would return a non-base
                 * (too-specific) path.
                 */
                if (baseURI.toString().toLowerCase().endsWith(locationServiceRelativePath.toLowerCase())) {
                    baseURI = URI.create(
                        baseURI.toString().substring(
                            0,
                            baseURI.toString().length() - locationServiceRelativePath.length()));
                }

                baseURI = URIUtils.ensurePathHasTrailingSlash(baseURI);
            }
        }

        return baseURI;
    }

    public URI getLocationServiceURI() {
        checkNotClosed();
        synchronized (locationServiceURILock) {
            if (locationServiceURI == null) {
                /*
                 * Construct a new URI, combining the base and location service
                 * paths.
                 */
                locationServiceURI =
                    URI.create(URIUtils.combinePaths(getBaseURI().toString(), locationServiceRelativePath));
            }

            return locationServiceURI;
        }
    }

    /**
     * Obtains the {@link TimeZone} associated with this {@link TFSConnection}.
     *
     * @return this {@link TFSConnection}'s {@link TimeZone} (never
     *         <code>null</code>)
     */
    public TimeZone getTimeZone() {
        checkNotClosed();
        synchronized (timeZoneLock) {
            if (timeZone == null) {
                timeZone = advisor.getTimeZone(instanceData);
                Check.notNull(timeZone, "timeZone"); //$NON-NLS-1$
            }

            return timeZone;
        }
    }

    /**
     * Obtains the {@link Locale} associated with this {@link TFSConnection}.
     *
     * @return this {@link TFSConnection}'s {@link Locale} (never
     *         <code>null</code>)
     */
    public Locale getLocale() {
        checkNotClosed();
        synchronized (localeLock) {
            if (locale == null) {
                locale = advisor.getLocale(instanceData);
                Check.notNull(locale, "locale"); //$NON-NLS-1$
            }

            return locale;
        }
    }

    /**
     * Obtains a {@link Collator} for the connection's current {@link Locale}
     * that considers all character differences important.
     *
     * @return a {@link Collator} for this connection's {@link Locale} that
     *         considers all character differences important.
     *
     * @see CollatorFactory#getCaseSensitiveCollator(Locale)
     */
    public Collator getCaseSensitiveCollator() {
        checkNotClosed();
        synchronized (collatorLock) {
            if (caseSensitiveCollator == null) {
                caseSensitiveCollator = CollatorFactory.getCaseSensitiveCollator(getLocale());
            }

            return caseSensitiveCollator;
        }
    }

    /**
     * <p>
     * Obtains a {@link Collator} for the connection's current {@link Locale}
     * that considers character and accent differences important, but not case.
     * </p>
     *
     * @return a {@link Collator} for this connection's {@link Locale} that
     *         recognizes primary differences in characters and accents, but not
     *         case
     *
     * @see CollatorFactory#getCaseInsensitiveCollator(Locale)
     */
    public Collator getCaseInsensitiveCollator() {
        checkNotClosed();
        synchronized (collatorLock) {
            if (caseInsensitiveCollator == null) {
                caseInsensitiveCollator = CollatorFactory.getCaseInsensitiveCollator(getLocale());
            }

            return caseInsensitiveCollator;
        }
    }

    /**
     * The capabilities of the TFS server
     */
    public ServerCapabilities getServerCapabilities() {
        checkNotClosed();
        return getServerDataProvider().getServerCapabilities();
    }

    public boolean isHosted() {
        checkNotClosed();
        return getServerCapabilities().contains(ServerCapabilities.HOSTED);
    }

    /**
     * Obtains the {@link PersistenceStoreProvider} that determines where cache
     * and configuration data is stored.
     * <p>
     * Child stores may be created from this base store.
     * <p>
     *
     * @see ConnectionAdvisor#getPersistenceStoreProvider(ConnectionInstanceData)
     *
     * @return this {@link TFSConnection}'s {@link PersistenceStoreProvider}
     *         (never <code>null</code>)
     */
    public PersistenceStoreProvider getPersistenceStoreProvider() {
        checkNotClosed();
        synchronized (persistenceStoreProviderLock) {
            if (persistenceStoreProvider != null) {
                return persistenceStoreProvider;
            }

            persistenceStoreProvider = advisor.getPersistenceStoreProvider(instanceData);

            return persistenceStoreProvider;
        }
    }

    public Credentials getCredentials() {
        checkNotClosed();
        return instanceData.getCredentials();
    }

    public AtomicReference<Credentials> getCredentialsHolder() {
        checkNotClosed();
        return instanceData.getCredentialsHolder();
    }

    public ConnectionAdvisor getConnectionAdvisor() {
        checkNotClosed();
        return advisor;
    }

    protected WebServiceFactory getWebServiceFactory() {
        checkNotClosed();
        synchronized (webServiceFactoryLock) {
            if (webServiceFactory != null) {
                return webServiceFactory;
            }

            webServiceFactory = advisor.getWebServiceFactory(instanceData);

            if (webServiceFactory == null) {
                throw new IllegalConfigurationException("ConnectionAdvisor returned a null WebServiceFactory"); //$NON-NLS-1$
            }

            return webServiceFactory;
        }
    }

    private ClientFactory getClientFactory() {
        checkNotClosed();
        synchronized (clientFactoryLock) {
            if (clientFactory != null) {
                return clientFactory;
            }

            clientFactory = advisor.getClientFactory(instanceData);

            if (clientFactory == null) {
                throw new IllegalConfigurationException("ConnectionAdvisor returned a null ClientFactory"); //$NON-NLS-1$
            }

            return clientFactory;
        }
    }

    /**
     * Convenience method to get the registration client for this connection.
     * May return <code>null</code> if this kind of {@link TFSConnection}
     * doesn't support the registration service.
     *
     * @return the {@link RegistrationClient} (possibly <code>null</code>)
     */
    public abstract RegistrationClient getRegistrationClient();

    /**
     * Gets a client from this {@link TFSConnection}. Clients are cached (there
     * will be at most one client created for each client type specified to this
     * method. To get a Windows Sharepoint Services client ({@link WSSClient})
     * do not call this method, call {@link #getWSSClient(ProjectInfo)} instead.
     *
     * @param clientType
     *        the type of client to get (must not be <code>null</code>)
     * @return a client of the requested type (never <code>null</code>)
     */
    public Object getClient(final Class<?> clientType) {
        checkNotClosed();
        Check.notNull(clientType, "clientType"); //$NON-NLS-1$

        /*
         * Sharepoint clients require a project name, so they have to be built
         * and cached differently.
         */
        if (WSSClient.class.equals(clientType)) {
            throw new IllegalArgumentException(MessageFormat.format(
                "To get a {0}, call getWssClient()", //$NON-NLS-1$
                WSSClient.class.getName()));
        }

        synchronized (clientsLock) {
            Object client = clients.get(clientType);

            if (client != null) {
                return client;
            }

            final ClientFactory factory = getClientFactory();

            client = factory.newClient(clientType, this);

            if (client == null) {
                throw new UnknownClientException(clientType);
            }

            clients.put(clientType, client);
            return client;
        }
    }

    /**
     * Gets a Windows Sharepoint Services client. Sharepoint clients are cached
     * (there will be at most one client created for each project name specified
     * to this method).
     *
     * @param projectInfo
     *        the team project info to get a Windows Sharepoint Services client
     *        for (must not be <code>null</code>)
     * @return a Windows Sharepoint Services client for the requested team
     *         project name (never <code>null</code>)
     */
    public WSSClient getWSSClient(final ProjectInfo projectInfo) {
        checkNotClosed();
        Check.isTrue(this instanceof TFSTeamProjectCollection, "connection is TeamProjectCollection"); //$NON-NLS-1$
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$

        synchronized (wssClientsLock) {
            WSSClient client = wssClients.get(projectInfo.getGUID());

            if (client != null) {
                return client;
            }

            final ClientFactory factory = getClientFactory();

            client = factory.newWSSClient((TFSTeamProjectCollection) this, projectInfo);

            if (client == null) {
                throw new IllegalConfigurationException(
                    MessageFormat.format(
                        "ClientFactory returned a null WssClient for project name [{0}]", //$NON-NLS-1$
                        projectInfo.getName()));
            }

            wssClients.put(projectInfo.getGUID(), client);
            return client;
        }
    }

    public void setConnectivityFailureOnLastWebServiceCall(final boolean failure) {
        final boolean lastFailure = connectivityFailureOnLastWebServiceCall.getAndSet(failure);

        if (lastFailure != failure) {
            ((ConnectivityFailureStatusChangeListener) connectivityFailureListeners.getListener()).onConnectivityFailureStatusChange();
        }
    }

    public boolean getConnectivityFailureOnLastWebServiceCall() {
        return connectivityFailureOnLastWebServiceCall.get();
    }

    public void addConnectivityFailureStatusChangeListener(final ConnectivityFailureStatusChangeListener listener) {
        connectivityFailureListeners.addListener(listener);
    }

    public void removeConnectivityFailureStatusChangeListener(final ConnectivityFailureStatusChangeListener listener) {
        connectivityFailureListeners.removeListener(listener);
    }

    protected static final class HTTPClientReference {
        private final HttpClient httpClient;
        private final HTTPClientFactory factory;

        private final Object lock = new Object();
        private int useCount = 1;

        public HTTPClientReference(final HttpClient httpClient, final HTTPClientFactory factory) {
            Check.notNull(httpClient, "httpClient"); //$NON-NLS-1$
            Check.notNull(factory, "factory"); //$NON-NLS-1$

            this.httpClient = httpClient;
            this.factory = factory;
        }

        public HttpClient getHTTPClient() {
            return httpClient;
        }

        public HTTPClientFactory getFactory() {
            return factory;
        }

        public int incrementUseCount() {
            synchronized (lock) {
                useCount++;

                return useCount;
            }
        }

        public int decrementUseCount() {
            synchronized (lock) {
                useCount--;

                return useCount;
            }
        }
    }
}
