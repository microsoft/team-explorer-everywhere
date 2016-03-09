// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFProxyServerSettings;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.checkinpolicies.PolicyAnnotation;
import com.microsoft.tfs.core.checkinpolicies.PolicyDefinition;
import com.microsoft.tfs.core.checkinpolicies.PolicySerializationException;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.security.AccessControlEntry;
import com.microsoft.tfs.core.clients.security.AccessControlEntryDetails;
import com.microsoft.tfs.core.clients.security.AccessControlListDetails;
import com.microsoft.tfs.core.clients.security.ISecurityService;
import com.microsoft.tfs.core.clients.security.SecurityNamespace;
import com.microsoft.tfs.core.clients.security.SecurityService;
import com.microsoft.tfs.core.clients.versioncontrol.engines.MergeEngine;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.GetEngine;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.OutputStreamDownloadOutput;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.TempDownloadWorker;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus;
import com.microsoft.tfs.core.clients.versioncontrol.events.BranchCommittedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.BranchObjectUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.DestroyEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.DownloadProxyException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.FeatureNotSupportedException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ItemNotMappedException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.OnlyOneWorkspaceException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.WorkspaceNotFoundException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.HistoryIterator;
import com.microsoft.tfs.core.clients.versioncontrol.internal.WebServiceLayer;
import com.microsoft.tfs.core.clients.versioncontrol.internal.WebServiceLayerLocalWorkspaces;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.BoundedExecutor;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineRequest;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalDataAccessLayer;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalPendingChangesTable;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalVersionPendingChangesTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceProperties;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.NullPathWatcherFactory;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.PendingChangesTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLock;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspacePropertiesTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceVersionTable;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherFactory;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.Wildcard;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Annotation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchProperties;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMerge;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMergeDetails;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedMerge;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.FileType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelChildOption;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Mapping;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.MergeCandidate;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.SecurityChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ServerSettings;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkspaceItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadOutput;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.InternalServerInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.RuntimeWorkspaceCache;
import com.microsoft.tfs.core.clients.webservices.IdentityConstants;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.core.clients.workitem.files.FileAttachmentDownloadException;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpMethodBase;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.core.httpclient.methods.PostMethod;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.util.notifications.Notification;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Closable;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.TappedInputStream;
import com.microsoft.tfs.util.TappedInputStream.ReadHandler;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;
import com.microsoft.tfs.util.temp.TempStorageService;

import ms.tfs.versioncontrol.clientservices._03._Repository4Soap;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap;

/**
 * <p>
 * Performs client tasks for source control, including workspace management,
 * item status, history, labels, branching, etc., communicating with the Team
 * Foundation Server when necessary.
 * </p>
 * <p>
 * This class publishes several events. To register for event notification, get
 * the {@link VersionControlEventEngine} from your instance using
 * {@link VersionControlClient#getEventEngine()}, then add listeners to it.
 * </p>
 * <p>
 * Don't create an instance of this class directly, instead use
 * {@link TFSTeamProjectCollection#getVersionControlClient()}.
 * </p>
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public final class VersionControlClient implements Closable {
    /*
     * Thread Safety Note
     *
     * Don't synchronize entire methods (or on "this"), instead synchronize on
     * dedicated lock objects for your field. This lets the class be more
     * re-entrant and avoids deadlocks where co-dependent features (workspace
     * cache and server GUID) would be waiting on the same class instance lock.
     * In this example, updating the server GUID does not have side effects that
     * could acquire another lock in this class, so it's best to have it use a
     * lock of its own.
     */

    private static final Log log = LogFactory.getLog(VersionControlClient.class);

    private static final String MAX_REQUEST_RETRY_PROPERTY = "com.microsoft.tfs.core.maxRequestRetry"; //$NON-NLS-1$
    private static final int MAX_REQUEST_RETRY_DEFAULT = 3;

    /**
     * The size of the download memory buffer, in bytes.
     */
    private static final int DOWNLOAD_BUFFER_SIZE = 32768;

    /**
     * GUID This is the set of namespaces that exist in the version control.
     */
    public static final GUID WORKSPACE_SECURITY_NAMESPACE_ID = new GUID("93BAFC04-9075-403a-9367-B7164EAC6B5C"); //$NON-NLS-1$

    /**
     * We fetch this many history items at a time during history queries. This
     * is the number Visual Studio's implementation uses.
     *
     * Do not make this number bigger! Ivestigation shows that the server will
     * never return more than this many items in one request, and our history
     * loop will exit too early if we get fewer results than this number in one
     * chunk.
     */
    private final static int HISTORY_ITEMS_CHUNK_SIZE = 256;

    /**
     * This is the default limit to the number of worker threads that will run
     * for this {@link VersionControlClient} to perform download and get
     * completion tasks concurrently.
     */
    private final static int DEFAULT_GET_ENGINE_WORKER_LIMIT = 8;

    /**
     * The number of seconds after which idle get engine worker threads are
     * stopped. Letting them hang around for a while lets them be re-used by
     * different (possibly unrelated) get operations initiated by UI layers.
     */
    private final static int DEFAULT_GET_ENGINE_WORKER_TIMEOUT_SECONDS = 20;

    // Final instance fields

    /*
     * These fields have an underscore prefix because they're just cached from
     * the constructor so we can build a WebServiceLayer object later. They're
     * not useful in this class otherwise.
     */
    private final _RepositorySoap _webService;
    private final _RepositoryExtensionsSoap _webServiceExtensions;
    private final _Repository4Soap _repository4;
    private final _Repository5Soap _repository5;

    /**
     * The base executor wrapped by {@link #uploadDownloadWorkerExecutor}, kept
     * for shutdown.
     */
    private final ThreadPoolExecutor threadPoolExecutor;

    /**
     * A bounded executor will dispatch runnables to background threads, but
     * only allows a given number to be run at once (blocking new submissions
     * until there is room). We use this for upload and download workers.
     */
    private final BoundedExecutor uploadDownloadWorkerExecutor;

    /**
     * Coordinates listeners and dispatches our events.
     */
    private final VersionControlEventEngine eventEngine = new VersionControlEventEngine();

    /**
     * The connection we were created from (initialized with).
     */
    private final TFSTeamProjectCollection connection;

    // Non-final instance fields (see thread safety note above)

    /**
     * A factory to create local workspace path watchers.
     * <p>
     * Don't access this field directly, use {@link #getPathWatcherFactory()}
     * instead.
     */
    private PathWatcherFactory pathWatcherFactory = new NullPathWatcherFactory();
    private final Object pathWatcherFactoryLock = new Object();

    /**
     * The GUID of the server we are using.
     * <p>
     * Don't access this field directly, use {@link #getServerGUID()} instead.
     */
    private GUID serverGUID;
    private final Object serverGUIDLock = new Object();

    /**
     * Facade over web service proxies of all versions.
     * <p>
     * Don't access this field directly, use {@link #getWebServiceLayer()}
     * instead.
     */
    private WebServiceLayer webServiceLayer;
    private final Object webServiceLayerLock = new Object();

    /**
     * A cache of features supported by the TFS server. Filled by
     * {@link #getServerSupportedFeatures()} and cached to reduce round-trips.
     * <p>
     * Don't access this field directly, use
     * {@link #getServerSupportedFeatures()} instead.
     */
    private SupportedFeatures serverSupportedFeatures;
    private final Object serverSupportedFeaturesLock = new Object();

    /**
     * In-memory cache of {@link Workspace} objects created by this
     * {@link VersionControlClient} for speedy re-use.
     * <p>
     * Don't access this field directly, use {@link #getRuntimeWorkspaceCache()}
     * instead.
     */
    private RuntimeWorkspaceCache runtimeWorkspaceCache;
    private final Object runtimeWorkspaceCacheLock = new Object();

    /**
     * Full URI for downloading files. Initialized lazily when files are
     * downloaded.
     * <p>
     * Don't access this field directly, use {@link #getDownloadURI()} instead.
     */
    private URI downloadFileURI;
    private final Object downloadFileURILock = new Object();

    /**
     * Full URI for uploading files. Initialized lazily when files are uploaded.
     * <p>
     * Don't access this field directly, use {@link #getUploadURI()} instead.
     */
    private URI uploadFileURI;
    private final Object uploadFileURILock = new Object();

    /**
     * In-memory cache of {@link FileType}s.
     * <p>
     * Don't access this field directly, use
     * {@link #queryCachedFileType(String)} instead.
     */
    private Map<String, FileType> cachedFileTypes;
    private final Object cachedFileTypesLock = new Object();

    /**
     * These property filters are automatically appended to any specified item
     * filters in methods that send item property filters to the server. The
     * main use for this fields is for properties like the Unix execute bit,
     * which clients would otherwise have to specify for method call.
     */
    private String[] defaultItemPropertyFilters;
    private final Object defaultItemPropertyFiltersLock = new Object();

    /**
     * Workspace level security namespace.
     * <p>
     * Don't access this field directly, use {@link #getWorkspaceSecurity()}
     * instead.
     */
    private SecurityNamespace workspaceSecurity;
    private final Object workspaceSecurityLock = new Object();

    /**
     * Current server settings
     * <p>
     * Don't access this field directly, use
     * {@link #getServerSettingsWithFallback()} instead.
     */
    private ServerSettings serverSettings;
    private final Object serverSettingsLock = new Object();

    /**
     * <p>
     * Creates a {@link VersionControlClient} that will use the given web
     * service proxy class and the given connection. The size of the download
     * worker thread pool and the thread idle timeouts can also be specified.
     * </p>
     * <p>
     * Generally you do not instantiate this class yourself. See
     * {@link TFSTeamProjectCollection#getClient(Class)}.
     * </p>
     *
     * @param connection
     *        the connection to use (must not be <code>null</code>)
     * @param webService
     *        the {@link _RepositorySoap} web service proxy to use (must not be
     *        <code>null</code>)
     * @param webServiceExtensions
     *        the {@link _RepositoryExtensionsSoap} proxy to use for TFS 2010
     *        features (may be null)
     * @param repository4
     *        the {@link _Repository4Soap} proxy to use for TFS 2012 features
     *        (may be null)
     * @param repository5
     *        the {@link _Repository5Soap} proxy to use for TFS 2012 QU1
     *        features (may be null)
     * @param maximumGetEngineWorkerThreads
     *        the maximum number of simultaneous worker threads started to
     *        process get operations. This controls the maximum number of
     *        threads that could be performing a file download in parallel,
     *        though these worker threads perform non-network work that may
     *        prevent the workers reaching maximum theoretical network
     *        parallelism. Must be > 0.
     * @param getEngineWorkerThreadIdleTimeoutSeconds
     *        the number of seconds of consecutive idle time after which a get
     *        engine worker thread stops running. Choose a number that provides
     *        for some thread re-use between calls that process get operations
     *        (so threads can be reused from previous operations). Choosing a
     *        very large number will result in idle threads hanging around long
     *        after their last get operation, possibly consuming resources when
     *        it is unlikely there will be any get-related work for them to
     *        perform in the near future. Must be >= 0.
     */
    protected VersionControlClient(
        final TFSTeamProjectCollection connection,
        final _RepositorySoap webService,
        final _RepositoryExtensionsSoap webServiceExtensions,
        final _Repository4Soap repository4,
        final _Repository5Soap repository5,
        final int maximumGetEngineWorkerThreads,
        final int getEngineWorkerThreadIdleTimeoutSeconds) {
        /*
         * TODO Make these parameters available via some {@link
         * TFSTeamProjectCollection} configuration mechanism.
         */

        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.isTrue(maximumGetEngineWorkerThreads > 0, "GetEngine worker threads must be > 0"); //$NON-NLS-1$
        Check.isTrue(getEngineWorkerThreadIdleTimeoutSeconds >= 0, "GetEngine worker timeout must be >= 0"); //$NON-NLS-1$
        Check.notNull(webService, "webService"); //$NON-NLS-1$

        this.connection = connection;
        this._webService = webService;
        this._webServiceExtensions = webServiceExtensions;
        this._repository4 = repository4;
        this._repository5 = repository5;

        /*
         * Look up the GUID of this VersionControlClient in the local workspace
         * cache. If it's not there, fetch it from the server.
         */
        final InternalServerInfo serverInfo =
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).getCache().getServerInfoByURI(
                connection.getBaseURI());
        if (null == serverInfo) {
            serverGUID = connection.getInstanceID();
        } else {
            serverGUID = serverInfo.getServerGUID();
        }

        /**
         * The default get engine worker pool. The actual Executor we use should
         * be unbounded becausethe BoundedExecutor handles the submission
         * limiting.
         */
        threadPoolExecutor = new ThreadPoolExecutor(
            0,
            Integer.MAX_VALUE,
            getEngineWorkerThreadIdleTimeoutSeconds,
            TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());

        uploadDownloadWorkerExecutor = new BoundedExecutor(threadPoolExecutor, maximumGetEngineWorkerThreads);

        /*
         * Configure default property filters for the Unix execute bit.
         */
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)
            && getWebServiceLayer().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
            setDefaultItemPropertyFilters(new String[] {
                PropertyConstants.EXECUTABLE_KEY,
                PropertyConstants.SYMBOLIC_KEY
            });
        }

        // Start listening for Workstation events
        Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).addWorkstationEventListener(this);
    }

    /**
     * Creates a {@link VersionControlClient} that will use the given web
     * service proxy class and the given connection. This client will use the
     * default download worker thread pool size and timeout.
     *
     * Generally you do not instantiate this class yourself. See
     * {@link TFSTeamProjectCollection#getClient(Class)}.
     *
     * @param connection
     *        the connection to use (must not be <code>null</code>)
     * @param repository
     *        the {@link _RepositorySoap} web service proxy to use (must not be
     *        <code>null</code>)
     * @param repositoryExtensions
     *        the {@link _RepositoryExtensionsSoap} proxy to use for TFS 2010
     *        features (may be null)
     * @param repository4
     *        the {@link _Repository4Soap} proxy to use for TFS 2012 features
     *        (may be null)
     */
    public VersionControlClient(
        final TFSTeamProjectCollection connection,
        final _RepositorySoap repository,
        final _RepositoryExtensionsSoap repositoryExtensions,
        final _Repository4Soap repository4,
        final _Repository5Soap repository5) {
        this(
            connection,
            repository,
            repositoryExtensions,
            repository4,
            repository5,
            DEFAULT_GET_ENGINE_WORKER_LIMIT,
            DEFAULT_GET_ENGINE_WORKER_TIMEOUT_SECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // Stop listening for Workstation events
        Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).removeWorkstationEventListener(this);

        threadPoolExecutor.shutdown();
    }

    /**
     * @return the web service proxy layer (never <code>null</code>)
     */
    public WebServiceLayer getWebServiceLayer() {
        synchronized (webServiceLayerLock) {
            if (webServiceLayer == null) {
                webServiceLayer = new WebServiceLayerLocalWorkspaces(
                    this,
                    _webService,
                    _webServiceExtensions,
                    _repository4,
                    _repository5);
            }

            return webServiceLayer;
        }
    }

    /**
     * This method is for internal use only.
     *
     * @return the {@link RuntimeWorkspaceCache} (never <code>null</code>)
     */
    public RuntimeWorkspaceCache getRuntimeWorkspaceCache() {
        synchronized (runtimeWorkspaceCacheLock) {
            if (runtimeWorkspaceCache == null) {
                runtimeWorkspaceCache = new RuntimeWorkspaceCache(this);
            }

            return runtimeWorkspaceCache;
        }
    }

    /**
     * Gets the factory used to construct {@link PathWatcher}s for local
     * workspaces.
     */
    public PathWatcherFactory getPathWatcherFactory() {
        synchronized (pathWatcherFactoryLock) {
            return pathWatcherFactory;
        }
    }

    /**
     * Sets the factory used to construct {@link PathWatcher}s for local
     * workspaces.
     */
    public void setPathWatcherFactory(final PathWatcherFactory pathWatcherFactory) {
        synchronized (pathWatcherFactoryLock) {
            this.pathWatcherFactory = pathWatcherFactory;
        }
    }

    /**
     * Gets the list of item property filters automatically appended to the
     * specified filters in methods that send item property filters to the
     * server.
     *
     * @return the default item property filters (may be <code>null</code>)
     */
    public String[] getDefaultItemPropertyFilters() {
        synchronized (defaultItemPropertyFiltersLock) {
            return defaultItemPropertyFilters;
        }
    }

    /**
     * Sets the list of item property filters automatically appended to the
     * specified filters in methods that send item property filters to the
     * server.
     *
     * @param filters
     *        the default item property filters (may be <code>null</code> or
     *        empty)
     */
    public void setDefaultItemPropertyFilters(final String[] filters) {
        synchronized (defaultItemPropertyFiltersLock) {
            this.defaultItemPropertyFilters = filters;
        }
    }

    /**
     * @return the web service level (never <code>null</code>)
     */
    public WebServiceLevel getServiceLevel() {
        return getWebServiceLayer().getServiceLevel();
    }

    /**
     * Gets this version control client's HttpClient used for uploads and
     * downloads, which is cached for performance reasons.
     *
     * @return the HttpClient used for uploads and downloads.
     */
    private HttpClient getHTTPClient() {
        return connection.getHTTPClient();
    }

    /**
     * @return a reference to the EventEngine used by this client. Add (and
     *         remove) listeners to this event engine instance in order to be
     *         notified of events. All client events are dispatched through this
     *         event engine.
     */
    public VersionControlEventEngine getEventEngine() {
        return eventEngine;
    }

    /**
     * @return the GUID that the Team Foundation Server uses to identify itself,
     *         This may change if the server software is re-installed.
     */
    public final GUID getServerGUID() {
        synchronized (serverGUIDLock) {
            return serverGUID;
        }
    }

    /**
     * Called by Workstation when refreshing the workspace cache file. Refreshes
     * the server GUID for this version control server.
     */
    public void refreshServerGUID() {
        // Ask the location service for the GUID for this collection
        synchronized (serverGUIDLock) {
            serverGUID = connection.getInstanceID();
        }
    }

    /**
     * @return the {@link BoundedExecutor} that throttles access to the worker
     *         process pool for uploads and downloads.
     */
    public BoundedExecutor getUploadDownloadWorkerExecutor() {
        return uploadDownloadWorkerExecutor;
    }

    /**
     * Gets the workspace containing a mapping for the given path. May throw a
     * {@link ItemNotMappedException} if there is no mapping or it's not mapped
     * for this server.
     *
     * @param localPath
     *        a local path (must not be <code>null</code> or empty)
     * @param throwIfNotFound
     *        if <code>true</code>, this method throws
     *        {@link ItemNotMappedException} if the path is not mapped;
     *        otherwise, returns <code>null</code>
     * @return the workspace containing a mapping for the path or
     *         <code>null</code> if throwIfNotFound is false and no matching
     *         workspace was found
     * @throws ItemNotMappedException
     *         if the path is not mapped to any local workspace
     */
    public Workspace getLocalWorkspace(String localPath, final boolean throwIfNotFound) throws ItemNotMappedException {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$

        localPath = LocalPath.canonicalize(localPath);

        final WorkspaceInfo info =
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).getLocalWorkspaceInfo(localPath);
        if (info == null || !info.getServerGUID().equals(getServerGUID())) {
            if (throwIfNotFound) {
                throw new ItemNotMappedException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.NoWorkingFolderForFormat"), //$NON-NLS-1$
                        localPath));
            } else {
                return null;
            }
        }

        return getRuntimeWorkspaceCache().getWorkspace(info);
    }

    /**
     * Look up the local workspace for the specified repository, workspaceName
     * and workspaceOwner combo. This will only ever return anything if the
     * workspaceOwner matches the current user. This returns the actual
     * instance, not a copy!
     */
    public Workspace getLocalWorkspace(final String workspaceName, final String workspaceOwner) {
        Check.notNullOrEmpty(workspaceName, "workspaceName"); //$NON-NLS-1$
        Check.notNullOrEmpty(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$

        final WorkspaceInfo info =
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).getLocalWorkspaceInfo(
                getServerGUID(),
                workspaceName,
                resolveUserUniqueName(workspaceOwner));

        if (info != null) {
            return getRuntimeWorkspaceCache().getWorkspace(info);
        }

        return null;
    }

    /**
     * Retrieve the specified workspace directly from the repository server. The
     * server throws an exception if it does not have a workspace that matches.
     * If the server reports that the workspace does not exist, the workspace
     * will be deleted from the cache if it exists there.
     *
     * @param workspaceName
     *        the workspace name (must not be <code>null</code> or empty)
     * @param workspaceOwner
     *        (must not be <code>null</code> or empty)
     * @return the workspace that matches
     * @throws WorkspaceNotFoundException
     *         if the workspace could not be found
     */
    public Workspace getRepositoryWorkspace(final String workspaceName, final String workspaceOwner)
        throws WorkspaceNotFoundException {
        Check.notNullOrEmpty(workspaceName, "workspaceName"); //$NON-NLS-1$
        Check.notNullOrEmpty(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$

        Workspace workspace;
        try {
            workspace = getRuntimeWorkspaceCache().cacheWorkspace(queryWorkspace(workspaceName, workspaceOwner));
        } catch (final WorkspaceNotFoundException e) {
            // If the failure is because a matching workspace does not exist on
            // the server, delete it if it exists in the local cache.
            removeCachedWorkspace(workspaceName, workspaceOwner);
            throw e;
        } finally {
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).saveConfigIfDirty();
        }

        return workspace;
    }

    /**
     * Get a list of matching workspaces. The repository must be a valid
     * repository but the workspace name, owner, and computer may be null, which
     * means match all: Note that this method returns a "private" copy of the
     * workspace so that any future changes to the workspace will not affect the
     * returned workspaces.
     *
     * @param workspaceName
     *        the name of the workspaces (<code>null</code> matches any)
     * @param workspaceOwner
     *        the owner name of the workspaces (<code>null</code> matches any)
     * @param computer
     *        the computer the workspaces are on (<code>null</code> matches any)
     * @return the set of workspaces matching the provided filters
     */
    public Workspace[] getRepositoryWorkspaces(
        final String workspaceName,
        final String workspaceOwner,
        final String computer) {
        return getRepositoryWorkspaces(
            workspaceName,
            workspaceOwner,
            computer,
            WorkspacePermissions.NONE_OR_NOT_SUPPORTED);
    }

    /**
     * Get a list of matching workspaces. The repository must be a valid
     * repository but the workspace name, owner, and computer may be null, which
     * means match all: Note that this method returns a "private" copy of the
     * workspace so that any future changes to the workspace will not affect the
     * returned workspaces.
     *
     * @param workspaceName
     *        the name of the workspaces (<code>null</code> matches any)
     * @param workspaceOwner
     *        the owner name of the workspaces (<code>null</code> matches any)
     * @param computer
     *        the computer the workspaces are on (<code>null</code> matches any)
     * @param permissionsFilter
     *        {@link WorkspacePermissions} to use for filtering by the
     *        AuthorizedUsers's permissions. Supply
     *        {@link WorkspacePermissions#NONE_OR_NOT_SUPPORTED} to not use
     *        permission filtering.
     * @return the set of workspaces matching the provided filters
     */
    public Workspace[] getRepositoryWorkspaces(
        final String workspaceName,
        final String workspaceOwner,
        final String computer,
        final WorkspacePermissions permissionsFilter) {
        final Workspace[] workspaces =
            getWebServiceLayer().queryWorkspaces(workspaceOwner, computer, permissionsFilter);

        // Filter the workspaces by the workspace name specified (owner and
        // computer matched above).
        final List<Workspace> matches = new ArrayList<Workspace>(workspaces.length);

        for (final Workspace workspace : workspaces) {
            if (workspaceName == null || Workspace.matchName(workspace.getName(), workspaceName)) {
                matches.add(workspace);
            }
        }

        final Workspace[] results =
            getRuntimeWorkspaceCache().cacheWorkspaces(matches.toArray(new Workspace[matches.size()]));

        try {
            // Let's check for local item exlcusion updates.
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).checkForLocalItemExclusionUpdates(
                this,
                false);
        } catch (final Exception ex) {
            log.error(
                "Failed to check for local item exclusion updates in VersionControlClient.getRepositoryWorkspaces()", //$NON-NLS-1$
                ex);
        }

        return results;
    }

    /**
     * Create a workspace on the server.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param workingFolders
     *        the initial working folder mappings for this workspace. May be
     *        null, which means no working folders mapped.
     * @param workspaceName
     *        the name of the new workspace (must not be <code>null</code>)
     * @param comment
     *        an optional comment to be stored with this workspace (may be
     *        null).
     * @param location
     *        where the workspace data is stored (if <code>null</code>, the
     *        server's default is used)
     * @param options
     *        options to use on the newly created workspace (if
     *        <code>null</code>, the default options are used)
     * @return the workspace object created by the server.
     */
    public Workspace createWorkspace(
        final WorkingFolder[] workingFolders,
        final String workspaceName,
        final String comment,
        final WorkspaceLocation location,
        final WorkspaceOptions options) {
        return createWorkspace(
            workingFolders,
            workspaceName,
            comment,
            location,
            options,
            WorkspacePermissionProfile.getPrivateProfile());
    }

    public Workspace createWorkspace(
        final WorkingFolder[] workingFolders,
        final String workspaceName,
        final String comment,
        final WorkspaceLocation location,
        final WorkspaceOptions options,
        final WorkspacePermissionProfile permissionProfile) {
        return createWorkspace(
            workingFolders,
            workspaceName,
            VersionControlConstants.AUTHENTICATED_USER,
            VersionControlConstants.AUTHENTICATED_USER,
            comment,
            location,
            options,
            permissionProfile);
    }

    /**
     * Create a workspace on the server.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param workingFolders
     *        the initial working folder mappings for this workspace. May be
     *        null, which means no working folders mapped.
     * @param workspaceName
     *        the name of the new workspace (must not be <code>null</code>)
     * @param owner
     *        the name of the workspace owner (if <code>null</code>, empty, or
     *        {@link VersionControlConstants#AUTHENTICATED_USER} the currently
     *        authorized user's name is used)
     * @param ownerDisplayName
     *        the display name of the workspace owner (if <code>null</code>,
     *        empty, or {@link VersionControlConstants#AUTHENTICATED_USER} the
     *        currently authorized user's display name is used)
     * @param comment
     *        an optional comment to be stored with this workspace (may be
     *        null).
     * @param location
     *        where the workspace data is stored (if <code>null</code>, the
     *        server's default is used)
     * @param options
     *        options to use on the newly created workspace (if
     *        <code>null</code>, the default options are used)
     * @return the workspace object created by the server.
     */
    public Workspace createWorkspace(
        final WorkingFolder[] workingFolders,
        final String workspaceName,
        final String owner,
        final String ownerDisplayName,
        final String comment,
        final WorkspaceLocation location,
        final WorkspaceOptions options) {
        return createWorkspace(
            workingFolders,
            workspaceName,
            owner,
            ownerDisplayName,
            comment,
            location,
            options,
            WorkspacePermissionProfile.getPrivateProfile());
    }

    public Workspace createWorkspace(
        WorkingFolder[] workingFolders,
        final String workspaceName,
        String owner,
        String ownerDisplayName,
        final String comment,
        WorkspaceLocation location,
        WorkspaceOptions options,
        WorkspacePermissionProfile permissionProfile) {
        Check.notNullOrEmpty(workspaceName, "workspaceName"); //$NON-NLS-1$

        if (owner == null || owner.length() == 0) {
            owner = VersionControlConstants.AUTHENTICATED_USER;
        }

        if (ownerDisplayName == null || ownerDisplayName.length() == 0) {
            ownerDisplayName = VersionControlConstants.AUTHENTICATED_USER;
        }

        if (location == null) {
            location = getServerSettingsWithFallback(new AtomicBoolean()).getDefaultWorkspaceLocation();
        }

        if (options == null) {
            // TODO can we get defaults from the server?
            options = WorkspaceOptions.NONE;
        }

        if (permissionProfile == null) {
            permissionProfile = WorkspacePermissionProfile.getPrivateProfile();
        }

        workingFolders = Workspace.checkForInternalMappingConflicts(workspaceName, workingFolders, false);

        Workspace workspace = new Workspace(
            this,
            workspaceName,
            resolveUserUniqueName(owner),
            resolveUserDisplayName(ownerDisplayName),
            null,
            comment,
            null,
            workingFolders,
            LocalHost.getShortName(),
            location,
            WorkspacePermissions.NONE_OR_NOT_SUPPORTED,
            permissionProfile,
            options);

        if (workspace.isLocal()) {
            // The workspace's computer is the same as the current computer;
            // check the local workspace cache to see if the mappings conflict
            // with workspaces from other team project collections.
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).getCache().checkForMappingConflicts(
                workspace,
                null);
        }

        // Create the workspace on the server.
        workspace = getWebServiceLayer().createWorkspace(workspace);

        // Set the permissions for the workspace
        // The CreateWorkspace webmethod will set us up with an ACL containing
        // only the owner ACE,
        // so we only need to call the security service if we have something
        // additional to do.
        if (permissionProfile.getAccessControlEntries().length > 0) {
            setPermissionProfile(workspace, permissionProfile);
            // The user's EffectivePermissions could have changed as a result of
            // setting the permissions
            // on this workspace. Redownload the Workspace from the server.

            workspace = queryWorkspace(workspace.getName(), workspace.getOwnerName());
        }

        // Run the workspace object from the server through the runtime
        // workspace cache.
        workspace = getRuntimeWorkspaceCache().cacheWorkspace(workspace);

        // Update the local cache. Return local or remote workspace, as
        // appropriate.
        if (workspace.isLocal()) {
            if (WorkspaceLocation.LOCAL == workspace.getLocation()) {
                // Delete any existing local metadata for this workspace.
                FileHelpers.deleteDirectory(workspace.getLocalMetadataDirectory());
                final WorkingFolder[] folders = workspace.getFolders();

                final WorkspacePermissionProfile finalPermissionProfile = permissionProfile;

                LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
                try {
                    transaction.execute(new WorkspacePropertiesTransaction() {
                        @Override
                        public void invoke(final LocalWorkspaceProperties wp) {
                            wp.setWorkingFolders(folders);
                            wp.doBaselineFolderMaintenance();
                            wp.applyPermissionsProfileToBaselineFolders(finalPermissionProfile);
                            wp.applyPermissionsProfileToWorkingFolders(finalPermissionProfile);
                        }
                    });
                } finally {
                    try {
                        transaction.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }

                transaction = new LocalWorkspaceTransaction(workspace);
                try {
                    transaction.execute(new PendingChangesTransaction() {

                        @Override
                        public void invoke(final LocalPendingChangesTable pc) {
                            // Create the initial pending changes table for this
                            // workspace. Write into it the initial pending
                            // changes signature.
                            pc.setClientSignature(WebServiceLayerLocalWorkspaces.INITIAL_PENDING_CHANGES_SIGNATURE);
                        }
                    });
                } finally {
                    try {
                        transaction.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }
            }

            // If there is a matching workspace in the cache file, remove it
            // (cache could be stale).
            removeCachedWorkspace(workspace.getName(), workspace.getOwnerName());

            // If the server doesn't support workspace permissions, or it does,
            // and we have Use permission on this
            // workspace, then add it to the cache.
            // if (workspace.VersionControlServer.WebServiceLevel <
            // WebServiceLevel.Tfs2010 ||
            // ((workspace.EffectivePermissions & (WorkspacePermissions.Use |
            // WorkspacePermissions.Administer)) != 0))
            // {
            // Workstation.Current.InsertWorkspaceIntoCache(workspace);
            // }
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).insertWorkspaceIntoCache(workspace);

            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).saveConfigIfDirty();

            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                workspace,
                Notification.VERSION_CONTROL_WORKSPACE_CREATED);

            try {
                // Let's check for local item exclusion updates.
                Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).checkForLocalItemExclusionUpdates(
                    this,
                    false);
            } catch (final Exception ex) {
                log.error(
                    "Failed to check for local item exclusion updates in VersionControlClient.createWorkspace()", //$NON-NLS-1$
                    ex);
            }
        }

        // Events must be fired outside the synchronized block.
        eventEngine.fireWorkspaceCreated(
            new WorkspaceEvent(EventSource.newFromHere(), workspace, WorkspaceEventSource.INTERNAL));

        return workspace;
    }

    /**
     * Update a workspace.
     *
     * @param workspace
     *        the workspace to update; after this method completes, the object
     *        will match server's properties for this workspace (must not be
     *        <code>null</code>)
     * @param newName
     *        the new name or <code>null</code> to keep the existing value
     * @param newOwner
     *        the new owner or <code>null</code> to keep the existing value
     * @param newComment
     *        the new comment or <code>null</code> to keep the existing value
     * @param newMappings
     *        the new mappings or <code>null</code> to keep the existing value
     * @param newComputer
     *        the new computer or <code>null</code> to keep the existing value
     */
    public void updateWorkspace(
        final Workspace workspace,
        final String newName,
        final String newOwner,
        final String newComment,
        final WorkingFolder[] newMappings,
        final String newComputer) {
        updateWorkspace(workspace, newName, newOwner, newComment, newMappings, newComputer, null);
    }

    /**
     * Update a workspace.
     *
     * @param workspace
     *        the workspace to update; after this method completes, the object
     *        will match server's properties for this workspace (must not be
     *        <code>null</code>)
     * @param newName
     *        the new name or <code>null</code> to keep the existing value
     * @param newOwner
     *        the new owner or <code>null</code> to keep the existing value
     * @param newComment
     *        the new comment or <code>null</code> to keep the existing value
     * @param newMappings
     *        the new mappings or <code>null</code> to keep the existing value
     * @param newComputer
     *        the new computer or <code>null</code> to keep the existing value
     * @param newPermissionProfile
     *        the new permissions or <code>null</code> to keep the existing
     *        value
     */
    public void updateWorkspace(
        final Workspace workspace,
        final String newName,
        final String newOwner,
        final String newComment,
        final WorkingFolder[] newMappings,
        final String newComputer,
        final WorkspacePermissionProfile newPermissionProfile) {
        updateWorkspace(
            workspace,
            newName,
            newOwner,
            newComment,
            newMappings,
            newComputer,
            newPermissionProfile,
            false,
            null,
            null);
    }

    /**
     * Update a workspace.
     *
     * @param workspace
     *        the workspace to update; after this method completes, the object
     *        will match server's properties for this workspace (must not be
     *        <code>null</code>)
     * @param newName
     *        the new name or <code>null</code> to keep the existing value
     * @param newOwner
     *        the new owner or <code>null</code> to keep the existing value
     * @param newComment
     *        the new comment or <code>null</code> to keep the existing value
     * @param newMappings
     *        the new mappings or <code>null</code> to keep the existing value
     * @param newComputer
     *        the new computer or <code>null</code> to keep the existing value
     * @param newPermissionProfile
     *        the new permissions or <code>null</code> to keep the existing
     *        value
     * @param removeUnparentedCloaks
     *        When true, will strip from the mappings any cloaks not parented by
     *        a mapping (default is <code>false</code> when other overloads are
     *        used)
     * @param newOptions
     *        the new workspace options or <code>null</code> to keep the
     *        existing value
     * @param newLocation
     *        the new workspace location or <code>null</code> to keep the
     *        existing value
     */
    public void updateWorkspace(
        final Workspace workspace,
        String newName,
        String newOwner,
        String newComment,
        WorkingFolder[] newMappings,
        String newComputer,
        WorkspacePermissionProfile newPermissionProfile,
        final boolean removeUnparentedCloaks,
        WorkspaceOptions newOptions,
        WorkspaceLocation newLocation) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        workspace.refresh();

        if (!workspace.hasAdministerPermission()) {
            throw new VersionControlException(
                MessageFormat.format(
                    Messages.getString("VersionControlClient.NoAdministerPermissionFormat"), //$NON-NLS-1$
                    workspace.getClient().getConnection().getAuthorizedIdentity().getDisplayName(),
                    workspace.getName(),
                    workspace.getOwnerDisplayName()));
        }

        if (newName == null) {
            newName = workspace.getName();
        }
        if (newOwner == null) {
            newOwner = workspace.getOwnerName();
        }
        if (newComment == null) {
            newComment = workspace.getComment();
        }
        if (newMappings == null) {
            newMappings = workspace.getFolders();
        }
        if (newComputer == null) {
            newComputer = workspace.getComputer();
        }
        if (newPermissionProfile == null) {
            newPermissionProfile = workspace.getPermissionsProfile();
        }
        if (newOptions == null) {
            newOptions = workspace.getOptions();
        }
        if (newLocation == null) {
            newLocation = workspace.getLocation();
        }

        Check.notNullOrEmpty(newName, "newName"); //$NON-NLS-1$
        Check.notNullOrEmpty(newOwner, "newOwner"); //$NON-NLS-1$
        // Comment may be null or empty
        Check.notNull(newMappings, "newMappings"); //$NON-NLS-1$
        Check.notNullOrEmpty(newComputer, "newComputer"); //$NON-NLS-1$
        // Permission profile may be null
        Check.notNull(newOptions, "newOptions"); //$NON-NLS-1$

        final WorkspaceLocation oldLocation = workspace.getLocation();
        if (newLocation != oldLocation) {
            /*
             * We will be changing the workspace location (converting from local
             * to server or vice versa) with this call. This operation is
             * performed by a subroutine and is always performed first.
             */
            try {
                setWorkspaceLocation(workspace, newLocation);
            } catch (final CoreCancelException e) {
                throw new CanceledException();
            }
        }

        final String oldLocalMetadataDirectory =
            (WorkspaceLocation.LOCAL == workspace.getLocation()) ? workspace.getLocalMetadataDirectory() : null;

        newMappings = Workspace.checkForInternalMappingConflicts(newName, newMappings, removeUnparentedCloaks);

        /*
         * Because we do not update workspace permissions and the workspace
         * owner atomically, we do not allow the user to update both of these
         * items in a single action.
         */
        if (!workspace.ownerNameMatches(newOwner)
            && newPermissionProfile != null
            && workspace.getPermissionsProfile() != null
            && newPermissionProfile != workspace.getPermissionsProfile()) {
            throw new VersionControlException(
                Messages.getString("VersionControlClient.CannotChangeWorkspaceOwnerAndPermissionsSimultaneously")); //$NON-NLS-1$
        }

        /*
         * If the new workspace ownername did not change then make sure we pass
         * up the unique name for the owning user so that we avoid a multiple
         * display name exception.
         */
        String newOwnerUnique = newOwner;
        if (workspace.ownerNameMatches(newOwner)) {
            newOwnerUnique = workspace.getOwnerName();
        }

        // Save the old name (if different) for the event at the end
        String oldName = null;
        if (!workspace.matchName(newName)) {
            oldName = workspace.getName();
        }

        /*
         * This is the Workspace object to be sent to the server in
         * UpdateWorkspace.
         */
        final Workspace workspaceToSend = new Workspace(
            this,
            newName,
            newOwner,
            newOwnerUnique,
            null,
            newComment,
            null,
            newMappings,
            newComputer,
            workspace.getLocation(),
            WorkspacePermissions.NONE_OR_NOT_SUPPORTED,
            newPermissionProfile,
            newOptions);

        /*
         * Now, check the working folders. Explicitly check whether the
         * workspace is local, including handling the new computer name being
         * different than the old for /updateComputerName, rather than relying
         * on an existing workspace having been deleted since the cache could be
         * out of sync with the server (i.e., workspace exists on the server but
         * not in the cache).
         */
        if (workspace.isLocal() || Workspace.matchComputer(newComputer, LocalHost.getShortName())) {
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).getCache().checkForMappingConflicts(
                workspaceToSend,
                workspace);
        }

        /*
         * Call UpdateWorkspace. This is the Workspace object the server sent
         * back, indicating the new state of the workspace object we currently
         * know as "workspace".
         */
        Workspace newWorkspace = getWebServiceLayer().updateWorkspace(
            workspace.getName(),
            workspace.getOwnerName(),
            workspaceToSend,
            SupportedFeatures.ALL);

        Check.notNull(newWorkspace, "newWorkspace"); //$NON-NLS-1$

        if (oldLocalMetadataDirectory != null
            && WorkspaceLocation.LOCAL == workspace.getLocation()
            && !Workspace.matchSecurityToken(workspace.getSecurityToken(), newWorkspace.getSecurityToken())) {
            FileHelpers.deleteDirectory(newWorkspace.getLocalMetadataDirectory());
            new File(oldLocalMetadataDirectory).renameTo(new File(newWorkspace.getLocalMetadataDirectory()));

            try {
                FileHelpers.deleteDirectory(oldLocalMetadataDirectory);
            } catch (final Exception e) {
                // This was best effort and does not block us in any way.
            }
        }

        /*
         * Update permissions profile (this will call security service). We do
         * this after the UpdateWorkspace call to handle scenarios such as: A
         * user with Administer permission on a workspace, who is not the owner
         * of the workspace, makes a change that is applied through
         * UpdateWorkspace (such as updating the name of the workspace), as well
         * as making a change to the permissions of the workspace that has the
         * net result of denying themselves permission to the workspace.
         *
         * If we apply the permissions before the UpdateWorkspace, the
         * UpdateWorkspace may fail. Additionally, if the security service's
         * entries for this workspace have been damaged in some way,
         * UpdateWorkspace will cause the repair mechanism to be invoked,
         * leaving us in a better state.
         *
         * Note that we apply the permissions profile using the workspace object
         * returned to us by UpdateWorkspace. This way we make sure we have the
         * most current security token for the ACL in the security service --
         * taking into account any potential change in the name of the workspace
         * that occurred with the UpdateWorkspace. A change in the owner cannot
         * be combined with a change in the permissions (see check above which
         * throws an exception in this case), so we do know that the owner has
         * stayed the same.
         */
        if (newPermissionProfile != null && newPermissionProfile != workspace.getPermissionsProfile()) {
            setPermissionProfile(newWorkspace, newPermissionProfile);

            // Updating the permissions profile may have changed the
            // AuthorizedUser's effective permissions.
            // We need to redownload the workspace object from the server now so
            // that these values are current.
            newWorkspace = queryWorkspace(newWorkspace.getName(), newWorkspace.getOwnerName());
            Check.notNull(newWorkspace, "newWorkspace"); //$NON-NLS-1$
        }

        final boolean hasNewOwner = !workspace.ownerNameMatches(newOwner);

        /*
         * OK, now let's have the runtime workspace cache handle updating our
         * constant workspace instance ("workspace") with the data from the new
         * workspace returned to us from the server ("newWorkspace").
         */
        getRuntimeWorkspaceCache().updateWorkspace(workspace, newWorkspace);

        final WorkspacePermissionProfile finalNewPermissionProfile = newPermissionProfile;

        if (WorkspaceLocation.LOCAL == workspace.getLocation()) {
            final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
            try {
                final IdentityDescriptor ownerIdentity = newWorkspace.getOwnerDescriptor();

                transaction.execute(new WorkspacePropertiesTransaction() {
                    @Override
                    public void invoke(final LocalWorkspaceProperties wp) {
                        wp.doBaselineFolderMaintenance();

                        // If the permission profile is changing
                        if (finalNewPermissionProfile != null) {
                            wp.applyPermissionsProfileToBaselineFolders(finalNewPermissionProfile);
                            wp.applyPermissionsProfileToWorkingFolders(finalNewPermissionProfile);
                        }

                        if (hasNewOwner && IdentityConstants.WINDOWS_TYPE.equals(ownerIdentity.getIdentityType())) {
                            /*
                             * Grant filesystem permissions to the new owner if
                             * the new owner is a Windows identity
                             */
                            wp.applyAceToBaselineFolders(ownerIdentity.getIdentifier(), true);
                            wp.applyAceToWorkingFolders(ownerIdentity.getIdentifier(), true);
                        }
                    }
                });
            } finally {
                try {
                    transaction.close();
                } catch (final IOException e) {
                    throw new VersionControlException(e);
                }
            }
        }

        try {
            // Let's force a check for local item exlcusion updates.
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).checkForLocalItemExclusionUpdates(
                this,
                true);
        } catch (final Exception ex) {
            log.error("Failed to check for local item exclusion updates in VersionControlClient.updateWorkspace()", ex); //$NON-NLS-1$
        }

        getEventEngine().fireWorkspaceUpdated(
            new WorkspaceUpdatedEvent(
                EventSource.newFromHere(),
                workspace,
                oldName,
                oldLocation,
                WorkspaceEventSource.INTERNAL));

        Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
            workspace,
            Notification.VERSION_CONTROL_WORKSPACE_CHANGED);
    }

    private void setPermissionProfile(final Workspace workspace, final WorkspacePermissionProfile permissionProfile) {
        Check.notNull(permissionProfile, "permissionProfile"); //$NON-NLS-1$

        if (permissionProfile != null && getWorkspaceSecurity() != null && workspace.getSecurityToken() != null) {
            final ArrayList<AccessControlEntryDetails> aceDetalsList = new ArrayList<AccessControlEntryDetails>();
            for (final AccessControlEntry ace : permissionProfile.getAccessControlEntries()) {
                aceDetalsList.add((AccessControlEntryDetails) ace);
            }

            final AccessControlListDetails acl = new AccessControlListDetails(
                false,
                workspace.getSecurityToken(),
                true,
                aceDetalsList.toArray(new AccessControlEntryDetails[0]));

            if (workspace.getOwnerDescriptor() != null) {
                final int allWorkspacePermissions = WorkspacePermissions.combine(new WorkspacePermissions[] {
                    WorkspacePermissions.READ,
                    WorkspacePermissions.USE,
                    WorkspacePermissions.CHECK_IN,
                    WorkspacePermissions.ADMINISTER
                }).toIntFlags();

                acl.setPermissions(workspace.getOwnerDescriptor(), allWorkspacePermissions, 0, false);
            }

            getWorkspaceSecurity().setAccessControlList(acl);
            workspace.setPermissionsProfile(permissionProfile);
        }
    }

    /**
     * Changes a workspace's location from server to local or vice versa.
     *
     * @param workspace
     *        the workspace to change the location of (must not be
     *        <code>null</code>)
     * @param newLocation
     *        the new location (must not be <code>null</code>)
     */
    private void setWorkspaceLocation(final Workspace workspace, final WorkspaceLocation newLocation)
        throws CoreCancelException {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(newLocation, "newLocation"); //$NON-NLS-1$

        if (newLocation == workspace.getLocation()) {
            // No work to do.
            return;
        }

        // Using UpdateWorkspace to set the location of a workspace was not
        // supported prior to revision Tfs2012_2.
        if (getWebServiceLayer().getServiceLevel().getValue() < WebServiceLevel.TFS_2012_2.getValue()) {
            throw new FeatureNotSupportedException(Messages.getString("Workspace.LocalWorkspacesNotSupported")); //$NON-NLS-1$
        }

        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();
        try {
            // 100 total work units, make sure they all add up below
            taskMonitor.begin(Messages.getString("VersionControlClient.ChangingWorkspaceLocation"), 100); //$NON-NLS-1$

            if (WorkspaceLocation.SERVER == newLocation) {
                // Local -> server
                final WorkspaceLock wLock = workspace.lock();
                try {
                    if (taskMonitor.isCanceled()) {
                        throw new CoreCancelException();
                    }

                    // 1. Reconcile
                    taskMonitor.setCurrentWorkDescription(
                        Messages.getString("VersionControlClient.ReconcilingLocalWorkspaceToServer")); //$NON-NLS-1$
                    final AtomicBoolean pendingChangesUpdatedByServer = new AtomicBoolean();
                    workspace.reconcile(false, pendingChangesUpdatedByServer);
                    taskMonitor.worked(20);

                    // Last cancellation point
                    if (taskMonitor.isCanceled()) {
                        throw new CoreCancelException();
                    }

                    // 2. Mark all items without a pending edit as read-only on
                    // disk, since
                    // this is now a server workspace.
                    taskMonitor.setCurrentWorkDescription(
                        Messages.getString("VersionControlClient.MarkingUneditedReadOnly")); //$NON-NLS-1$
                    LocalDataAccessLayer.markReadOnlyBit(workspace, true, taskMonitor.newSubTaskMonitor(30));

                    // 3. Ask the server to make this a server workspace. Before
                    // we do,
                    // grab a couple of pieces of information we won't be able
                    // to get afterward.
                    taskMonitor.setCurrentWorkDescription(
                        Messages.getString("VersionControlClient.ChangingWorkspaceLocationOnServer")); //$NON-NLS-1$
                    final String localMetadataDirectory = workspace.getLocalMetadataDirectory();
                    final String[] baselineFolders = LocalDataAccessLayer.getBaselineFolders(workspace);

                    getWebServiceLayer().updateWorkspace(
                        workspace.getName(),
                        workspace.getOwnerName(),
                        new Workspace(
                            this,
                            workspace.getName(),
                            workspace.getOwnerName(),
                            workspace.getOwnerDisplayName(),
                            workspace.getOwnerAliases(),
                            workspace.getComment(),
                            workspace.getSecurityToken(),
                            workspace.getFolders(),
                            workspace.getComputer(),
                            WorkspaceLocation.SERVER,
                            workspace.getPermissions(),
                            null,
                            workspace.getOptions()),
                        SupportedFeatures.ALL);
                    taskMonitor.worked(10);

                    // 4. Refresh the Workspace object in this instance of the
                    // client object model.
                    // This has the additional effect of updating the local
                    // workspace cache (VersionControl.config).
                    taskMonitor.setCurrentWorkDescription(
                        Messages.getString("VersionControlClient.RefreshingWorkspace")); //$NON-NLS-1$
                    workspace.refresh();
                    taskMonitor.worked(10);

                    // 5. Get rid of the baseline folders and local metadata
                    // directory for this workspace.
                    // (Past the point of cancellation)
                    taskMonitor.setCurrentWorkDescription(
                        MessageFormat.format(
                            Messages.getString("VersionControlClient.DeletingUnusedBaselineFormat"), //$NON-NLS-1$
                            localMetadataDirectory));
                    FileHelpers.deleteDirectory(localMetadataDirectory);
                    taskMonitor.worked(10);

                    final TaskMonitor deleteBaselineMonitor = taskMonitor.newSubTaskMonitor(20);
                    deleteBaselineMonitor.begin("", baselineFolders.length); //$NON-NLS-1$
                    for (final String baselineFolder : baselineFolders) {
                        taskMonitor.setCurrentWorkDescription(
                            MessageFormat.format(
                                Messages.getString("VersionControlClient.DeletingUnusedBaselineFormat"), //$NON-NLS-1$
                                baselineFolder));

                        FileHelpers.deleteDirectory(baselineFolder);
                        deleteBaselineMonitor.worked(1);
                    }
                    deleteBaselineMonitor.done();
                } finally {
                    if (wLock != null) {
                        wLock.close();
                    }
                }

                // 6. Fire a cross-process notification indicating that the
                // workspace object changed.
                Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                    workspace,
                    Notification.VERSION_CONTROL_WORKSPACE_CHANGED);
            } else if (WorkspaceLocation.LOCAL == newLocation) {
                // Server -> local

                /*
                 * Create a clone of the workspace object which is marked as a
                 * local workspace. We'll use this object for local work before
                 * we convert the workspace on the server. This Workspace object
                 * must not escape this method.
                 */
                final Workspace localWorkspace = new Workspace(
                    this,
                    workspace.getName(),
                    workspace.getOwnerName(),
                    workspace.getOwnerDisplayName(),
                    workspace.getOwnerAliases(),
                    workspace.getComment(),
                    workspace.getSecurityToken(),
                    workspace.getFolders(),
                    workspace.getComputer(),
                    WorkspaceLocation.LOCAL,
                    workspace.getPermissions(),
                    null,
                    workspace.getOptions());

                final WorkspaceLock wLock = workspace.lock();
                try {
                    boolean workspacePropertiesCreated = false;

                    try {
                        if (taskMonitor.isCanceled()) {
                            throw new CoreCancelException();
                        }

                        // Delete any existing local metadata for this
                        // workspace.
                        taskMonitor.setCurrentWorkDescription(
                            Messages.getString("VersionControlClient.RemovingMetadataForLocalWorkspace")); //$NON-NLS-1$
                        FileHelpers.deleteDirectory(localWorkspace.getLocalMetadataDirectory());
                        taskMonitor.worked(10);

                        if (taskMonitor.isCanceled()) {
                            throw new CoreCancelException();
                        }

                        // 1. Set up the workspace properties table for this
                        // workspace.
                        taskMonitor.setCurrentWorkDescription(
                            Messages.getString("VersionControlClient.CreatingPropertiesTable")); //$NON-NLS-1$

                        final LocalWorkspaceTransaction propsTransaction =
                            new LocalWorkspaceTransaction(localWorkspace);

                        // We're explicitly creating a new workspace properties
                        // table here. We don't
                        // want autorecovery to ever run in this scenario.
                        propsTransaction.setAutoRecover(false);

                        try {
                            propsTransaction.execute(new WorkspacePropertiesTransaction() {
                                @Override
                                public void invoke(final LocalWorkspaceProperties wp) {
                                    // Set the working folders for the workspace
                                    // (initial population).
                                    wp.setWorkingFolders(workspace.getFolders());
                                    wp.doBaselineFolderMaintenance();
                                }
                            });
                        } finally {
                            try {
                                propsTransaction.close();
                            } catch (final IOException e) {
                                throw new VersionControlException(e);
                            }
                        }

                        workspacePropertiesCreated = true;
                        taskMonitor.worked(10);

                        if (taskMonitor.isCanceled()) {
                            throw new CoreCancelException();
                        }

                        // 2. Set the pending changes for the local workspace.
                        taskMonitor.setCurrentWorkDescription(
                            Messages.getString("VersionControlClient.CreatingPendingChangesTable")); //$NON-NLS-1$

                        final LocalWorkspaceTransaction pcTransaction = new LocalWorkspaceTransaction(localWorkspace);

                        try {
                            pcTransaction.execute(new LocalVersionPendingChangesTransaction() {
                                @Override
                                public void invoke(final WorkspaceVersionTable lv, final LocalPendingChangesTable pc) {
                                    final PendingSet set = workspace.getPendingChanges();

                                    if (set != null && set.getPendingChanges() != null) {
                                        pc.replacePendingChanges(set.getPendingChanges());
                                    }
                                }
                            });
                        } finally {
                            try {
                                pcTransaction.close();
                            } catch (final IOException e) {
                                throw new VersionControlException(e);
                            }
                        }

                        taskMonitor.worked(10);

                        if (taskMonitor.isCanceled()) {
                            throw new CoreCancelException();
                        }

                        // 3. Get the data we need to populate the local version
                        // table for the workspace.
                        taskMonitor.setCurrentWorkDescription(
                            Messages.getString("VersionControlClient.CreatingLocalVersionTable")); //$NON-NLS-1$

                        final WorkspaceItemSet[] workspaceItemSets = workspace.getItems(new ItemSpec[] {
                            new ItemSpec(ServerPath.ROOT, RecursionType.FULL)
                        }, DeletedState.ANY, ItemType.ANY, true, GetItemsOptions.INCLUDE_RECURSIVE_DELETES);

                        if (null != workspaceItemSets && 1 == workspaceItemSets.length) {
                            if (taskMonitor.isCanceled()) {
                                throw new CoreCancelException();
                            }

                            // 4. Populate the local version table for the
                            // workspace.

                            final BaselineRequest[] baselineRequests = LocalDataAccessLayer.populateLocalVersionTable(
                                localWorkspace,
                                workspaceItemSets[0].getItems(),
                                taskMonitor.newSubTaskMonitor(10));

                            if (null != baselineRequests) {
                                if (taskMonitor.isCanceled()) {
                                    throw new CoreCancelException();
                                }

                                // 5. Put the baselines in place (either by
                                // gzip+hash of existing local content,
                                // or by downloading content from the server).
                                taskMonitor.setCurrentWorkDescription(
                                    Messages.getString("VersionControlClient.CompressingAndDownloadingBaselines")); //$NON-NLS-1$
                                LocalDataAccessLayer.processConversionBaselineRequests(
                                    localWorkspace,
                                    Arrays.asList(baselineRequests));
                            }

                            taskMonitor.worked(20);
                        } else {
                            taskMonitor.worked(30);
                        }

                        // Last cancellation point
                        if (taskMonitor.isCanceled()) {
                            throw new CoreCancelException();
                        }
                    } catch (final CoreCancelException e) {
                        // If we were canceled and we have a workspace
                        // properties table in place, go ahead and take the time
                        // to clean up the $tf folders we created.
                        if (workspacePropertiesCreated) {
                            final String[] baselineFolders = LocalDataAccessLayer.getBaselineFolders(localWorkspace);

                            for (final String baselineFolder : baselineFolders) {
                                FileHelpers.deleteDirectory(baselineFolder);
                            }
                        }

                        throw e;
                    }

                    // 5. Mark all items in the workspace as writable.
                    taskMonitor.setCurrentWorkDescription(
                        Messages.getString("VersionControlClient.RemovingReadOnlyFromUneditedItems")); //$NON-NLS-1$
                    LocalDataAccessLayer.markReadOnlyBit(localWorkspace, false, taskMonitor.newSubTaskMonitor(20));

                    // 6. Mark this workspace as a local workspace.
                    taskMonitor.setCurrentWorkDescription(
                        Messages.getString("VersionControlClient.ChangingWorkspaceLocationOnServer")); //$NON-NLS-1$
                    getWebServiceLayer().updateWorkspace(
                        workspace.getName(),
                        workspace.getOwnerName(),
                        localWorkspace,
                        SupportedFeatures.ALL);
                    taskMonitor.worked(10);

                } finally {
                    if (wLock != null) {
                        wLock.close();
                    }
                }

                // 7. Refresh this Workspace instance in the client object
                // model. This has the additional effect of updating the local
                // workspace cache (VersionControl.config).
                taskMonitor.setCurrentWorkDescription(Messages.getString("VersionControlClient.RefreshingWorkspace")); //$NON-NLS-1$
                workspace.refresh();
                workspace.getWorkspaceWatcher().workingFoldersChanged(workspace.getFolders());
                taskMonitor.worked(10);

                // 8. Fire a cross-process notification indicating that the
                // workspace object changed.
                Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                    workspace,
                    Notification.VERSION_CONTROL_WORKSPACE_CHANGED);
            }
        } finally {
            taskMonitor.done();
        }
    }

    /**
     * Delete a workspace on the server.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param workspace
     *        the workspace to delete.
     */
    public void deleteWorkspace(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        // Delete the workspace on the server.
        final boolean isLocal = workspace.isLocal();

        final String localMetadataRoot =
            WorkspaceLocation.LOCAL == workspace.getLocation() ? workspace.getLocalMetadataDirectory() : null;

        final boolean isInCache = (isLocal
            && Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).getLocalWorkspaceInfo(
                getServerGUID(),
                workspace.getName(),
                workspace.getOwnerName()) != null);

        try {
            getWebServiceLayer().deleteWorkspace(workspace.getName(), workspace.getOwnerName());

            if (isInCache) {
                // Delete it from the cache.
                removeCachedWorkspace(workspace.getName(), workspace.getOwnerName());
            }
        } catch (final VersionControlException exception) {
            // If this failed only because the workspace doesn't exist on
            // the server and it does
            // exist in the cache, delete it locally. Otherwise, re-throw
            // the exception.
            if (!(exception instanceof WorkspaceNotFoundException) || !isInCache) {
                // Re-throw the exception if the exception is not
                // WorkspaceNotFound or if it did not
                // exist in either the repository or the local cache.
                throw exception;
            } else {
                removeCachedWorkspace(workspace.getName(), workspace.getOwnerName());
            }
        } finally {
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).saveConfigIfDirty();
        }

        workspace.setDeleted(true);

        // Events must be fired outside the synchronized block.
        eventEngine.fireWorkspaceDeleted(
            new WorkspaceEvent(EventSource.newFromHere(), workspace, WorkspaceEventSource.INTERNAL));

        if (isLocal) {
            if (null != localMetadataRoot) {
                try {
                    FileHelpers.deleteDirectory(localMetadataRoot);
                } catch (final Exception e) {
                    // This was best effort and does not block us in any way.
                    log.warn(MessageFormat.format(
                        "Ignoring exception deleting local metadata directory {0}", //$NON-NLS-1$
                        localMetadataRoot), e);
                }
            }

            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                workspace,
                Notification.VERSION_CONTROL_WORKSPACE_DELETED);
        }
    }

    /**
     * Retrieve the workspace that is mapped to the provided local path. This
     * method searches all known workspaces on the current computer to identify
     * a workspace that has explicitly or implicitly mapped the provided local
     * path. If no workspace is found, this method throws a
     * ItemNotMappedException.
     *
     * @param localPath
     *        A local path for which a workspace is desired (must not be
     *        <code>null</code>)
     * @return A reference to the workspace object that has mapped the specified
     *         local path
     * @throws ItemNotMappedException
     *         if the path is not mapped to any local workspace
     */
    public Workspace getWorkspace(final String localPath) throws ItemNotMappedException {
        return getLocalWorkspace(localPath, true);
    }

    /**
     * This is the same as GetWorkspace() except that it returns null rather
     * than throwing ItemNotMappedException if the path is not in any known
     * local workspace.
     *
     * @param localPath
     *        A local path for which a workspace is desired (must not be
     *        <code>null</code>)
     * @return A reference to the workspace object that has mapped the specified
     *         local path or null if the local path is not in a local workspace
     */
    public Workspace tryGetWorkspace(final String localPath) {
        return getLocalWorkspace(localPath, false);
    }

    /**
     * Retrieve a workspace from the name and owner in the specified
     * {@link WorkspaceInfo}. The workspace does not need to be on the current
     * computer. The local cache is consulted first, and if it does not contain
     * a matching workspace, the server is queried. If the workspace cannot be
     * found, the method throws.
     *
     * @param workspaceInfo
     *        workspace information object (must not be <code>null</code>)
     * @return a reference to the workspace object representing the workspace
     *         with the specified name and owner
     * @throws WorkspaceNotFoundException
     *         if no matching workspace was found
     */
    public Workspace getWorkspace(final WorkspaceInfo workspaceInfo) throws WorkspaceNotFoundException {
        Check.notNull(workspaceInfo, "workspaceInfo"); //$NON-NLS-1$

        return getWorkspace(workspaceInfo.getName(), workspaceInfo.getOwnerName());
    }

    /**
     * Retrieve a workspace from the name and owner in the specified
     * {@link WorkspaceInfo}. The workspace does not need to be on the current
     * computer. The local cache is consulted first, and if it does not contain
     * a matching workspace, the server is queried. If the workspace cannot be
     * found, the method throws.
     *
     * @param workspaceName
     *        The name of the workspace (must not be <code>null</code>)
     * @param workspaceOwner
     *        The owner of the workspace (must not be <code>null</code>)
     * @return a reference to the workspace object representing the workspace
     *         with the specified name and owner
     * @throws WorkspaceNotFoundException
     *         if no matching workspace was found
     */
    public Workspace getWorkspace(final String workspaceName, final String workspaceOwner)
        throws WorkspaceNotFoundException {
        Check.notNull(workspaceName, "workspaceName"); //$NON-NLS-1$
        Check.notNull(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$

        // Use the internal client method to return a workspace object without
        // going to the server, if we can.
        final Workspace workspace = getLocalWorkspace(workspaceName, workspaceOwner);
        if (workspace == null) {
            return getRepositoryWorkspace(workspaceName, workspaceOwner);
        }
        return workspace;
    }

    /**
     * Returns the workspace on the server that matches the given parameters.
     * Always queries the server immediately; does not check the local workspace
     * cache.
     * <p>
     * Unlike {@link #queryWorkspaces(String, String, String)}, this method does
     * not update the local workspace cache when workspaces are queried, because
     * the workspace's computer is unknown (and the computer must be know to
     * update the cache).
     *
     * @param name
     *        the workspace name to match, null to match all.
     * @param owner
     *        the owner name to match, null to match all. Use
     *        {@link VersionControlConstants#AUTHENTICATED_USER} to retrieve
     *        workspaces owned by the currently logged in user.
     * @return the matching workspace or null if no matching workspace was
     *         found.
     */
    public Workspace queryWorkspace(final String name, final String owner) {
        return getWebServiceLayer().queryWorkspace(name, owner);
    }

    /**
     * Always queries the server immediately; does not check the local workspace
     * cache.
     *
     * @see #queryWorkspaces(String, String, String, WorkspacePermissions)
     */
    public Workspace[] queryWorkspaces(final String name, final String owner, final String computer) {
        return getRepositoryWorkspaces(name, owner, computer, WorkspacePermissions.NONE_OR_NOT_SUPPORTED);
    }

    /**
     * Returns all workspaces on the server that match the given parameters.
     * Always queries the server immediately; does not check the local workspace
     * cache.
     *
     * @param workspaceName
     *        the workspace name to match, null to match all.
     * @param workspaceOwner
     *        the owner name to match, null to match all. Use
     *        {@link VersionControlConstants#AUTHENTICATED_USER} to retrieve
     *        workspaces owned by the currently logged in user.
     * @param computer
     *        the computer name to match, null to match all. Use
     *        LocalHost.getShortName() to match workspaces for this computer.
     * @param permissions
     *        find only workspaces matching the given permissions (must not be
     *        <code>null</code>) Use
     *        {@link WorkspacePermissions#NONE_OR_NOT_SUPPORTED} to find all
     *        workspaces.
     * @return an array of matching workspaces. May be empty but never null.
     */
    public Workspace[] queryWorkspaces(
        final String workspaceName,
        final String workspaceOwner,
        final String computer,
        final WorkspacePermissions permissionsFilter) {
        return getRepositoryWorkspaces(workspaceName, workspaceOwner, computer, permissionsFilter);
    }

    /**
     * Queries the server for the set of matching shelvesets.
     *
     * @param shelvesetName
     *        the name of the desired shelvesets; null to match all.
     * @param shelvesetOwner
     *        the owner of the desired shelvesets; null to match all.
     * @param itemPropertyFilters
     *        the list of properties to be returned on the shelvesets. To get
     *        all properties pass a single filter that is simply "*" (may be
     *        <code>null</code>)
     * @return the shelvesets that matched the query parameters. May be empty
     *         but never null.
     */
    public Shelveset[] queryShelvesets(
        final String shelvesetName,
        final String shelvesetOwner,
        String[] itemPropertyFilters) {
        itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        return getWebServiceLayer().queryShelvesets(shelvesetName, shelvesetOwner, itemPropertyFilters);
    }

    /**
     * Delete a shelveset on the server.
     *
     * @param name
     *        the name of the shelveset to delete (must not be <code>null</code>
     *        or empty).
     * @param owner
     *        the owner of the shelveset to delete (must not be
     *        <code>null</code> or empty).
     */
    public void deleteShelveset(final String name, final String owner) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        Check.notNullOrEmpty(owner, "owner"); //$NON-NLS-1$

        getWebServiceLayer().deleteShelveset(name, owner);
    }

    /**
     * Report failures from the server to the user. This method is generally not
     * called by users of this library.
     *
     * @param workspace
     *        the workspace where these failures occurred (must not be
     *        <code>null</code>)
     * @param failures
     *        the failure array returned from the server (if null, this method
     *        does nothing)
     */
    public void reportFailures(final Workspace workspace, final Failure[] failures) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        if (failures != null) {
            for (int i = 0; i < failures.length; i++) {
                eventEngine.fireNonFatalError(
                    new NonFatalErrorEvent(EventSource.newFromHere(), workspace, failures[i]));
            }
        }
    }

    /**
     * Report failures from the server to the user. This method is generally not
     * called by users of this library.
     *
     * @param failures
     *        the failure array returned from the server (if null, this method
     *        does nothing)
     */
    private void reportFailures(final Failure[] failures) {
        if (failures != null) {
            for (int i = 0; i < failures.length; i++) {
                eventEngine.fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, failures[i]));
            }
        }
    }

    /**
     * Called to record that the download proxy has failed. This is called when
     * a download via proxy fails and the proxy should be disabled before the
     * download is retried.
     */
    public void recordDownloadProxyFailure() {
        connection.getTFProxyServerSettings().recordFailure();
    }

    /**
     * @return the URI for downloading files.
     */
    protected URI getDownloadURI() {
        synchronized (downloadFileURILock) {
            if (downloadFileURI == null) {
                downloadFileURI = URI.create(
                    connection.getServerDataProvider().locationForCurrentConnection(
                        ServiceInterfaceNames.VERSION_CONTROL_DOWNLOAD,
                        ServiceInterfaceIdentifiers.VERSION_CONTROL_DOWNLOAD));

                /*
                 * TFS 2010 will return absolute URIs, but 2008 and previous
                 * will just have path info.
                 */
                if (downloadFileURI.getHost() == null) {
                    downloadFileURI = getConnection().getBaseURI().resolve(downloadFileURI);
                }
            }

            return downloadFileURI;
        }
    }

    /**
     * @return the URI for uploading files.
     */
    protected URI getUploadURI() {
        synchronized (uploadFileURILock) {
            if (uploadFileURI == null) {
                uploadFileURI = URI.create(
                    connection.getServerDataProvider().locationForCurrentConnection(
                        ServiceInterfaceNames.VERSION_CONTROL_UPLOAD,
                        ServiceInterfaceIdentifiers.VERSION_CONTROL_UPLOAD));

                /*
                 * TFS 2010 will return absolute URIs, but 2008 and previous
                 * will just have path info.
                 */
                if (uploadFileURI.getHost() == null) {
                    uploadFileURI = getConnection().getBaseURI().resolve(uploadFileURI);
                }
            }

            return uploadFileURI;
        }
    }

    /**
     * @equivalence downloadFile(spec, destinationFile, autoGunzip, null, null)
     */
    public void downloadFile(final DownloadSpec spec, final File destinationFile, final boolean autoGunzip)
        throws CanceledException {
        downloadFile(spec, destinationFile, autoGunzip, null, null);
    }

    /**
     * Download the file described by spec to the destination file. This method
     * can try the download multiple times to work around some kinds of
     * transient errors (VC proxy unreachable, TCP socket resets). If a fatal
     * error is encountered, an exception is thrown and the destination file is
     * deleted if it exists (but this may fail).
     * <p>
     * If a download proxy is configured and enabled, and an error is
     * encountered download the file, the {@link VersionControlClient} is
     * configured to bypass the download proxy for the rest of its lifetime and
     * the download is retried one time directly from the TFS application
     * server.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param spec
     *        the spec that describes the file to download (must not be
     *        <code>null</code>)
     * @param destinationFile
     *        the local file that will receive the contents of the downloaded
     *        file (must not be <code>null</code> or empty). This file will be
     *        created if it does not exist, otherwise it will be overwritten.
     * @param autoGunzip
     *        if true, the content downloaded is automatically decompressed via
     *        gzip if its Content-Type is application/gzip before being written
     *        to disk (almost all users of this class want this behavior). If
     *        false, the file is written directly to disk without being
     *        uncompressed.
     * @param eventSource
     *        a custom {@link EventSource} or <code>null</code> to use
     *        {@link EventSource#newFromHere()}
     * @param taskMonitor
     *        a custom {@link TaskMonitor} or <code>null</code> to use the
     *        monitor from the {@link TaskMonitorService}
     * @throws CanceledException
     *         if the download was canceled by the user via the
     *         {@link TaskMonitor}. This method tries to delete the
     *         destinationFile after cancelation but this may fail (and these
     *         errors are logged but not rethrown).
     */
    public void downloadFile(
        final DownloadSpec spec,
        final File destinationFile,
        final boolean autoGunzip,
        final EventSource eventSource,
        final TaskMonitor taskMonitor) throws CanceledException {
        Check.notNull(spec, "spec"); //$NON-NLS-1$
        Check.notNull(destinationFile, "destinationFile"); //$NON-NLS-1$

        if (!destinationFile.getParentFile().exists() && !destinationFile.getParentFile().mkdirs()) {
            /*
             * Double-check before throwing to avoid race conditions in mkdirs
             */
            if (!destinationFile.getParentFile().isDirectory()) {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.ErrorCreatingDirectoryBeforeDownloadFormat"), //$NON-NLS-1$
                        destinationFile.getParentFile(),
                        destinationFile));
            }
        }

        FileOutputStream outputStream = null;
        boolean wasCanceled = false;

        try {
            // Overwrite any existing file
            outputStream = new FileOutputStream(destinationFile);

            // This method handles retrying when the VC proxy is unreachable
            downloadFileToStream(spec, outputStream, autoGunzip, eventSource, taskMonitor);

            IOUtils.closeSafely(outputStream);
        } catch (final CanceledException e) {
            wasCanceled = true;
            return;
        } catch (final IOException e) {
            // Error creating temp file, stream error, or something else
            // fatal
            throw new VersionControlException(e);
        } finally {
            // Closes for cancelation, too
            if (outputStream != null) {
                IOUtils.closeSafely(outputStream);
                outputStream = null;
            }

            // Delete if canceled
            if (wasCanceled && !destinationFile.delete()) {
                log.warn(MessageFormat.format("Error deleting file {0} after cancelation", destinationFile)); //$NON-NLS-1$
            }
        }
    }

    /**
     * Downloads a file to a temporary location (created by the method),
     * gunzipping if necessary. The file in the temporary folder is given the
     * name in the fileName parameter.
     * <p>
     * Has the same transient network problem retry behavior as
     * {@link #downloadFile(DownloadSpec, File, boolean)}.
     *
     * @param spec
     *        the spec that describes the file to download (must not be
     *        <code>null</code>)
     * @param fileName
     *        the file name (not full path) to give the file locally, once it is
     *        downloaded (must not be <code>null</code> or empty)
     * @return the {@link FileAttachmentDownloadException} where the file's data
     *         was written.
     * @throws CanceledException
     *         if the operation was canceled via the default {@link TaskMonitor}
     */
    public File downloadFileToTempLocation(final DownloadSpec spec, final String fileName) throws CanceledException {
        Check.notNull(spec, "spec"); //$NON-NLS-1$
        Check.notNullOrEmpty(fileName, "fileName"); //$NON-NLS-1$

        /*
         * Create a temporary directory using the TempStorageService, since it
         * can clean up automatically when this application exits.
         */
        File tempDir;
        try {
            tempDir = TempStorageService.getInstance().createTempDirectory();
        } catch (final IOException ex) {
            throw new VersionControlException(ex);
        }

        final File tempFile = new File(tempDir, fileName);

        downloadFile(spec, tempFile, true);

        return tempFile;
    }

    /**
     * This convenience method does not support retrying downloads that failed
     * with transient network problems. Call
     * {@link #downloadFileToStreams(DownloadSpec, DownloadOutput[], EventSource, TaskMonitor)}
     * with a {@link DownloadOutput} that supports stream reset if you need
     * retries.
     *
     * @equivalence downloadFileToStream(spec, outputStream, autoGunzip, null,
     *              null)
     */
    public void downloadFileToStream(final DownloadSpec spec, final OutputStream outputStream, final boolean autoGunzip)
        throws CanceledException {
        downloadFileToStream(spec, outputStream, autoGunzip, null, null);
    }

    /**
     * This convenience method does not support retrying downloads that failed
     * with transient network problems. Call
     * {@link #downloadFileToStreams(DownloadSpec, DownloadOutput[], EventSource, TaskMonitor)}
     * with a {@link DownloadOutput} that supports stream reset if you need
     * retries.
     *
     *
     * @equivalence downloadFileToStreams(spec, new DownloadOutput[] { new
     *              OutputStreamDownloadOutput(outputStream, autoGunzip) },
     *              eventSource, taskMonitor)
     */
    public void downloadFileToStream(
        final DownloadSpec spec,
        final OutputStream outputStream,
        final boolean autoGunzip,
        final EventSource eventSource,
        final TaskMonitor taskMonitor) throws CanceledException {
        Check.notNull(outputStream, "outputStream"); //$NON-NLS-1$

        downloadFileToStreams(spec, new DownloadOutput[] {
            new OutputStreamDownloadOutput(outputStream, autoGunzip)
        }, eventSource, taskMonitor);
    }

    /**
     * Download the file described by spec to the destination stream or streams.
     * <p>
     * <h3>Notice:</h3>
     * <p>
     * This method can only recover from transient network problems (TCP socket
     * resets) if all {@link DownloadOutput}s support stream reset. Download
     * proxy connect errors do not require output stream reset because not data
     * will have been written to them yet.
     * <p>
     * The output streams are always left open (they may have been reset, but no
     * effort is made to finally close them), even when an exception is thrown.
     *
     * @param spec
     *        the spec that describes the file to download (must not be
     *        <code>null</code>)
     * @param outputs
     *        the outputs where the downloaded data will be written (must not be
     *        <code>null</code> or empty)
     * @param eventSource
     *        a custom {@link EventSource} or <code>null</code> to use
     *        {@link EventSource#newFromHere()}
     * @param taskMonitor
     *        a custom {@link TaskMonitor} or <code>null</code> to use the
     *        monitor from the {@link TaskMonitorService}
     * @throws CanceledException
     *         if the download was cancelled by the user via core's
     *         {@link TaskMonitor}. The output streams may have had some data
     *         written to them.
     */
    public void downloadFileToStreams(
        final DownloadSpec spec,
        final DownloadOutput[] outputs,
        EventSource eventSource,
        final TaskMonitor taskMonitor) throws CanceledException {
        Check.notNull(spec, "spec"); //$NON-NLS-1$
        Check.notNullOrEmpty(outputs, "outputs"); //$NON-NLS-1$

        final int maxRetry = Integer.getInteger(MAX_REQUEST_RETRY_PROPERTY, MAX_REQUEST_RETRY_DEFAULT);

        if (eventSource == null) {
            eventSource = EventSource.newFromHere();
        }

        /*
         * This method aims to be tolerant of connection resets caused by
         * half-open sockets. It detects them and retries the operation once.
         * See the full notes in
         * com.microsoft.tfs.core.ws.runtime.client.SOAPService
         * #executeSOAPRequest about why this can happen when talking to IIS.
         *
         * Some JREs use the string "Connection reset by peer", others use
         * "Connection reset". We will match both.
         *
         * As soon as an exception is caught, we remember that we had that type
         * of exception, then let the loop retry. Second time we hit that same
         * exception we throw it.
         *
         * Also retry when when the download proxy is unavailable.
         */

        boolean hadDownloadProxyException = false;
        boolean hadSocketException = false;

        for (int retryCount = 1; retryCount <= maxRetry; retryCount++) {
            log.info("File download attempt " + retryCount); //$NON-NLS-1$

            try {
                // Don't reset for proxy error; no data would have been written
                if (hadSocketException) {
                    resetOutputs(outputs);
                }

                downloadFileToStreamsInternal(spec, outputs, taskMonitor);

                /*
                 * This breaks from the loop in the success case.
                 */
                return;
            } catch (final SocketException e) {
                log.warn("SocketException for " + spec.getQueryString(), e); //$NON-NLS-1$
                /*
                 * If this fault was not a TCP connection reset, rethrow it.
                 * Weeds out non-retryable socket exceptions.
                 */
                if (!e.getMessage().startsWith("Connection reset")) //$NON-NLS-1$
                {
                    throw new VersionControlException(e);
                }

                if (retryCount == maxRetry) {
                    log.warn(MessageFormat.format(
                        "Max retry reached {0}, not trying any longer", //$NON-NLS-1$
                        spec.getQueryString()));
                    throw new VersionControlException(e);
                } else {
                    hadSocketException = true;
                }

                log.warn(
                    MessageFormat.format("Retrying download after a connection reset for {0}", spec.getQueryString()), //$NON-NLS-1$
                    e);
            } catch (final SocketTimeoutException e) {
                log.warn("SocketTimeoutException for " + spec.getQueryString(), e); //$NON-NLS-1$

                if (retryCount == maxRetry) {
                    log.warn(MessageFormat.format(
                        "Max retry reached {0}, not trying any longer", //$NON-NLS-1$
                        spec.getQueryString()));
                    throw new VersionControlException(e);
                } else {
                    hadSocketException = true;
                }

                log.warn(
                    MessageFormat.format("Retrying download after a socket timeout for {0}", spec.getQueryString()), //$NON-NLS-1$
                    e);
            } catch (final DownloadProxyException e) {
                /*
                 * Since we call recordDownloadProxyFailure, we should never
                 * have another of these during a retry, but the check is
                 * probably valuable to prevent an infinite loop.
                 */
                if (hadDownloadProxyException) {
                    log.warn(
                        MessageFormat.format(
                            "Second download proxy error for {0}, which should never happen because we disabled the download proxy, rethrowing", //$NON-NLS-1$
                            spec.getQueryString()));
                    throw new VersionControlException(e);
                } else {
                    hadDownloadProxyException = true;
                }

                log.warn(MessageFormat.format(
                    "Download proxy error for {0}, disabling for this session", //$NON-NLS-1$
                    spec.getQueryString()));

                /*
                 * The download proxy exception has a nice message in it, but we
                 * need to let the user know we'll retry.
                 */
                getEventEngine().fireNonFatalError(new NonFatalErrorEvent(eventSource, this, e));
                getEventEngine().fireNonFatalError(new NonFatalErrorEvent(eventSource, this, new DownloadProxyException(
                    //@formatter:off
                    Messages.getString("VersionControlClient.DisablingDownloadProxyForThisSessionAndRetrying")))); //$NON-NLS-1$
                    //@formatter:on
                recordDownloadProxyFailure();
            } catch (final IOException e) {
                /*
                 * This is an exception from resetting the streams after a retry
                 * (probably because the output doesn't support stream reset).
                 * Just rethrow.
                 */
                log.warn("Fatal IOException stops download retries", e); //$NON-NLS-1$
                throw new VersionControlException(e);
            }

            // Let the loop retry the download.

            final long delayTime = 10000 * (long) Math.pow(2, retryCount - 1);
            log.info("Retrying request in " + delayTime + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                Thread.sleep(delayTime);
            } catch (final InterruptedException e) {
                log.debug("Sleeping thred has been interrupted", e); //$NON-NLS-1$
                throw new VersionControlException(e);
            }
        }
    }

    /**
     * Resets all the streams in the given outputs. Throws the first exception
     * if any could not be reset, but tries to close them all anyway.
     */
    private void resetOutputs(final DownloadOutput[] outputs) throws IOException {
        IOException firstResetException = null;

        for (final DownloadOutput output : outputs) {
            try {
                output.resetOutputStream();
            } catch (final IOException e) {
                /*
                 * DownloadOutputStreamSpec is not required to support reset.
                 */
                log.warn(MessageFormat.format("Could not reset stream on download output {1}", output), e); //$NON-NLS-1$

                if (firstResetException == null) {
                    firstResetException = e;
                }
            }
        }

        if (firstResetException != null) {
            throw firstResetException;
        }
    }

    /**
     * Downloads to one or more output streams, immediately throwing on all
     * errors (never retries). The streams are always left open.
     */
    private void downloadFileToStreamsInternal(
        final DownloadSpec spec,
        final DownloadOutput[] outputs,
        TaskMonitor taskMonitor) throws CanceledException, DownloadProxyException, SocketException {
        GetMethod method = null;

        if (taskMonitor == null) {
            taskMonitor = TaskMonitorService.getTaskMonitor();
        }

        InputStream responseStream = null;

        try {
            method = beginDownloadRequest(spec);

            final String contentLength = method.getResponseHeader("Content-Length").getValue(); //$NON-NLS-1$
            String contentType = method.getResponseHeader("Content-Type").getValue(); //$NON-NLS-1$

            responseStream = method.getResponseBodyAsStream();

            /*
             * Split the outputs into two lists: those that require the wire
             * bytes (which may be compressed with gzip, may not) and those that
             * want normal post-processed (possibly gunzipped) bytes.
             *
             * If the server's content type isn't gzip, all the outputs go into
             * normalOutputs because the server is sending un-gzipped content.
             * If the server's content type is gzip, and all the outputs want
             * wire content, they can also all go to normalOutputs and a
             * GZIPInputStream isn't even used.
             */
            final List<DownloadOutput> wireOutputs = new ArrayList<DownloadOutput>();
            final List<DownloadOutput> normalOutputs = new ArrayList<DownloadOutput>();

            boolean needGZIPInputStream = false;

            /*
             * Note that the server can send zero-byte files that have a
             * designated Content-Type of "application/gzip". These are not
             * actually valid gzip files (all gzip files, regardless of length,
             * require a gzip header and are thus at least 20 bytes.) Therefore
             * we treat 0 byte files as "application/octet-stream" regardless of
             * what the server calls them and don't open a gzip decoder for
             * them.
             */
            if ("0".equals(contentLength) //$NON-NLS-1$
                || contentType.equalsIgnoreCase(DownloadContentTypes.APPLICATION_OCTET_STREAM)) {
                contentType = DownloadContentTypes.APPLICATION_OCTET_STREAM;

                // All outputs get uncompressed files (not wire)
                for (final DownloadOutput output : outputs) {
                    output.setActualContentType(DownloadContentTypes.APPLICATION_OCTET_STREAM);
                    normalOutputs.add(output);
                }
            } else if (contentType.equalsIgnoreCase(DownloadContentTypes.APPLICATION_GZIP)) {
                contentType = DownloadContentTypes.APPLICATION_GZIP;

                // Split the outputs into two lists
                for (final DownloadOutput output : outputs) {
                    if (output.isAutoGunzip()) {
                        needGZIPInputStream = true;
                        normalOutputs.add(output);
                        output.setActualContentType(DownloadContentTypes.APPLICATION_OCTET_STREAM);
                    } else {
                        wireOutputs.add(output);
                        output.setActualContentType(DownloadContentTypes.APPLICATION_GZIP);
                    }
                }

                /*
                 * Optimization: If no GZIPInputStream is needed, move all the
                 * outputs to normalOutputs and we can skip allocating a
                 * GZIPInputStream entirely.
                 */
                if (!needGZIPInputStream) {
                    normalOutputs.addAll(wireOutputs);
                    wireOutputs.clear();
                }
            } else {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.UnsupportedContentTypeFormat"), //$NON-NLS-1$
                        contentType));
            }

            if (needGZIPInputStream) {
                /*
                 * If there are any wire outputs, hook up a TappedInputStream to
                 * feed them.
                 */
                if (wireOutputs.size() > 0) {
                    responseStream = new TappedInputStream(responseStream, new ReadHandler() {
                        @Override
                        public void handleRead(final byte[] b, final int off, final int len, final int readCount)
                            throws IOException {
                            for (final DownloadOutput output : wireOutputs) {
                                output.getOutputStream().write(b, off, readCount);
                            }
                        }

                        @Override
                        public void handleRead(final byte[] b, final int readCount) throws IOException {
                            for (final DownloadOutput output : wireOutputs) {
                                output.getOutputStream().write(b, 0, readCount);
                            }
                        }

                        @Override
                        public void handleRead(final byte b) throws IOException {
                            for (final DownloadOutput output : wireOutputs) {
                                output.getOutputStream().write(b);
                            }
                        }
                    });
                }

                responseStream = new GZIPInputStream(responseStream);
            }

            /*
             * Read from the response stream writing to all the normal outputs.
             * We have to pull the bytes through responseStream even if there
             * are no uncompressed outputs so the TappedInputStream works.
             */

            final byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            if (method.getResponseContentLength() > 0) {
                int read;
                while ((read = responseStream.read(buffer, 0, buffer.length)) > 0) {
                    if (taskMonitor.isCanceled()) {
                        abortDownloadRequest(method);
                        throw new CanceledException();
                    }

                    for (final DownloadOutput output : normalOutputs) {
                        output.getOutputStream().write(buffer, 0, read);
                    }
                }
            } else {
                // This is a zero length file. Force the outputs to start.
                for (final DownloadOutput output : normalOutputs) {
                    output.getOutputStream();
                }
            }

            IOUtils.closeSafely(responseStream);
            responseStream = null;

            finishDownloadRequest(method);
            method = null;
        } catch (final SocketException e) {
            throw e;
        } catch (final IOException e) {
            throw new VersionControlException(e);
        } finally {
            if (responseStream != null) {
                IOUtils.closeSafely(responseStream);
            }

            if (method != null) {
                abortDownloadRequest(method);
            }
        }
    }

    /**
     * Downloads a collection of {@link Item}s to a temporary location
     * preserving the server path structure. The local download location is
     * specified by the localRoot parameter, which can be null (a temporary
     * location is chosen by the method in this case).
     * <p>
     * {@link Item}s that are files must have a non-null download URL. Folder
     * {@link Item}s will be created on disk even if empty.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param items
     *        the set of items to download (must not be <code>null</code>)
     * @param serverRoot
     *        the server path shared by all items (must not be <code>null</code>
     *        or empty).
     * @param localRoot
     *        the local path where the downloaded items will be placed. If null,
     *        a temporary path is chosen by the method.
     * @return the full local path to the directory where the items were
     *         downloaded.
     * @throws CanceledException
     *         if this operation is canceled by the default {@link TaskMonitor}
     */
    public String downloadItems(final Item[] items, final String serverRoot, String localRoot)
        throws CanceledException {
        Check.notNull(items, "items"); //$NON-NLS-1$
        Check.notNull(serverRoot, "serverRoot"); //$NON-NLS-1$

        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();

        taskMonitor.begin(Messages.getString("VersionControlClient.DownloadingItems"), items.length + 2); //$NON-NLS-1$
        try {
            if (localRoot == null) {
                try {
                    localRoot = TempStorageService.getInstance().createTempDirectory().getAbsolutePath();
                } catch (final IOException ex) {
                    throw new VersionControlException(ex);
                }

                TempStorageService.getInstance().forgetItem(new File(localRoot));
            }
            taskMonitor.worked(1);

            Arrays.sort(items, new Comparator<Item>() {
                @Override
                public int compare(final Item item1, final Item item2) {
                    return ServerPath.compareTopDown(item1.getServerItem(), item2.getServerItem());
                }
            });
            taskMonitor.worked(1);

            final EventSource eventSource = EventSource.newFromHere();

            /*
             * Since we will dispatch some get operations to be completed in a
             * different thread (managed by an Executor), we have to keep track
             * of them here. The completion service wraps the bounded executor
             * shared by all GetEngines that use the same VersionControlClients.
             * The completion service only tracks the jobs we have submitted for
             * execution, so we can simply count the completed jobs to know when
             * they have all been processed.
             */
            final AccountingCompletionService<WorkerStatus> completionService =
                new AccountingCompletionService<WorkerStatus>(getUploadDownloadWorkerExecutor());

            try {
                for (int i = 0; i < items.length; i++) {
                    if (taskMonitor.isCanceled()) {
                        throw new CanceledException();
                    }

                    final Item item = items[i];

                    if (item.getItemType() == ItemType.FILE) {
                        Check.notNull(item.getDownloadURL(), "item.getDownloadUrl()"); //$NON-NLS-1$
                    }

                    if (ServerPath.isChild(serverRoot, item.getServerItem()) == false) {
                        throw new IllegalArgumentException(
                            MessageFormat.format(
                                "Item''s server path {0} must start with server root {1}", //$NON-NLS-1$
                                item.getServerItem(),
                                serverRoot));
                    }

                    final String localPath = ServerPath.makeLocal(item.getServerItem(), serverRoot, localRoot);

                    taskMonitor.setCurrentWorkDescription(localPath);

                    completionService.submit(
                        new TempDownloadWorker(
                            eventSource,
                            taskMonitor,
                            this,
                            item.getDownloadURL(),
                            new File(localPath),
                            item.getItemType()));

                    taskMonitor.worked(1);
                }
            } finally {
                GetEngine.waitForCompletions(completionService);
            }

            if (taskMonitor.isCanceled()) {
                throw new CanceledException();
            }

            return localRoot;
        } finally {
            taskMonitor.done();
        }
    }

    /**
     * Creates an HTTP download connection item for the given download spec,
     * initialized for downloading a file from the TFS. The object returned is
     * an Apache HttpClient GetMethod object whose status code has been
     * validated to be HttpStatus.SC_OK. This means you can read the response
     * from this stream using getResponseBodyAsStream() or
     * getResponseBodyAsString() (or other methods). You must pass this method
     * object back into this class's finishDownloadRequest() when you are
     * finished.
     * <p>
     * If a TFS download proxy was set during the construction of TFSConnection,
     * it is contacted here. Otherwise, the host passed to the TFSConnection
     * constructor is contacted.
     * <p>
     * <b>NOTE:</b> This method is not synchronized (but is thread-safe) because
     * doing so would allow deadlock during concurrent downloads. When there are
     * more downloader threads than available connections in the HttpClient's
     * pool, a thread may sleep in the HttpClient code (while holding a lock on
     * this object) until a connection becomes available, which can only happen
     * if other threads finish using their connections (which they can't because
     * they need to acquire a lock on this object to do so).
     *
     * @param spec
     *        the spec object that describes the item being downloaded (not
     * @return a new GetMethod object that has been validated to have status
     *         HttpStatus.SC_OK. The caller can read its data using
     *         getResponseBodyAsStream or getResponseBodyAsString(), then close
     *         the stream with releaseConnection().
     * @throws MalformedURLException
     *         if an error occured building the URL from the spec and the
     *         existing connection information.
     * @throws DownloadProxyException
     *         if a download proxy was used and the HTTP GET failed for any
     *         reason.
     * @throws SocketException
     *         if a socket error occurred. This method handles most IO
     *         exceptions by rethrowing them as {@link VersionControlException}
     *         s, but {@link SocketException}s are thrown so callers can detect
     *         connection resets and retry their operation. Retrying
     *         automatically inside this method is problematic because it knows
     *         nothing about higher-level state that may need reset for a retry.
     */
    private GetMethod beginDownloadRequest(final DownloadSpec spec)
        throws MalformedURLException,
            DownloadProxyException,
            SocketException {
        Check.notNull(spec, "spec"); //$NON-NLS-1$

        final TFProxyServerSettings tfProxyServerSettings = connection.getTFProxyServerSettings();

        String tfsProxyURL = null;
        if (tfProxyServerSettings.isAvailable()) {
            tfsProxyURL = tfProxyServerSettings.getURL();
        }

        /*
         * A note about URL encoding / escaping:
         *
         * The download spec that TFS gives us contains some already-escaped
         * values. If we pass that string into a URI class constructor or
         * resolve it against a URI, the escape characters will be
         * double-escaped. So we deal with URI strings only here.
         */

        String downloadURIString = null;
        String downloadHost = null;

        /*
         * URI Encoding Special Notice
         *
         * The download spec is a URI query string
         * ("name=value&zap=baz&other=stuff") but some of the query arguments
         * are already URI encoded ("s=before%2Fafter") and some of them are not
         * ("cp=/tfs/Collection Name With Spaces/")!
         *
         * To avoid breaking the string down into each args and encoding just
         * the ones we know are usually unencoded ("cp", which is the project
         * collection name), we call a special utility method in URIUtils which
         * exists pretty much just for this one case.
         */

        final String reEncodedDownloadSpec = URIUtils.encodeQueryIgnoringPercentCharacters(spec.getQueryString());

        if (tfsProxyURL == null) {
            /*
             * Use the existing server settings.
             */
            final URI uri = getDownloadURI();

            downloadURIString = uri.toString() + "?" + reEncodedDownloadSpec; //$NON-NLS-1$
            downloadHost = uri.getHost();
        } else {
            // TODO: Check with TFS2010 VC Proxy

            /*
             * TFS 2005 uses a proxy download file without a "V1.0" in it. TFS
             * 2008 introduced the "V1.0" part, but remains compatible with TFS
             * 2005's path. TFS 2010 drops the 2005 compat. To detect which
             * version, we check the supported features for "create branch",
             * which was introduced in 2008. Kind of a hack.
             */
            final String proxyDownloadFile = getServerSupportedFeatures().contains(SupportedFeatures.CREATE_BRANCH)
                ? VersionControlConstants.PROXY_DOWNLOAD_FILE_2008 : VersionControlConstants.PROXY_DOWNLOAD_FILE_2005;

            final URI uri = URIUtils.newURI(tfsProxyURL).resolve(proxyDownloadFile);

            downloadURIString = uri.toString()
                + "?" //$NON-NLS-1$
                + reEncodedDownloadSpec
                + "&" //$NON-NLS-1$
                + VersionControlConstants.PROXY_REPOSITORY_ID_QUERY_STRING
                + "=" //$NON-NLS-1$
                + getServerGUID();
            downloadHost = uri.getHost();
        }

        /*
         * Construct a Get method for our HttpClient to use.
         */
        final GetMethod getMethod = new GetMethod(downloadURIString);
        getMethod.setDoAuthentication(true);

        final HttpClient client = getHTTPClient();
        int status = -1;

        /*
         * If any exception happens, finish the method immediately because we
         * won't be returning it. This releases the connection to the connection
         * manager.
         */
        try {
            status = client.executeMethod(getMethod);
        } catch (final ConnectException e) {
            finishDownloadRequest(getMethod);
            if (tfsProxyURL != null) {
                throw new DownloadProxyException(MessageFormat.format(
                    Messages.getString("VersionControlClient.CouldNotConnectToDownloadProxyServerFormat"), //$NON-NLS-1$
                    downloadHost,
                    e.getMessage()));
            } else {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.CouldNotConnecttoTFSFormat"), //$NON-NLS-1$
                        downloadHost,
                        e.getLocalizedMessage()));
            }
        } catch (final UnknownHostException e) {
            finishDownloadRequest(getMethod);
            if (tfsProxyURL != null) {
                throw new DownloadProxyException(MessageFormat.format(
                    Messages.getString("VersionControlClient.CouldNotResolveDownloadProxyServertoNetworkAddressFormat"), //$NON-NLS-1$
                    downloadHost));
            } else {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.CouldNotResolveTFSToNetworkAddressFormat"), //$NON-NLS-1$
                        downloadHost));
            }
        } catch (final SocketException e) {
            finishDownloadRequest(getMethod);
            throw e;
        } catch (final SocketTimeoutException e) {
            finishDownloadRequest(getMethod);
            if (tfsProxyURL != null) {
                throw new DownloadProxyException(MessageFormat.format(
                    Messages.getString("VersionControlClient.ErrorConnectingToDownloadProxyServerFormat"), //$NON-NLS-1$
                    downloadHost,
                    e.getMessage()));
            } else {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.ErrorConnectingToTFSFormat"), //$NON-NLS-1$
                        downloadHost,
                        e.getLocalizedMessage()));
            }
        } catch (final IOException e) {
            finishDownloadRequest(getMethod);
            if (tfsProxyURL != null) {
                throw new DownloadProxyException(MessageFormat.format(
                    Messages.getString("VersionControlClient.ErrorConnectingToDownloadProxyServerFormat"), //$NON-NLS-1$
                    downloadHost,
                    e.getMessage()));
            } else {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.ErrorConnectingToTFSFormat"), //$NON-NLS-1$
                        downloadHost,
                        e.getLocalizedMessage()));
            }
        }

        if (status != HttpStatus.SC_OK) {
            /*
             * Finish the method before we throw.
             */
            finishDownloadRequest(getMethod);

            if (tfsProxyURL != null) {
                throw new DownloadProxyException(MessageFormat.format(
                    Messages.getString("VersionControlClient.DownloadProxyAtURLReturnedHTTPStatusForGETFormat"), //$NON-NLS-1$
                    tfsProxyURL.toString(),
                    status));
            } else {
                /* Handle status will always throw a VersionControlException */
                handleStatus(getMethod);
            }
        }

        return getMethod;
    }

    /**
     * Aborts a file download.
     *
     * @param method
     *        the GetMethod object returned by beginDownloadRequest().
     */
    private void abortDownloadRequest(final GetMethod method) {
        Check.notNull(method, "method"); //$NON-NLS-1$
        method.abort();
        finishDownloadRequest(method);
    }

    /**
     * Finishes a file download. No status code is returned because none is
     * available.
     *
     * @param method
     *        the GetMethod object returned by beginDownloadRequest().
     */
    private void finishDownloadRequest(final GetMethod method) {
        Check.notNull(method, "method"); //$NON-NLS-1$
        method.releaseConnection();
    }

    /**
     * Begins a file upload. Creates PostMethod object configured with the URL
     * of the TFS and all required authentication information. The caller should
     * configure this object's post data, then pass the object into this class's
     * executeUploadRequest().
     * <p>
     * <b>NOTE:</b> This method is not synchronized (but is thread-safe) because
     * doing so would allow deadlock during concurrent uploads. When there are
     * more uploader threads than available connections in the HttpClient's
     * pool, a thread may sleep in the HttpClient code (while holding a lock on
     * this object) until a connection becomes available, which can only happen
     * if other threads finish using their connections (which they can't because
     * they need to acquire a lock on this object to do so).
     *
     * @return a new PostMethod object set to the Team Foundation Server's
     *         upload page URL. The caller should set the request stream using
     *         setRequestBody(), then execute this class's
     *         executeUploadRequest() with the configured object.
     */
    public PostMethod beginUploadRequest() {
        final String uploadPage = getUploadURI().toString();

        /*
         * Construct a Post method for our HttpClient to use.
         */
        final PostMethod post = new PostMethod(uploadPage);
        post.setDoAuthentication(true);

        return post;
    }

    /**
     * Executes a file upload. First call beginUploadRequest(), then configure
     * the object it returns, then supply that object to this method. The HTTP
     * status code is returned. After this method completes, and you have
     * checked its status code, you must call this class's
     * finishUploadRequest().
     *
     * @param method
     *        the object constructed by beginUploadRequest() and configured with
     *        your post data.
     * @throws IOException
     *         if an error occurred opening the connection to the server
     * @throws VersionControlException
     *         if an error occurred executing the method (if the status code is
     *         not HttpStatus.SC_OK).
     */
    public void executeUploadRequest(final PostMethod method) throws IOException {
        Check.notNull(method, "method"); //$NON-NLS-1$

        final HttpClient client = getHTTPClient();
        Check.notNull(client, "client"); //$NON-NLS-1$

        final int status = client.executeMethod(method);

        if (status != HttpStatus.SC_OK) {
            /* Handle status will always throw a VersionControlException */
            handleStatus(method);
        }
    }

    /**
     * This will attempt to handle non-OK statuses from executing methods
     * against the server. This is used only by the upload and download
     * functions, since those are not part of the SOAP client framework and do
     * not understand SOAP faults.
     *
     * This message will always throw a {@link VersionControlException}. TFS may
     * return detailed status information in the body of a message. If this
     * information exists, it will be returned in the exception details.
     * Otherwise, the exception message will merely contain information about
     * the HTTP response.
     *
     * @throws VersionControlException
     *         with the details of the error that occurred
     */
    private void handleStatus(final HttpMethodBase method) {
        Check.notNull(method, "method"); //$NON-NLS-1$

        String request = "request"; //$NON-NLS-1$

        try {
            request = method.getName() + " " + method.getURI(); //$NON-NLS-1$
        } catch (final IOException e) {
            /* Suppress */
        }

        if (method.getStatusCode() == 500 && method.getResponseHeader("X-VersionControl-Exception") != null) //$NON-NLS-1$
        {
            /*
             * Contains the exception name, such as
             * "IncompletePendingChangeException"
             */
            final String exceptionName = method.getResponseHeader("X-VersionControl-Exception").getValue(); //$NON-NLS-1$

            /*
             * Contains a long(er) informative error message.
             *
             * Note that 2048 is chosen as this is the chunk size in httpclient
             * getResponseBody(int), this will avoid reallocs.
             */
            String responseString;

            try {
                responseString = method.getResponseBodyAsString(2048);
            } catch (final IOException e) {
                throw new VersionControlException(
                    Messages.getString("VersionControlClient.CouldNotParseErrorMessageFromServer"), //$NON-NLS-1$
                    e);
            }

            if (responseString.length() > 0) {
                throw new VersionControlException(responseString);
            } else if (exceptionName != null) {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.ServerThrewExceptionForRequestFormat"), //$NON-NLS-1$
                        exceptionName,
                        request));
            }
        }

        throw new VersionControlException(
            MessageFormat.format(
                Messages.getString("VersionControlClient.ServerReturnedHTTPStatusForRequestFormat"), //$NON-NLS-1$
                Integer.toString(method.getStatusCode()),
                request));
    }

    /**
     * Finishes a file upload.
     *
     * @param method
     *        the object constructed by beginUploadRequest().
     */
    public void finishUploadRequest(final PostMethod method) {
        Check.notNull(method, "method"); //$NON-NLS-1$
        method.releaseConnection();
    }

    /*
     * Visual Studio's client object model has the older getChangeset() methods
     * calling the TFS 2005 queryChangeset() method, and newer getChangeset()
     * methods calling the TFS 2010 queryChangesetExtended().
     *
     * Since TEE uses default item filters for execute bit, we want callers to
     * be able to simply pass down filters and have the most capable method used
     * for the server version. All the getChangeset() methods in this class
     * detect the server version and call the most recent web service method.
     */

    /**
     * Gets {@link Changeset} information including individual change
     * information, excluding download information.
     */
    public Changeset getChangeset(final int changesetID) {
        return getChangeset(changesetID, true, false, null, null);
    }

    /**
     * Gets the {@link Changeset} object that describes the given changeset ID.
     *
     * @param changesetID
     *        the ID of the changeset to get information for.
     * @param includeChanges
     *        if <code>true</code> information about the individual changes in
     *        the changeset are returned
     * @param includeDownloadInfo
     *        if <code>true</code> the individual change items will have
     *        download URLs
     * @param changesetPropertyFilters
     *        a list of properties to return with the changeset (may be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of properties to return on the items in the changeset (may
     *        be <code>null</code>)
     * @return an {@link Changeset} that describes the changeset information.
     */
    public Changeset getChangeset(
        final int changesetID,
        final boolean includeChanges,
        final boolean includeDownloadInfo,
        final String[] changesetPropertyFilters,
        String[] itemPropertyFilters) {
        final Changeset changeset;
        if (getWebServiceLayer().getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
            // Mix these in before the test below so we hit the right service
            itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

            /*
             * Only available in TFS 2010 or newer, and server errors if given
             * null property filters.
             */
            changeset = getWebServiceLayer().queryChangesetExtended(
                changesetID,
                includeChanges,
                includeDownloadInfo,
                changesetPropertyFilters,
                null,
                itemPropertyFilters);
        } else {
            changeset = getWebServiceLayer().queryChangeset(changesetID, includeChanges, includeDownloadInfo, true);
        }

        // sort the changes array so that we have a consistent output
        if (includeChanges) {
            changeset.sortChanges();
        }

        return changeset;
    }

    /**
     * Gets the changes within a changeset Allows the caller to page changes
     * back from the server.
     *
     * @param changesetID
     *        the changeset for which to get changes
     * @param includeDownloadInfo
     *        If true, the server will include the information needed to
     *        download files. Only set this to true if you are going to be
     *        downloading the files using the objects that are returned. The
     *        call will be faster and require less bandwidth when this parameter
     *        is false.
     * @param pageSize
     *        the number of items to return
     * @param lastItem
     *        instructs the server to return items which sort after this item.
     *        If null, the server will begin from the start of the changeset.
     *        This parameter should be null in the first call to this method,
     *        and then should be the last seen value on subsequent calls
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each item (may
     *        be <code>null</code>)
     * @param includeMergeSourceInfo
     *        whether to include source information about renames / merges
     * @return the changes in the changeset
     */
    public Change[] getChangesForChangeset(
        final int changesetID,
        final boolean includeDownloadInfo,
        final int pageSize,
        final ItemSpec lastItem,
        String[] itemPropertyFilters,
        final boolean includeMergeSourceInfo) {
        itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        return webServiceLayer.queryChangesForChangeset(
            changesetID,
            includeDownloadInfo,
            pageSize,
            lastItem,
            null,
            itemPropertyFilters,
            includeMergeSourceInfo);
    }

    /*
     * Legacy getItem and getItems which use ItemID. Perhaps remove all these
     * methods?
     */

    public Item getItem(final int itemID, final int changesetNumber) {
        return this.getItem(itemID, changesetNumber, false);
    }

    public Item getItem(final int itemID, final int changesetNumber, final GetItemsOptions options) {
        return this.getItems(new int[] {
            itemID
        }, changesetNumber, options)[0];
    }

    public Item getItem(final int itemID, final int changesetNumber, final boolean includeDownloadInfo) {
        return this.getItems(new int[] {
            itemID
        }, changesetNumber, includeDownloadInfo)[0];
    }

    public Item[] getItems(final int[] itemIDs, final int changesetNumber) {
        return this.getItems(itemIDs, changesetNumber, GetItemsOptions.NONE);
    }

    public Item[] getItems(final int[] itemIDs, final int changesetNumber, final boolean includeDownloadInfo) {
        return this.getItems(
            itemIDs,
            changesetNumber,
            includeDownloadInfo ? GetItemsOptions.DOWNLOAD : GetItemsOptions.NONE);
    }

    public Item[] getItems(final int[] itemIDs, final int changesetNumber, final GetItemsOptions options) {
        Check.notNull(itemIDs, "itemIDs"); //$NON-NLS-1$

        for (int i = 0; i < itemIDs.length; i++) {
            if (changesetNumber == -1) {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.ItemWithIDCannotBeUsedWithChangesetNumberFormat"), //$NON-NLS-1$
                        Integer.toString(itemIDs[i]),
                        Integer.toString(changesetNumber)));
            }

            if (itemIDs[i] < 1) {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.ItemIDIsInvalidFormat"), //$NON-NLS-1$
                        Integer.toString(itemIDs[i])));
            }
        }

        if (changesetNumber <= 0) {
            throw new VersionControlException(
                MessageFormat.format(
                    Messages.getString("VersionControlClient.NotValidChangesetNumberPleaseSpecifyBetween1AndMaxFormat"), //$NON-NLS-1$
                    Integer.toString(changesetNumber),
                    Integer.toString(Changeset.MAX)));
        }

        return getWebServiceLayer().queryItemsByID(
            itemIDs,
            changesetNumber,
            options.contains(GetItemsOptions.DOWNLOAD),
            options);
    }

    /*
     * getItem and testItemExists
     */

    /**
     * @equivalence getItem(path, LatestVersionSpec.INSTANCE)
     */
    public Item getItem(final String path) {
        return getItem(path, LatestVersionSpec.INSTANCE);
    }

    /**
     * @equivalence getItem(path, version, 0)
     */
    public Item getItem(final String path, final VersionSpec version) {
        return getItem(path, version, 0);
    }

    /**
     * @equivalence getItem(path, version, deletedState, false)
     */
    public Item getItem(final String path, final VersionSpec version, final DeletedState deletedState) {
        return getItem(path, version, deletedState, false);
    }

    /**
     * @equivalence getItem(path, version, deletionId, false);
     */
    public Item getItem(final String path, final VersionSpec version, final int deletionID) {
        return getItem(path, version, deletionID, false);
    }

    /**
     * @equivalence getItem(path, version, deletedState, includeDownloadInfo ?
     *              GetItemsOptions.DOWNLOAD : GetItemsOptions.NONE)
     */
    public Item getItem(
        final String path,
        final VersionSpec version,
        final DeletedState deletedState,
        final boolean includeDownloadInfo) {
        return getItem(
            path,
            version,
            deletedState,
            includeDownloadInfo ? GetItemsOptions.DOWNLOAD : GetItemsOptions.NONE);
    }

    /**
     * @equivalence getItem(path, version, (deletionID > 0) ?
     *              DeletedState.DELETED : DeletedState.NON_DELETED, options)
     */
    public Item getItem(
        final String path,
        final VersionSpec version,
        final int deletionID,
        final GetItemsOptions options) {
        return getItem(path, version, (deletionID > 0) ? DeletedState.DELETED : DeletedState.NON_DELETED, options);
    }

    /**
     * @equivalence getItem(path, version, deletionID, includeDownloadInfo ?
     *              GetItemsOptions.DOWNLOAD : GetItemsOptions.NONE)
     */
    public Item getItem(
        final String path,
        final VersionSpec version,
        final int deletionID,
        final boolean includeDownloadInfo) {
        return getItem(
            path,
            version,
            deletionID,
            includeDownloadInfo ? GetItemsOptions.DOWNLOAD : GetItemsOptions.NONE);
    }

    /**
     * Gets information about one item.
     *
     * @param path
     *        the server or local path of the item to get information for (no
     *        wildcards) (must not be <code>null</code> or empty)
     * @param version
     *        the version of the item to get information about (must not be
     *        <code>null</code>)
     * @param options
     *        flags that control the amount of information returned for the item
     * @return an {@link Item} describing the given path, never
     *         <code>null</code> (throws {@link VersionControlException} if the
     *         item was not found)
     * @throws VersionControlException
     *         if wildcards were present in the path, or if the item was not
     *         found in the repository
     */
    public Item getItem(
        final String path,
        final VersionSpec version,
        final DeletedState deletedState,
        final GetItemsOptions options) {
        final Item ret = getItemInternal(path, version, deletedState, options);

        if (ret == null) {
            if (version == null || version.equals(LatestVersionSpec.INSTANCE)) {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.ItemWasNotFoundInRepositoryFormat"), //$NON-NLS-1$
                        path));
            }

            throw new VersionControlException(
                MessageFormat.format(
                    Messages.getString("VersionControlClient.ItemWasNotFoundInRepositoryAtSpecifiedVersionFormat"), //$NON-NLS-1$
                    path,
                    version.toString()));
        }

        return ret;
    }

    /**
     * @equivalence testItemExists(path, LatestVersionSpec.INSTANCE,
     *              DeletedState.NON_DELETED)
     */
    public boolean testItemExists(final String path) {
        return testItemExists(path, LatestVersionSpec.INSTANCE, DeletedState.NON_DELETED);
    }

    /**
     * @equivalence testItemExists(path, version, DeletedState.NON_DELETED)
     */
    public boolean testItemExists(final String path, final VersionSpec version) {
        return testItemExists(path, version, DeletedState.NON_DELETED);
    }

    /**
     * Tests whether an item exists in the repository at the given version.
     *
     * @param path
     *        the server or local path to test for existence (must not be
     *        <code>null</code> or empty)
     * @param version
     *        the version of the item to test for (must not be <code>null</code>
     *        )
     * @param deletedState
     *        the deleted state of the item to test for (must not be
     *        <code>null</code>)
     * @return true if the item exists at the given version and deleted state,
     *         false if it does not
     * @throws VersionControlException
     *         if the path contained wildcards
     */
    public boolean testItemExists(final String path, final VersionSpec version, final DeletedState deletedState) {
        return getItemInternal(path, version, deletedState, GetItemsOptions.NONE) != null;
    }

    /**
     * Gets information about an item, but does not throw if the item was not
     * found.
     */
    private Item getItemInternal(
        final String path,
        final VersionSpec version,
        final DeletedState deletedState,
        final GetItemsOptions options) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$

        if (Wildcard.isWildcard(path)) {
            throw new VersionControlException(Messages.getString("VersionControlClient.WildcardsAreNotAllowedInPath")); //$NON-NLS-1$
        }

        final ItemSet set =
            this.getItems(new ItemSpec(path, RecursionType.NONE), version, deletedState, ItemType.ANY, options);

        final Item[] items = set.getItems();

        if (items.length > 0) {
            return items[0];
        }

        return null;
    }

    /*
     * getItems
     */

    /**
     * @equivalence getItems(path, LatestVersionSpec.INSTANCE,
     *              RecursionType.NONE)
     */
    public ItemSet getItems(final String path) {
        return getItems(path, LatestVersionSpec.INSTANCE, RecursionType.NONE);
    }

    /**
     * @equivalence getItems(path, LatestVersionSpec.INSTANCE, recursion)
     */
    public ItemSet getItems(final String path, final RecursionType recursion) {
        return getItems(path, LatestVersionSpec.INSTANCE, recursion);
    }

    /**
     * @equivalence getItems(path, version, recursion, DeletedState.NON_DELETED,
     *              ItemType.ANY, false)
     */
    public ItemSet getItems(final String path, final VersionSpec version, final RecursionType recursion) {
        return getItems(path, version, recursion, DeletedState.NON_DELETED, ItemType.ANY, false);
    }

    /**
     * @equivalence getItems(itemSpecs, version, deletedState, itemType, false)
     */
    public ItemSet[] getItems(
        final ItemSpec[] itemSpecs,
        final VersionSpec version,
        final DeletedState deletedState,
        final ItemType itemType) {
        return getItems(itemSpecs, version, deletedState, itemType, false);
    }

    /**
     * @equivalence getItems(new ItemSpec[] { itemSpec }, version, deletedState,
     *              itemType, options)[0]
     */
    public ItemSet getItems(
        final ItemSpec itemSpec,
        final VersionSpec version,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        return getItems(new ItemSpec[] {
            itemSpec
        }, version, deletedState, itemType, options)[0];
    }

    /**
     * @equivalence getItems(new ItemSpec[] { itemSpec }, version, deletedState,
     *              itemType, includeDownloadInfo)[0]
     */
    public ItemSet getItems(
        final ItemSpec itemSpec,
        final VersionSpec version,
        final DeletedState deletedState,
        final ItemType itemType,
        final boolean includeDownloadInfo) {
        return getItems(new ItemSpec[] {
            itemSpec
        }, version, deletedState, itemType, includeDownloadInfo)[0];
    }

    /**
     * @equivalence getItems(itemSpecs, version, deletedState, itemType,
     *              includeDownloadInfo ? GetItemsOptions.DOWNLOAD :
     *              GetItemsOptions.NONE)
     */
    public ItemSet[] getItems(
        final ItemSpec[] itemSpecs,
        final VersionSpec version,
        final DeletedState deletedState,
        final ItemType itemType,
        final boolean includeDownloadInfo) {
        return getItems(
            itemSpecs,
            version,
            deletedState,
            itemType,
            includeDownloadInfo ? GetItemsOptions.DOWNLOAD : GetItemsOptions.NONE);
    }

    /**
     * @equivalence getItems(path, version, recursion, deletedState, itemType,
     *              false)
     */
    public ItemSet getItems(
        final String path,
        final VersionSpec version,
        final RecursionType recursion,
        final DeletedState deletedState,
        final ItemType itemType) {
        return getItems(path, version, recursion, deletedState, itemType, false);
    }

    /**
     * @equivalence getItems(new ItemSpec(path, recursion), version,
     *              deletedState, itemType, includeDownloadInfo)
     */
    public ItemSet getItems(
        final String path,
        final VersionSpec version,
        final RecursionType recursion,
        final DeletedState deletedState,
        final ItemType itemType,
        final boolean includeDownloadInfo) {
        return getItems(new ItemSpec(path, recursion), version, deletedState, itemType, includeDownloadInfo);
    }

    /**
     * @equivalence getItems(itemSpecs, version, deletedState, itemType,
     *              options, null)
     */
    public ItemSet[] getItems(
        final ItemSpec[] itemSpecs,
        final VersionSpec version,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        return getItems(itemSpecs, version, deletedState, itemType, options, null);
    }

    /**
     * Gets information about multiple items. An {@link ItemSet} is returned for
     * each query item.
     *
     * @param itemSpecs
     *        the items to get information about (must not be <code>null</code>
     *        or empty)
     * @param version
     *        the version of each of the items to get information about (not
     *        null)
     * @param deletedState
     *        the deleted state of items (must not be <code>null</code>)
     * @param itemType
     *        the types of matching items that should be returned (must not be
     *        <code>null</code>)
     * @param options
     *        flags that control the amount of information returned for the item
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each item (may
     *        be <code>null</code>)
     * @return an array of {@link ItemSet} instances, one for each given
     *         {@link ItemSpec} in the original order. May be empty but never
     *         null.
     */
    public ItemSet[] getItems(
        final ItemSpec[] itemSpecs,
        final VersionSpec version,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options,
        String[] itemPropertyFilters) {
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(itemSpecs, workspaceName, workspaceOwner);

        final ItemSet[] sets = getWebServiceLayer().queryItems(
            workspaceName.get(),
            workspaceOwner.get(),
            itemSpecs,
            version,
            deletedState,
            itemType,
            options.contains(GetItemsOptions.DOWNLOAD),
            options,
            itemPropertyFilters,
            null);

        /*
         * If the server did not sort the set contents, sort them locally.
         */
        if (options.contains(GetItemsOptions.UNSORTED) == false) {
            for (int i = 0; i < sets.length; i++) {
                /*
                 * Sort a temporary copy (using the Item sort logic) and set it
                 * back on the object.
                 */
                final Item[] items = sets[i].getItems();

                Arrays.sort(items);

                sets[i].setItems(items);
            }
        }

        return sets;
    }

    /*
     * getExtendedItems
     */

    /**
     * @equivalence getExtendedItems(itemSpecs, deletedState, itemType,
     *              GetItemsOptions.NONE)
     */
    public ExtendedItem[][] getExtendedItems(
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType) {
        return getExtendedItems(itemSpecs, deletedState, itemType, GetItemsOptions.NONE);
    }

    /**
     * @equivalence getExtendedItems(null, null, itemSpecs, deletedState,
     *              itemType, options)
     */
    public ExtendedItem[][] getExtendedItems(
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        return getExtendedItems(null, null, itemSpecs, deletedState, itemType, options);
    }

    /**
     * @equivalence getExtendedItems(new ItemSpec[] { new ItemSpec(itemPath,
     *              RecursionType.NONE) }, deletedState, itemType,
     *              GetItemsOptions.NONE)[0]
     */
    public ExtendedItem[] getExtendedItems(
        final String itemPath,
        final DeletedState deletedState,
        final ItemType itemType) {
        return getExtendedItems(new ItemSpec[] {
            new ItemSpec(itemPath, RecursionType.NONE)
        }, deletedState, itemType, GetItemsOptions.NONE)[0];
    }

    /**
     * @equivalence getExtendedItems(workspaceName, workspaceOwner, itemSpecs,
     *              deletedState, itemType, options, null)
     */
    public ExtendedItem[][] getExtendedItems(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        return getExtendedItems(workspaceName, workspaceOwner, itemSpecs, deletedState, itemType, options, null);
    }

    /**
     * Gets extended information about items, with full results control.
     *
     * @param workspaceName
     *        the name of the workspace to get extended information for. If the
     *        paths in the itemSpecs are server paths, this parameter is ignored
     *        (may be null). If this parameter is null and the itemSpec paths
     *        are local, the correct local workspace is determined
     *        automatically.
     * @param workspaceOwner
     *        the owner of the workspace to get extended information for. If the
     *        paths in the itemSpecs are server paths, this parameter is ignored
     *        (may be null). If this parameter is null and the itemSpec paths
     *        are local, the correct local workspace is determined
     *        automatically.
     * @param itemSpecs
     *        instances of {@link ItemSpec} that describe the item sets you want
     *        returned. One {@link ItemSet} will be returned for each
     *        {@link ItemSpec} (must not be <code>null</code> or empty)
     * @param deletedState
     *        the deleted state of items you want to list (must not be
     *        <code>null</code>)
     * @param itemType
     *        the types of items you want to list (must not be <code>null</code>
     *        )
     * @param options
     *        the {@link GetItemsOptions} which control the returned results
     *        (must not be <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each extended
     *        item (may be <code>null</code>)
     * @return an array of {@link ExtendedItem} arrays, each outer array
     *         representing one given {@link ItemSpec}, and each inner array
     *         representing the matches found for those {@link ItemSpec}s
     *         (should be only one object in these inner arrays because
     *         recursion is not an option). Inner arrays may be empty but are
     *         never null.
     */
    public ExtendedItem[][] getExtendedItems(
        String workspaceName,
        String workspaceOwner,
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options,
        String[] itemPropertyFilters) {
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(deletedState, "deletedState"); //$NON-NLS-1$
        Check.notNull(itemType, "itemType"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        if (options.contains(GetItemsOptions.UNSORTED) || options.contains(GetItemsOptions.DOWNLOAD)) {
            throw new VersionControlException(
                MessageFormat.format(
                    Messages.getString("VersionControlClient.TheUnsortedAndDownloadOptionsAreNotAllowedFormat"), //$NON-NLS-1$
                    GetItemsOptions.UNSORTED.toString(),
                    GetItemsOptions.DOWNLOAD.toString()));
        }

        /*
         * Detect the correct local workspace if none was supplied. Only works
         * if the paths are local paths. For server paths, a null name and owner
         * is returned, and sending that to the server results in less
         * information returned (no version info, etc.).
         */
        if (workspaceName == null && workspaceOwner == null) {
            final AtomicReference<String> workspaceNameHolder = new AtomicReference<String>();
            final AtomicReference<String> workspaceOwnerHolder = new AtomicReference<String>();

            determineWorkspaceNameAndOwner(itemSpecs, workspaceNameHolder, workspaceOwnerHolder);

            workspaceName = workspaceNameHolder.get();
            workspaceOwner = workspaceOwnerHolder.get();
        }

        return getWebServiceLayer().queryItemsExtended(
            workspaceName,
            workspaceOwner,
            itemSpecs,
            deletedState,
            itemType,
            options,
            itemPropertyFilters);
    }

    /**
     * For the passed array of server paths, work out the list of Team Projects
     * associated with those items then query the server for the Checkin Note
     * Field Definitions for those projects.
     *
     * @param serverPaths
     *        Sting array containing paths to server items that the calling
     *        method requires the checkin note field definitions.
     * @return SortedSet of unique ACheckinNoteFieldDefinition objects in
     *         display order.
     */
    public final SortedSet<CheckinNoteFieldDefinition> queryCheckinNoteFieldDefinitionsForServerPaths(
        final String[] serverPaths) {
        Check.notNullOrEmpty(serverPaths, "serverPaths"); //$NON-NLS-1$

        // Calculate unqiue list of Team Projects from itemPaths.
        final String[] projects = ServerPath.getTeamProjects(serverPaths);

        final CheckinNoteFieldDefinition[] definitions = getWebServiceLayer().queryCheckinNoteDefinition(projects);

        final TreeSet<CheckinNoteFieldDefinition> unique = new TreeSet<CheckinNoteFieldDefinition>();

        if (definitions != null) {
            for (final CheckinNoteFieldDefinition definition : definitions) {
                unique.add(definition);
            }
        }

        return unique;
    }

    /**
     * Set the exclusive checkout property on a project.
     *
     * @param serverPath
     *        server path of project or file in project (must not be
     *        <code>null</code> or empty).
     * @param exclusiveCheckout
     *        flag indicating if force exclusive checkout should be enabled or
     *        disabled.
     */
    public final void setExclusiveCheckout(final String serverPath, final boolean exclusiveCheckout) {
        Check.notNullOrEmpty(serverPath, "serverPath"); //$NON-NLS-1$

        final String project = ServerPath.getTeamProject(serverPath);

        getWebServiceLayer().createAnnotation(
            VersionControlConstants.EXCLUSIVE_CHECKOUT_ANNOTATION,
            project,
            0,
            Boolean.toString(exclusiveCheckout),
            null,
            true);
    }

    /**
     * Gets the value associated with the given annotation name for the given
     * item at the given version. Either annotationName or annotatedServerItem,
     * or both, must be supplied (must not be <code>null</code>)
     *
     * @param annotationName
     *        the name of the annotation to query. If null, annotatedServerItem
     *        must not be null, and all annotations for the given server item
     *        and version are returned.
     * @param annotatedServerItem
     *        the server path of the item to query annotations for. If null,
     *        annotationName must not be null, and all annotations for all
     *        server items that match the given annotation name and version will
     *        be returned.
     * @param version
     *        the version of the given annotatedServerItem to query annotations
     *        for. If 0, annotations that are not attached to any version of
     *        that item are returned.
     * @return the annotations that matched the query. May be empty but never
     *         null.
     */
    public Annotation[] queryAnnotation(
        final String annotationName,
        final String annotatedServerItem,
        final int version) {
        Check.isTrue(
            annotationName != null || annotatedServerItem != null,
            "one of annotationName or annotatedServerItem must not be null"); //$NON-NLS-1$

        return getWebServiceLayer().queryAnnotation(annotationName, annotatedServerItem, version);
    }

    /**
     * Creates an annotation.
     *
     * @param annotationName
     *        the name of the annotation to create. If null, annotatedServerItem
     *        must not be null, and an annotation for the given server item and
     *        version is created.
     * @param annotatedServerItem
     *        the server path of the item to create an annotation on (must not
     *        be <code>null</code>) deleted.
     * @param version
     *        the version of the given annotatedServerItem to create. If 0, an
     *        annotation that is not attached to any version of that item is
     *        created.
     * @param annotationValue
     *        the value to store in the annotation. Can be a very large string
     *        (must not be <code>null</code>)
     * @param comment
     *        an optional comment (may be null).
     * @param overwrite
     *        true to overwrite an existing annotation with the same location
     *        information, false to error if one already exists.
     */
    public void createAnnotation(
        final String annotationName,
        final String annotatedServerItem,
        final int version,
        final String annotationValue,
        final String comment,
        final boolean overwrite) {
        Check.notNull(annotatedServerItem, "annotatedServerItem"); //$NON-NLS-1$
        Check.notNull(annotationValue, "annotationValue"); //$NON-NLS-1$

        getWebServiceLayer().createAnnotation(
            annotationName,
            annotatedServerItem,
            version,
            annotationValue,
            comment,
            overwrite);
    }

    /**
     * Deletes an annotation.
     *
     * @param annotationName
     *        the name of the annotation to delete. If null, annotatedServerItem
     *        must not be null, and all annotations for the given server item
     *        and version are deleted.
     * @param annotatedServerItem
     *        the server path of the item to delete annotations on. If null,
     *        annotationName must not be null, and all annotations for all
     *        server items that match the given annotation name and version will
     *        be deleted.
     * @param version
     *        the version of the given annotatedServerItem to delete annotations
     *        for. If 0, annotations that are not attached to any version of
     *        that item are deleted.
     * @param annotationValue
     *        the value (may be <code>null</code>)
     */
    public void deleteAnnotation(
        final String annotationName,
        final String annotatedServerItem,
        final int version,
        final String annotationValue) {
        Check.isTrue(
            annotationName != null || annotatedServerItem != null,
            "one of annotationName or annotatedServerItem must not be null"); //$NON-NLS-1$

        getWebServiceLayer().deleteAnnotation(annotationName, annotatedServerItem, version, annotationValue);
    }

    public TFSTeamProjectCollection getConnection() {
        return connection;
    }

    /**
     * Queries the server for history about an item. History items are returned
     * as an array of changesets.
     *
     * @param serverOrLocalPath
     *        the server or local path to the server item being queried for its
     *        history (must not be <code>null</code> or empty).
     * @param version
     *        the version of the item to query history for (history older than
     *        this version will be returned) (must not be <code>null</code>)
     * @param deletionID
     *        the deletion ID for the item, if it is a deleted item (pass 0 if
     *        the item is not deleted).
     * @param recursion
     *        whether to query recursively (must not be <code>null</code>)
     * @param user
     *        only include historical changes made by this user (pass null to
     *        retrieve changes made by all users).
     * @param versionFrom
     *        the beginning version to query historical changes from (pass null
     *        to start at the first version).
     * @param versionTo
     *        the ending version to query historical changes to (pass null to
     *        end at the most recent version).
     * @param maxCount
     *        the maximum number of changes to return (pass Integer.MAX_VALUE
     *        for all available values). Must be > 0.
     * @param includeFileDetails
     *        true to include individual file change details with the returned
     *        results, false to return only general changeset information.
     * @param slotMode
     *        if true, all items that have occupied the given serverPath (during
     *        different times) will have their changes returned. If false, only
     *        the item that matches that path at the given version will have its
     *        changes returned.
     * @param sortAscending
     *        when <code>true</code> gets the top maxCount changes in ascending
     *        order, when <code>false</code> gets them in descending order
     * @return the changesets that matched the history query, null if the server
     *         did not return a changeset array.
     */
    public Changeset[] queryHistory(
        final String serverOrLocalPath,
        final VersionSpec version,
        final int deletionID,
        final RecursionType recursion,
        final String user,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        int maxCount,
        final boolean includeFileDetails,
        final boolean slotMode,
        final boolean includeDownloadInfo,
        final boolean sortAscending) throws ServerPathFormatException {
        Check.notNullOrEmpty(serverOrLocalPath, "serverOrLocalPath"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$
        Check.isTrue(maxCount > 0, "maxCount must be greater than 0"); //$NON-NLS-1$

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(serverOrLocalPath, workspaceName, workspaceOwner);

        /*
         * Local paths require valid workspace name and owner.
         */
        if ((workspaceName.get() == null || workspaceOwner.get() == null)
            && ServerPath.isServerPath(serverOrLocalPath) == false) {
            Check.isTrue(
                false,
                MessageFormat.format("Could not determine the workspace for local path {0}", serverOrLocalPath)); //$NON-NLS-1$
        }

        final ItemSpec spec = new ItemSpec(serverOrLocalPath, recursion, deletionID);

        final VersionSpec fromSpec = (versionFrom != null) ? versionFrom : new ChangesetVersionSpec(1);

        VersionSpec endVersion = versionTo;

        final ArrayList<Changeset> foundChangeSets = new ArrayList<Changeset>();

        /*
         * Changesets come in newest first (reverse chronological order).
         *
         * Fetch the changesets in chunks of HISTORY_ITEMS_CHUNK_SIZE. If we've
         * hit maxCount, or we've hit the first changeset (number 1), or the
         * server sent us fewer changesets than we expected (including 0), we
         * quit and return the results to the user.
         *
         * We decrement maxCount as our counter. chunk is kept just for tracing.
         */
        int chunk = 1;
        while (maxCount > 0) {
            if (log.isDebugEnabled()) {
                log.debug(MessageFormat.format(
                    "chunk {0}, maxCount {1}", //$NON-NLS-1$
                    Integer.toString(chunk++),
                    Integer.toString(maxCount)));
            }

            // NOTE chunk is just for tracing, it only gets incremented when
            // debug is enabled.

            final int requestedCount = Math.min(maxCount, HISTORY_ITEMS_CHUNK_SIZE);

            if (log.isDebugEnabled()) {
                log.debug(MessageFormat.format("requestedCount {0}", Integer.toString(requestedCount))); //$NON-NLS-1$
            }

            if (log.isDebugEnabled()) {
                log.debug(MessageFormat.format("requesting from {0} to {1}", ((fromSpec != null) ? fromSpec.toString() //$NON-NLS-1$
                    : "null"), ((endVersion != null) ? endVersion.toString() : "null"))); //$NON-NLS-1$ //$NON-NLS-2$
            }

            final Changeset[] sets = getWebServiceLayer().queryHistory(
                workspaceName.get(),
                workspaceOwner.get(),
                spec,
                version,
                user,
                fromSpec,
                endVersion,
                requestedCount,
                includeFileDetails,
                includeDownloadInfo,
                slotMode,
                sortAscending);

            if (sets == null) {
                log.debug("got null history sets"); //$NON-NLS-1$
                return null;
            }

            if (sets.length == 0) {
                log.debug("got empty history sets"); //$NON-NLS-1$
                break;
            }

            // if the user asked for the changes, sort them
            if (includeFileDetails) {
                for (final Changeset set : sets) {
                    set.sortChanges();
                }
            }

            // Add all to the list
            foundChangeSets.addAll(Arrays.asList(sets));

            if (log.isDebugEnabled()) {
                log.debug(MessageFormat.format("got {0} history sets", sets.length)); //$NON-NLS-1$
            }

            /*
             * If the server sent us fewer than we had hoped, we should break,
             * because we're done.
             */
            if (sets.length < requestedCount) {
                log.debug("got less than a full chunk so exiting"); //$NON-NLS-1$
                break;
            }

            /*
             * Save the change set number of the last item in the list (which is
             * the oldest, chronologically), so we can create the end version
             * spec for the next time through the loop.
             */
            final int lastChangesetID = sets[sets.length - 1].getChangesetID();

            /*
             * This check saves us from asking for any more changesets when we
             * know we're done.
             */
            if (lastChangesetID == 1) {
                break;
            }

            /*
             * Decrement the counter by the number we just got, and set up the
             * end version for the next query.
             */
            maxCount -= sets.length;
            endVersion = new ChangesetVersionSpec(lastChangesetID - 1);
        }

        return foundChangeSets.toArray(new Changeset[foundChangeSets.size()]);
    }

    /**
     * Queries the server for history about an item. Results are returned as an
     * {@link Iterator} of {@link Changeset}s.
     *
     * @param serverOrLocalPath
     *        the server or local path to the server item being queried for its
     *        history (must not be <code>null</code> or empty).
     * @param version
     *        the version of the item to query history for (history older than
     *        this version will be returned) (must not be <code>null</code>)
     * @param deletionID
     *        the deletion ID for the item, if it is a deleted item (pass 0 if
     *        the item is not deleted).
     * @param recursion
     *        whether to query recursively (must not be <code>null</code>)
     * @param user
     *        only include historical changes made by this user (pass null to
     *        retrieve changes made by all users).
     * @param versionFrom
     *        the beginning version to query historical changes from (pass null
     *        to start at the first version).
     * @param versionTo
     *        the ending version to query historical changes to (pass null to
     *        end at the most recent version).
     * @param maxCount
     *        the maximum number of changes to return (pass Integer.MAX_VALUE
     *        for all available values). Must be > 0.
     * @param includeFileDetails
     *        true to include individual file change details with the returned
     *        results, false to return only general changeset information.
     * @param slotMode
     *        if true, all items that have occupied the given serverPath (during
     *        different times) will have their changes returned. If false, only
     *        the item that matches that path at the given version will have its
     *        changes returned.
     * @param sortAscending
     *        when <code>true</code> gets the top maxCount changes in ascending
     *        order, when <code>false</code> gets them in descending order
     * @return the changesets that matched the history query, null if the server
     *         did not return a changeset array.
     */
    public Iterator<Changeset> queryHistoryIterator(
        final String serverOrLocalPath,
        final VersionSpec version,
        final int deletionID,
        final RecursionType recursion,
        final String user,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final int maxCount,
        final boolean includeFileDetails,
        final boolean slotMode,
        final boolean includeDownloadInfo,
        final boolean sortAscending) throws ServerPathFormatException {
        Check.notNullOrEmpty(serverOrLocalPath, "serverOrLocalPath"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$
        Check.isTrue(maxCount > 0, "maxCount must be greater than 0"); //$NON-NLS-1$

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(serverOrLocalPath, workspaceName, workspaceOwner);

        /*
         * Local paths require valid workspace name and owner.
         */
        if ((workspaceName.get() == null || workspaceOwner.get() == null)
            && ServerPath.isServerPath(serverOrLocalPath) == false) {
            Check.isTrue(
                false,
                MessageFormat.format("Could not determine the workspace for local path {0}", serverOrLocalPath)); //$NON-NLS-1$
        }

        final ItemSpec itemSpec = new ItemSpec(serverOrLocalPath, recursion, deletionID);

        final HistoryIterator iterator = new HistoryIterator(
            getWebServiceLayer(),
            workspaceName.get(),
            workspaceOwner.get(),
            itemSpec,
            version,
            user,
            versionFrom,
            versionTo,
            maxCount,
            includeFileDetails,
            includeDownloadInfo,
            slotMode,
            sortAscending);

        iterator.prime();

        return iterator;
    }

    /**
     * Creates a branch of the given source path at the given version to the
     * given target path.
     * <p>
     * <b>Note</b>
     * <p>
     * This method exists as a fast alternative to the standard method of
     * creating a branch: by pending a branch change, then checking in those
     * changes.
     * </p>
     * <p>
     * Only server paths are accepted.
     * </p>
     * <p>
     * <em>This method is only supported by Team Foundation Server 2008 SP1 and
     * later.</em>
     * </p>
     *
     * @param sourceServerPath
     *        the server path to an existing item which will be branched (must
     *        not be <code>null</code>)
     * @param targetServerPath
     *        the server path to the destination where the branch will be made
     *        (not null).
     * @param version
     *        the version of the source item to branch into the target item (not
     *        null).
     * @return the changeset identifier that resulted from the check-in.
     * @throws UnsupportedOperationException
     *         if this method is used when connected to a server that does not
     *         support branch creation without pending changes.
     */
    public int createBranch(final String sourceServerPath, final String targetServerPath, final VersionSpec version) {
        return createBranch(sourceServerPath, targetServerPath, version, null, null, null, null, null);
    }

    /**
     * Creates a branch of the given source path at the given version to the
     * given target path.
     * <p>
     * <b>Note</b>
     * <p>
     * This method exists as a fast alternative to the standard method of
     * creating a branch: by pending a branch change, then checking in those
     * changes.
     * </p>
     * <p>
     * Only server paths are accepted.
     * </p>
     * <p>
     * <em>This method is only supported by Team Foundation Server 2008 SP1 and
     * later.</em>
     * </p>
     *
     * @param sourceServerPath
     *        the server path to an existing item which will be branched (must
     *        not be <code>null</code>)
     * @param targetServerPath
     *        the server path to the destination where the branch will be made
     *        (not null).
     * @param version
     *        the version of the source item to branch into the target item (not
     *        null).
     * @param owner
     *        the owner of the changeset that creates the branch (may be null)
     * @param comment
     *        the comment for the changeset that creates the branch (may be
     *        null)
     * @param checkinNote
     *        the checkin note provided with the changeset that creates the
     *        branch (may be null)
     * @param policyOverride
     *        the check-in policy override information for the changeset that
     *        creates the branch (may be null)
     * @param mappings
     *        the mappings to use for the branch (may be null)
     * @return the changeset identifier that resulted from the check-in.
     * @throws UnsupportedOperationException
     *         if this method is used when connected to a server that does not
     *         support branch creation without pending changes.
     */
    public int createBranch(
        final String sourceServerPath,
        final String targetServerPath,
        final VersionSpec version,
        final String owner,
        final String comment,
        final CheckinNote checkinNote,
        final PolicyOverrideInfo policyOverride,
        final Mapping[] mappings) {
        Check.notNull(sourceServerPath, "sourcePath"); //$NON-NLS-1$
        Check.notNull(targetServerPath, "targetPath"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$
        Check.isTrue(ServerPath.isServerPath(sourceServerPath), "source path must be a server path"); //$NON-NLS-1$
        Check.isTrue(ServerPath.isServerPath(targetServerPath), "target path must be a server path"); //$NON-NLS-1$

        final Changeset changeset = new Changeset(owner, comment, checkinNote, policyOverride);

        final AtomicReference<Failure[]> failures = new AtomicReference<Failure[]>();

        /*
         * TODO test the wrapper for mappings for 2010. This needs testing
         * because _Mapping is a base class and has a private
         * getWebServiceObject(), but since it's abstract, all derived classes
         * will have a real getWebServiceObject() and the wrapper utils should
         * invoke the derived method.
         */
        final CheckinResult result = getWebServiceLayer().createBranch(
            sourceServerPath,
            targetServerPath,
            version,
            changeset,
            null,
            mappings,
            failures);

        // TODO call ReportCheckinConflictsFailuresAndThrow

        if (result.getChangeset() > 0) {
            eventEngine.fireBranchCommitted(
                new BranchCommittedEvent(
                    EventSource.newFromHere(),
                    sourceServerPath,
                    targetServerPath,
                    version,
                    owner,
                    comment,
                    checkinNote,
                    policyOverride,
                    mappings,
                    result.getChangeset()));

            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).notifyForFolderContentChanged(
                this,
                result.getChangeset());
        }

        return result.getChangeset();
    }

    /**
     * Updates a changeset's information with the comment and check-in notes in
     * the given {@link Changeset}.
     *
     * @param changeset
     *        the new changeset information, where the changeset ID number
     *        matches an existing changeset (must not be <code>null</code>)
     */
    public void updateChangeset(final Changeset changeset) {
        Check.notNull(changeset, "changeset"); //$NON-NLS-1$

        getWebServiceLayer().updateChangeset(
            changeset.getChangesetID(),
            changeset.getComment(),
            changeset.getCheckinNote());
    }

    /**
     * Create or update a label for items in this workspace.
     *
     * @param label
     *        the label to create or update (must not be <code>null</code>)
     * @param items
     *        the items to be included in the label creation or update (not
     *        null).
     * @param options
     *        options that affect the processing of the label creation or update
     *        (must not be <code>null</code> or empty).
     * @return the label results, null if none were returned. May be empty but
     *         never null.
     */
    public LabelResult[] createLabel(
        final VersionControlLabel label,
        final LabelItemSpec[] items,
        final LabelChildOption options) {
        Check.notNull(label, "label"); //$NON-NLS-1$
        Check.notNull(items, "items"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(items, workspaceName, workspaceOwner);

        /*
         * Resolve authorized user name (bug 5743)
         */
        label.setOwner(
            IdentityHelper.getUniqueNameIfCurrentUser(getConnection().getAuthorizedIdentity(), label.getOwner()));

        final AtomicReference<Failure[]> failuresHolder = new AtomicReference<Failure[]>();

        final LabelResult[] ret = getWebServiceLayer().labelItem(
            workspaceName.get(),
            workspaceOwner.get(),
            label,
            items,
            options,
            failuresHolder);

        reportFailures(failuresHolder.get());

        return ret;
    }

    /**
     * Deletes the specified label.
     *
     * @param label
     *        the label to delete (must not be <code>null</code>)
     * @param scope
     *        the scope of the label to delete (must not be <code>null</code>)
     * @return the result of the label deletion. May be empty but never null.
     */
    public LabelResult[] deleteLabel(final String label, final String scope) {
        Check.notNull(label, "label"); //$NON-NLS-1$
        Check.notNull(scope, "scope"); //$NON-NLS-1$

        return getWebServiceLayer().deleteLabel(label, scope);
    }

    /**
     * Removes a label that was applied to an item.
     *
     * @param label
     *        the label to remove (must not be <code>null</code>)
     * @param scope
     *        the scope of the label to remove (must not be <code>null</code>)
     * @param items
     *        the items to remove from the label (must not be <code>null</code>
     *        or empty)
     * @param version
     *        the version of the items to remove to match (may be
     *        <code>null</code>)
     * @return the label results, null if none were returned. May be empty but
     *         never null.
     */
    public LabelResult[] unlabelItem(
        final String label,
        final String scope,
        final ItemSpec[] items,
        final VersionSpec version) {
        Check.notNull(label, "label"); //$NON-NLS-1$
        Check.notNullOrEmpty(items, "items"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(items, workspaceName, workspaceOwner);

        final AtomicReference<Failure[]> failuresHolder = new AtomicReference<Failure[]>();

        final LabelResult[] results = getWebServiceLayer().unlabelItem(
            workspaceName.get(),
            workspaceOwner.get(),
            label,
            scope,
            items,
            version,
            failuresHolder);

        reportFailures(failuresHolder.get());

        return results;
    }

    /**
     * Query the collection of labels that match the given specifications.
     *
     * @param label
     *        the label name to match (may be null?).
     * @param scope
     *        the scope of the label to match (may be null?).
     * @param owner
     *        the owner of the label to match (may be null?).
     * @param includeItemDetails
     *        if true, details about the labeled items are included in the
     *        results, otherwise only general label information is included.
     * @param filterItem
     *        if not <code>null</code>, only labels containing this item are
     *        returned.
     * @param filterItemVersion
     *        if filterItem was supplied, only labels that include this version
     *        of the filterItem are returned, otherwise may be null.
     * @return the label items that matched the query. May be empty but never
     *         null.
     */
    public VersionControlLabel[] queryLabels(
        final String label,
        final String scope,
        final String owner,
        final boolean includeItemDetails,
        final String filterItem,
        final VersionSpec filterItemVersion) {
        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(filterItem, workspaceName, workspaceOwner);

        if (filterItem != null
            && (workspaceName.get() == null || workspaceOwner.get() == null)
            && ServerPath.isServerPath(filterItem) == false) {
            Check.isTrue(
                false,
                MessageFormat.format("Could not determine the workspace for local path {0}", filterItem)); //$NON-NLS-1$
        }

        return getWebServiceLayer().queryLabels(
            workspaceName.get(),
            workspaceOwner.get(),
            label,
            scope,
            owner,
            filterItem,
            filterItemVersion,
            includeItemDetails,
            true);
    }

    /**
     * @equivalence queryPendingSets( serverOrLocalPaths, recursionType,
     *              includeDownloadInfo, queryWorkspaceName,
     *              queryWorkspaceOwner, null)
     */
    public PendingSet[] queryPendingSets(
        final String[] serverOrLocalPaths,
        final RecursionType recursionType,
        final boolean includeDownloadInfo,
        final String queryWorkspaceName,
        final String queryWorkspaceOwner) {
        return queryPendingSets(
            serverOrLocalPaths,
            recursionType,
            includeDownloadInfo,
            queryWorkspaceName,
            queryWorkspaceOwner,
            null);
    }

    /**
     * Get pending changes for the given item paths, even from another user's
     * workspace.
     *
     * @param serverOrLocalPaths
     *        the items (files or directories) to get pending changes for. Pass
     *        {@link ServerPath#ROOT} with {@link RecursionType#FULL} to match
     *        all (must not be <code>null</code> or empty)
     * @param recursionType
     *        the type of recursion to apply to the given server paths (must not
     *        be <code>null</code>)
     * @param queryWorkspaceName
     *        the name of the workspace to query for pending changes. Pass
     *        <code>null</code> to match all.
     * @param queryWorkspaceOwner
     *        the owner of the workspace to query for pending changes. Pass
     *        <code>null</code> to match all.
     * @param itemPropertyFilters
     *        a list of property names to return on the pending change object if
     *        they exist (may be <code>null</code>)
     * @return a pending set including all the pending changes. May be empty but
     *         never <code>null</code>.
     */
    public PendingSet[] queryPendingSets(
        final String[] serverOrLocalPaths,
        final RecursionType recursionType,
        final boolean includeDownloadInfo,
        final String queryWorkspaceName,
        final String queryWorkspaceOwner,
        String[] itemPropertyFilters) {
        Check.notNullOrEmpty(serverOrLocalPaths, "serverOrLocalPaths"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$

        itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        final ItemSpec[] specs = new ItemSpec[serverOrLocalPaths.length];
        for (int i = 0; i < serverOrLocalPaths.length; i++) {
            specs[i] = new ItemSpec(serverOrLocalPaths[i], recursionType, 0);
        }

        return queryPendingSets(
            specs,
            includeDownloadInfo,
            queryWorkspaceName,
            queryWorkspaceOwner,
            false,
            itemPropertyFilters);
    }

    /**
     * @equivalence queryPendingSets(itemSpecs, includeDownloadInfo,
     *              queryWorkspaceName, queryWorkspaceOwner, false)
     */
    public PendingSet[] queryPendingSets(
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo,
        final String queryWorkspaceName,
        final String queryWorkspaceOwner) {
        return queryPendingSets(itemSpecs, includeDownloadInfo, queryWorkspaceName, queryWorkspaceOwner, false);
    }

    /**
     * @equivalence queryPendingSets( itemSpecs, includeDownloadInfo,
     *              queryWorkspaceName, queryWorkspaceOwner, includeCandidates,
     *              null)
     */
    public PendingSet[] queryPendingSets(
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo,
        final String queryWorkspaceName,
        final String queryWorkspaceOwner,
        final boolean includeCandidates) {
        return queryPendingSets(
            itemSpecs,
            includeDownloadInfo,
            queryWorkspaceName,
            queryWorkspaceOwner,
            includeCandidates,
            null);
    }

    /**
     * Get pending changes for the given item specs, even from another user's
     * workspace.
     *
     * @param itemSpecs
     *        the ItemSpecs to get pending changes for (must not be
     *        <code>null</code> or empty)
     * @param queryWorkspaceName
     *        the name of the workspace to query for pending changes. Pass null
     *        to match all.
     * @param queryWorkspaceOwner
     *        the owner of the workspace to query for pending changes. Pass null
     *        to match all.
     * @param includeCandidates
     *        if <code>true</code> for a local workspace, candidate changes will
     *        be populated on the pending set. A pending set will be returned if
     *        the workspace contains pending changes or candidate changes
     * @param itemPropertyFilters
     *        a list of property names to return on the pending change object if
     *        they exist (may be <code>null</code>)
     * @return a pending set including all the pending changes. May be empty but
     *         never null.
     */
    public PendingSet[] queryPendingSets(
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo,
        final String queryWorkspaceName,
        final String queryWorkspaceOwner,
        final boolean includeCandidates,
        String[] itemPropertyFilters) {
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(itemSpecs, workspaceName, workspaceOwner);

        final AtomicReference<Failure[]> failures = new AtomicReference<Failure[]>();

        final PendingSet[] ret = getWebServiceLayer().queryPendingSets(
            workspaceName.get(),
            workspaceOwner.get(),
            queryWorkspaceName,
            queryWorkspaceOwner,
            itemSpecs,
            includeDownloadInfo,
            failures,
            includeCandidates,
            itemPropertyFilters);

        reportFailures(failures.get());

        return ret;
    }

    /**
     * @equivalence queryShelvedChanges(shelvesetName, shelvesetOwner,
     *              itemSpecs, includeDownloadInfo, null)
     */
    public PendingSet[] queryShelvedChanges(
        final String shelvesetName,
        final String shelvesetOwner,
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo) {
        return queryShelvedChanges(shelvesetName, shelvesetOwner, itemSpecs, includeDownloadInfo, null);
    }

    /**
     * Gets the shelved changes for the given item specs.
     *
     * @param shelvesetName
     *        the shelveset name (may be null)
     * @param shelvesetOwner
     *        the user name (may be null), which is the shelveset owner if the
     *        shelveset name is not <code>null</code>
     * @param itemSpecs
     *        the items to query changes for (null for all).
     * @param includeDownloadInfo
     *        If true, the server will include the information needed to
     *        download files. Only set this to true if you are going to be
     *        downloading the files using the objects that are returned. The
     *        call will be faster and require less bandwidth when this parameter
     *        is false (default for overloads that don't specify it)
     * @param itemPropertyFilters
     *        a list of property names to return on the pending change object if
     *        they exist (may be <code>null</code>)
     * @return an array of pending sets with the pending changes for the shelved
     *         changes. May be empty but never null.
     */
    public PendingSet[] queryShelvedChanges(
        final String shelvesetName,
        final String shelvesetOwner,
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo,
        String[] itemPropertyFilters) {
        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(itemSpecs, workspaceName, workspaceOwner);

        itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        return queryShelvedChanges(
            workspaceName.get(),
            workspaceOwner.get(),
            shelvesetName,
            shelvesetOwner,
            itemSpecs,
            includeDownloadInfo,
            itemPropertyFilters);
    }

    /**
     * Gets the shelved changes for the given item specs.
     *
     * @param workspaceName
     *        the name of the workspace to query for (may be <code>null</code>)
     * @param workspaceOwner
     *        the owner of the workspace to query for (may be <code>null</code>)
     * @param shelvesetName
     *        the shelveset name (may be null)
     * @param shelvesetOwner
     *        the user name (may be null), which is the shelveset owner if the
     *        shelveset name is not <code>null</code>
     * @param itemSpecs
     *        the items to query changes for (null for all).
     * @param includeDownloadInfo
     *        If true, the server will include the information needed to
     *        download files. Only set this to true if you are going to be
     *        downloading the files using the objects that are returned. The
     *        call will be faster and require less bandwidth when this parameter
     *        is false (default for overloads that don't specify it)
     * @param itemPropertyFilters
     *        a list of property names to return on the pending change object if
     *        they exist (may be <code>null</code>)
     * @return an array of pending sets with the pending changes for the shelved
     *         changes. May be empty but never null.
     */
    public PendingSet[] queryShelvedChanges(
        final String workspaceName,
        final String workspaceOwner,
        final String shelvesetName,
        final String shelvesetOwner,
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo,
        String[] itemPropertyFilters) {
        final AtomicReference<Failure[]> failures = new AtomicReference<Failure[]>();

        itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        final PendingSet[] ret = getWebServiceLayer().queryShelvedChanges(
            workspaceName,
            workspaceOwner,
            shelvesetName,
            shelvesetOwner,
            itemSpecs,
            includeDownloadInfo,
            failures,
            itemPropertyFilters);

        reportFailures(failures.get());

        return ret;
    }

    /**
     * Gets the checkin policies defined for the given server paths, which must
     * be team projects.
     *
     * @param serverPaths
     *        the paths to get policy definitions for. If null, an empty array
     *        is returned.
     * @return the definitions loaded from the server. May be empty but never
     *         null.
     * @throws PolicySerializationException
     *         if an error occurred reading the policy definitions from the
     *         server.
     */
    public PolicyDefinition[] getCheckinPoliciesForServerPaths(final String[] serverPaths)
        throws PolicySerializationException {
        if (serverPaths == null || serverPaths.length == 0) {
            return new PolicyDefinition[0];
        }

        /*
         * Expand the server paths into full paths, if they weren't already,
         * then ensure the paths are team projects (and not root).
         */
        for (int i = 0; i < serverPaths.length; i++) {
            serverPaths[i] = ServerPath.canonicalize(serverPaths[i]);

            final int depth = ServerPath.getFolderDepth(serverPaths[i]);

            /*
             * If the folder path is root, throw.
             */
            if (depth == 0) {
                throw new VersionControlException(
                    Messages.getString("VersionControlClient.CheckinPoliciesMayOnlyBeAssociatedWithTeamProjects")); //$NON-NLS-1$
            }

            /*
             * If the folder path is some sub item of a team project, just use
             * its team project part.
             */
            if (depth > 1) {
                serverPaths[i] = ServerPath.getTeamProject(serverPaths[i]);
            }
        }

        Annotation[] annotations;

        /*
         * Optimize for the single team project, but simply request them all for
         * more than one path.
         */
        if (serverPaths.length == 1) {
            annotations = queryAnnotation(PolicyAnnotation.CHECKIN_POLICY_ANNOTATION_NAME, serverPaths[0], 0);
        } else {
            annotations = queryAnnotation(PolicyAnnotation.CHECKIN_POLICY_ANNOTATION_NAME, null, 0);
        }

        /*
         * Decode the annotations we received for the paths that are affected.
         */
        final ArrayList<PolicyDefinition> definitions = new ArrayList<PolicyDefinition>();
        for (int i = 0; i < annotations.length; i++) {
            for (int j = 0; j < serverPaths.length; j++) {
                final Annotation thisAnnotation = annotations[i];
                final String thisServerPath = serverPaths[j];

                /*
                 * If this annotation applies to this path.
                 */
                if (ServerPath.equals(thisServerPath, thisAnnotation.getItem())) {
                    /*
                     * Decode the single annotation value into (possibly)
                     * multiple definitions. This method may throw
                     * PolicySerializationException, which we want to throw from
                     * this method.
                     */
                    final PolicyAnnotation a = PolicyAnnotation.fromAnnotation(thisAnnotation.getValue());

                    // Add the decoded definitions.
                    definitions.addAll(Arrays.asList(a.getDefinitions()));
                }
            }
        }

        return definitions.toArray(new PolicyDefinition[definitions.size()]);
    }

    /**
     * Look up the merge candidates for merging between the requested items.
     *
     * @param sourcePath
     *        the local or server path of the source of the potential merge
     *        (must not be <code>null</code> or empty)
     * @param targetPath
     *        the local or server path of the target of the potential merge
     *        (must not be <code>null</code> or empty)
     * @param recursion
     *        what level of recursion we should apply to the candidate search
     *        (may be <code>null</code>).
     * @param mergeFlags
     *        merge command option(s) compatible with the /cadidate option (must
     *        not be <code>null</code>).
     * @return the array of merge candidates returned by the server. May be
     *         empty but never <code>null</code>.
     */
    public MergeCandidate[] getMergeCandidates(
        final String sourcePath,
        final String targetPath,
        final RecursionType recursion,
        final MergeFlags mergeFlags) {
        Check.notNullOrEmpty(sourcePath, "sourcePath"); //$NON-NLS-1$
        Check.notNullOrEmpty(targetPath, "targetPath"); //$NON-NLS-1$
        Check.notNull(mergeFlags, "mergeFlags"); //$NON-NLS-1$

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(new String[] {
            sourcePath,
            targetPath
        }, workspaceName, workspaceOwner);

        return getWebServiceLayer().queryMergeCandidates(
            workspaceName.get(),
            workspaceOwner.get(),
            new ItemSpec(sourcePath, recursion),
            new ItemSpec(targetPath, recursion),
            mergeFlags);
    }

    /**
     * Gets the branch objects inside the given root item with the given
     * recursion.
     *
     * @param rootItem
     *        the root item (must not be <code>null</code>)
     * @param recursion
     *        the recursion type (must not be <code>null</code>)
     * @return the branch objects found
     * @since TFS 2010
     */
    public BranchObject[] queryBranchObjects(final ItemIdentifier rootItem, final RecursionType recursion) {
        Check.notNull(rootItem, "rootItem"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$

        return getWebServiceLayer().queryBranchObjects(rootItem, recursion);
    }

    public void updateBranchObject(final BranchProperties branchProperties, final boolean updateExisting) {
        Check.notNull(branchProperties, "branchProperties"); //$NON-NLS-1$

        getWebServiceLayer().updateBranchObject(branchProperties, updateExisting);

        /*
         * NOTE: the .NET OM distinguishes between updateBranchObject events and
         * createBranchObject events. (They have create and update methods that
         * are identical except for the event fired. We do not distinguish.
         */
        eventEngine.fireBranchObjectUpdated(new BranchObjectUpdatedEvent(EventSource.newFromHere(), branchProperties));
    }

    /**
     * Deletes a branch object.
     *
     * @param branch
     *        the branch item to delete (must not be <code>null</code>)
     * @since TFS 2010
     */
    public void deleteBranchObject(final ItemIdentifier branch) {
        Check.notNull(branch, "branch"); //$NON-NLS-1$

        getWebServiceLayer().deleteBranchObject(branch);
    }

    /**
     * Gets information about merges performed on the given target item (and
     * version), optionally qualified by a source item (which can be null).
     *
     * @param sourceItem
     *        the item that is the source of merges to be returned (may be
     *        <code>null</code>)
     * @param sourceVersion
     *        the version of the source item for the merges (may be
     *        <code>null</code> if sourceItem is <code>null</code>)
     * @param targetItem
     *        the item that is the target of merges to be returned (must not be
     *        <code>null</code> or empty)
     *
     * @param targetVersion
     *        the version of the target item for the merges (must not be
     *        <code>null</code>)
     * @param versionFrom
     *        the oldest version to be included in the results (may be
     *        <code>null</code>)
     * @param versionTo
     *        the most recent version to be included in the results (may be
     *        <code>null</code>)
     * @param recursion
     *        the type of recursion to apply to the given items (must not be
     *        <code>null</code>)
     * @return the {@link ChangesetMerge}s returned by the server. May be empty
     *         but never <code>null</code>.
     */
    public ChangesetMerge[] queryMerges(
        final String sourceItem,
        final VersionSpec sourceVersion,
        final String targetItem,
        final VersionSpec targetVersion,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final RecursionType recursion) {
        Check.notNull(targetItem, "targetItem"); //$NON-NLS-1$
        Check.notNull(targetVersion, "targetVersion"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$

        final List<String> allPaths = new ArrayList<String>(2);

        ItemSpec sourceItemSpec = null;
        if (sourceItem != null) {
            sourceItemSpec = new ItemSpec(sourceItem, recursion);
            allPaths.add(sourceItem);
        }

        final ItemSpec targetItemSpec = new ItemSpec(targetItem, recursion);
        allPaths.add(targetItem);

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(allPaths.toArray(new String[allPaths.size()]), workspaceName, workspaceOwner);

        final AtomicReference<Changeset[]> changesetsHolder = new AtomicReference<Changeset[]>();

        final ChangesetMerge[] merges = getWebServiceLayer().queryMerges(
            workspaceName.get(),
            workspaceOwner.get(),
            sourceItemSpec,
            sourceVersion,
            targetItemSpec,
            targetVersion,
            versionFrom,
            versionTo,
            VersionControlConstants.MAX_MERGES_RESULTS,
            true,
            changesetsHolder);

        /*
         * Hook up the changesets that came back into the merge objects this
         * method returns.
         */
        if (merges != null) {
            final Map<Integer, Changeset> changesetIDToChangesetMap = new HashMap<Integer, Changeset>();

            /*
             * Map all the changesets by ID so we can hook them into the merges
             * that have matching target versions.
             */
            final Changeset[] changesets = changesetsHolder.get();
            if (changesets != null && changesets.length > 0) {
                for (int i = 0; i < changesets.length; i++) {
                    changesetIDToChangesetMap.put(new Integer(changesets[i].getChangesetID()), changesets[i]);
                }
            }

            for (int i = 0; i < merges.length; i++) {
                merges[i].setTargetChangeset(changesetIDToChangesetMap.get(new Integer(merges[i].getTargetVersion())));
            }
        }

        return merges;
    }

    /**
     * Gets detailed information about merges performed on the given target item
     * (and version), optionally qualified by a source item (which can be null).
     *
     * @param sourceItem
     *        the item that is the source of merges to be returned (may be null)
     * @param sourceVersion
     *        the version of the source item for the merges (may be null if
     *        sourceItem is null)
     * @param sourceDeletionID
     *        the deletion ID for the source item, if a specific deletion is
     *        being queried.
     * @param targetItem
     *        the item that is the target of merges to be returned (must not be
     *        <code>null</code> or empty)
     * @param targetVersion
     *        the version of the target item for the merges (must not be
     *        <code>null</code>)
     * @param targetDeletionID
     *        the deletion ID for the target item, if a specific deletion is
     *        being queried.
     * @param versionFrom
     *        the oldest version to be included in the results (may be null)
     * @param versionTo
     *        the most recent version to be included in the results (may be
     *        null)
     * @param recursion
     *        the type of recursion to apply to the given items (must not be
     *        <code>null</code>)
     * @return the {@link ChangesetMergeDetails} returned by the server.
     */
    public ChangesetMergeDetails queryMergesWithDetails(
        final String sourceItem,
        final VersionSpec sourceVersion,
        final int sourceDeletionID,
        final String targetItem,
        final VersionSpec targetVersion,
        final int targetDeletionID,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final RecursionType recursion) {
        Check.notNull(targetItem, "targetItem"); //$NON-NLS-1$
        Check.notNull(targetVersion, "targetVersion"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$

        final List<String> allPaths = new ArrayList<String>(2);
        ItemSpec sourceItemSpec = null;
        if (sourceItem != null) {
            sourceItemSpec = new ItemSpec(sourceItem, recursion, sourceDeletionID);
            allPaths.add(sourceItem);
        }

        final ItemSpec targetItemSpec = new ItemSpec(targetItem, recursion, targetDeletionID);
        allPaths.add(targetItem);

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(allPaths.toArray(new String[allPaths.size()]), workspaceName, workspaceOwner);

        return getWebServiceLayer().queryMergesWithDetails(
            workspaceName.get(),
            workspaceOwner.get(),
            sourceItemSpec,
            sourceVersion,
            targetItemSpec,
            targetVersion,
            versionFrom,
            versionTo,
            VersionControlConstants.MAX_MERGES_RESULTS,
            true);
    }

    /**
     * Gets source changes for a given {@link ItemSpec} in a specific version
     * range. The result is the set of changes as ExtendedMerge objects, which
     * contain the source of the merge (item, version, deletionID, and change)
     * as well as the changeset details. The {@link Changeset} is not a complete
     * object: only the owner, committer, date, comment, and changesetID are
     * available. No items, release notes, or other data are available.
     *
     * @param targetItemSpec
     *        the item that is the target of merges to be queried (must not be
     *        <code>null</code>)
     * @param targetVersionSpec
     *        the version of the target item to query at (must not be
     *        <code>null</code>)
     * @param versionFrom
     *        the oldest version to be included in the results (may be null)
     * @param versionTo
     *        the most recent version to be included in the results (may be
     *        null)
     * @param options
     *        the options to use (must not be <code>null</code>)
     * @return the {@link ChangesetMergeDetails} returned by the server.
     */
    public ExtendedMerge[] queryMergesExtended(
        final ItemSpec targetItemSpec,
        final VersionSpec targetVersionSpec,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final QueryMergesExtendedOptions options) {
        Check.notNull(targetItemSpec, "targetItemSpec"); //$NON-NLS-1$
        Check.notNull(targetVersionSpec, "targetVersionSpec"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        final AtomicReference<String> workspaceName = new AtomicReference<String>();
        final AtomicReference<String> workspaceOwner = new AtomicReference<String>();

        determineWorkspaceNameAndOwner(targetItemSpec, workspaceName, workspaceOwner);

        return getWebServiceLayer().queryMergesExtended(
            workspaceName.get(),
            workspaceOwner.get(),
            targetItemSpec,
            targetVersionSpec,
            versionFrom,
            versionTo,
            options);
    }

    /**
     * Returns all items which have a merge relationship to the provided item.
     *
     * @param serverItem
     *        Server Item to query.
     * @return Array of {@link ItemIdentifier} objects.
     */
    public ItemIdentifier[] queryMergeRelationships(final String serverItem) {
        Check.notNullOrEmpty(serverItem, "serverItem"); //$NON-NLS-1$

        return getWebServiceLayer().queryMergeRelationships(serverItem);
    }

    /**
     * Sets the checkin policies for the given team project path. If the given
     * definitions are <code>null</code> or empty, all policies are removed
     * (annotation is deleted).
     *
     * @param teamProjectPath
     *        the team project path to set policies for (must not be
     *        <code>null</code>)
     * @param definitions
     *        the policy definitions to set for the path, overwriting any
     *        previously set policy definitions. If <code>null</code> or empty,
     *        all policies are removed for the team project.
     */
    public void setCheckinPolicies(final String teamProjectPath, final PolicyDefinition[] definitions) {
        Check.notNull(teamProjectPath, "teamProjectPath"); //$NON-NLS-1$

        if (definitions == null || definitions.length == 0) {
            log.info(MessageFormat.format(
                "Got null or empty definitions, so deleting all policies for {0}", //$NON-NLS-1$
                teamProjectPath));
            deleteAnnotation(PolicyAnnotation.CHECKIN_POLICY_ANNOTATION_NAME, teamProjectPath, 0, null);
        } else {
            log.info(MessageFormat.format(
                "Writing {0} policy definitions to annotation for {1}", //$NON-NLS-1$
                definitions.length,
                teamProjectPath));

            final PolicyAnnotation annotation = new PolicyAnnotation(definitions);
            final String value = annotation.toAnnotationValue();

            createAnnotation(PolicyAnnotation.CHECKIN_POLICY_ANNOTATION_NAME, teamProjectPath, 0, value, null, true);
        }
    }

    /**
     * Gets the {@link SupportedFeatures} for the server this
     * {@link VersionControlClient} is connected to. This value is retrieved
     * from the server the first time this method is called. Subsequent calls to
     * this method will return the cached value.
     *
     * @return the server supported features (never <code>null</code>)
     */
    public SupportedFeatures getServerSupportedFeatures() {
        synchronized (serverSupportedFeaturesLock) {
            if (serverSupportedFeatures != null) {
                return serverSupportedFeatures;
            }

            serverSupportedFeatures = getWebServiceLayer().getSupportedFeatures();

            return serverSupportedFeatures;
        }
    }

    /**
     * Gets {@link ServerSettings} from the Server if available If not, it will
     * return a settings object with appropriate defaults.
     *
     * @param fallbackUsed
     *        Returns true if the default was used due to the server not
     *        supporting this feature
     */
    public ServerSettings getServerSettingsWithFallback(final AtomicBoolean fallbackUsed) {
        if (serverSettings == null) {
            synchronized (serverSettingsLock) {
                if (serverSettings == null) {
                    fallbackUsed.set(false);

                    serverSettings = getWebServiceLayer().getServerSettings();
                    if (serverSettings == null) {
                        // If settings are null that indicates that the we are
                        // talking to a server that doesn't support this web
                        // method.
                        fallbackUsed.set(true);
                        serverSettings = new ServerSettings(WorkspaceLocation.SERVER);
                    }
                }
            }
        }

        return serverSettings;
    }

    public int getMaxServerPathLength() {
        return getServerSettingsWithFallback(new AtomicBoolean()).getMaxAllowedServerPathLength();
    }

    /**
     * Gets the latest changeset ID from the server.
     *
     * @return the changeset ID number of the latest changeset.
     */
    public int getLatestChangesetID() {
        return getWebServiceLayer().getRepositoryProperties().getLatestChangesetID();
    }

    /**
     * Permanently destroys a versioned item. The item to be destroyed is
     * identified by an {@link ItemSpec} (which must contain a server path and
     * use full recursion) and an {@link VersionSpec}. The destroyed item is the
     * item that has the specified server path at the specified version. The
     * item to be destroyed does not need to be deleted before calling destroy.
     * If the item is deleted, a deletion ID can be specified in the item spec.
     * <p>
     * If destroy is successful, the items are immediately destroyed on the
     * server - destroy does not create pending changes like many other version
     * control methods.
     * <p>
     * The destroy feature is not available in TFS 2005. This method will throw
     * an exception if this {@link VersionControlClient} is connected to a Team
     * Foundation server that does not support destroy. Additionally, the
     * authenticated user must have the AdminProjectRights permission on all
     * items that will be destroyed.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param itemSpec
     *        identifies the item to destroy (must not be <code>null</code> and
     *        must contain a server path, not a local path)
     * @param versionSpec
     *        identifies the item to destroy (must not be <code>null</code>)
     * @param stopAt
     *        if keeping history, identifies the version including and after
     *        which file contents will be preserved for - defaults to latest
     *        version (must be <code>null</code> if not passing
     *        {@link DestroyFlags#KEEP_HISTORY})
     * @param flags
     *        the flags for this destroy operation, or {@link DestroyFlags#NONE}
     *        for default options
     * @return the items that were destroyed as a result of this call (never
     *         <code>null</code>)
     */
    public Item[] destroy(
        final ItemSpec itemSpec,
        final VersionSpec versionSpec,
        final VersionSpec stopAt,
        final DestroyFlags flags) {
        return destroy(itemSpec, versionSpec, stopAt, flags, null, null);
    }

    /**
     * Permanently destroys a versioned item. The item to be destroyed is
     * identified by an {@link ItemSpec} (which must contain a server path and
     * use full recursion) and an {@link VersionSpec}. The destroyed item is the
     * item that has the specified server path at the specified version. The
     * item to be destroyed does not need to be deleted before calling destroy.
     * If the item is deleted, a deletion ID can be specified in the item spec.
     * <p>
     * If destroy is successful (and {@link DestroyFlags#PREVIEW} was not
     * specified), the items are immediately destroyed on the server - destroy
     * does not create pending changes like many other version control methods.
     * <p>
     * The destroy feature is not available in TFS 2005. This method will throw
     * an exception if this {@link VersionControlClient} is connected to a Team
     * Foundation server that does not support destroy. Additionally, the
     * authenticated user must have the AdminProjectRights permission on all
     * items that will be destroyed.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param itemSpec
     *        identifies the item to destroy (must not be <code>null</code> and
     *        must contain a server path, not a local path)
     * @param versionSpec
     *        identifies the item to destroy (must not be <code>null</code>)
     * @param stopAt
     *        if keeping history, identifies the version including and after
     *        which file contents will be preserved for - defaults to latest
     *        version (must be <code>null</code> if not passing
     *        {@link DestroyFlags#KEEP_HISTORY})
     * @param flags
     *        the flags for this destroy operation, or {@link DestroyFlags#NONE}
     *        for default options
     * @param affectedPendingChanges
     *        a list to be filled with the pending sets affected by the destroy
     *        operation if the {@link DestroyFlags#AFFECTED_CHANGES} flag is
     *        set. May be <code>null</code> if the list of changes is not
     *        required.
     * @param affectedShelvedChanges
     *        a list to be filled with the shelved sets affected by the destroy
     *        operation if the {@link DestroyFlags#AFFECTED_CHANGES} flag is
     *        set. May be <code>null</code> if the list of changes is not
     *        required.
     * @return the items that were destroyed as a result of this call (never
     *         <code>null</code>)
     */
    public Item[] destroy(
        final ItemSpec itemSpec,
        final VersionSpec versionSpec,
        final VersionSpec stopAt,
        DestroyFlags flags,
        final List<PendingSet> affectedPendingChanges,
        final List<PendingSet> affectedShelvedChanges) {
        Check.notNull(itemSpec, "itemSpec"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        if (flags == null) {
            flags = DestroyFlags.NONE;
        }

        if (!ServerPath.isServerPath(itemSpec.getItem())) {
            throw new IllegalArgumentException(
                Messages.getString("VersionControlClient.DestroyOperationRequiresServerPath")); //$NON-NLS-1$
        }

        final AtomicReference<Failure[]> failuresHolder = new AtomicReference<Failure[]>();
        final AtomicReference<PendingSet[]> pendingChangesHolder = new AtomicReference<PendingSet[]>();
        final AtomicReference<PendingSet[]> shelvedChangesHolder = new AtomicReference<PendingSet[]>();

        Item[] resultItems = getWebServiceLayer().destroy(
            itemSpec,
            versionSpec,
            stopAt,
            flags,
            failuresHolder,
            pendingChangesHolder,
            shelvedChangesHolder);

        if (resultItems == null) {
            resultItems = new Item[0];
        }

        if (affectedPendingChanges != null && pendingChangesHolder.get() != null) {
            for (final PendingSet set : pendingChangesHolder.get()) {
                affectedPendingChanges.add(set);
            }
        }

        if (affectedShelvedChanges != null && shelvedChangesHolder.get() != null) {
            for (final PendingSet set : shelvedChangesHolder.get()) {
                affectedShelvedChanges.add(set);
            }
        }

        for (final Item item : resultItems) {
            final DestroyEvent event = new DestroyEvent(EventSource.newFromHere(), item, stopAt, flags);
            eventEngine.fireDestroyEvent(event);
        }

        Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).notifyForFolderContentChanged(this);

        reportFailures(failuresHolder.get());

        return resultItems;
    }

    /**
     * Resolve local file conflicts.
     */
    public void resolveLocalConflicts(
        final Workspace workspace,
        final Conflict[] conflicts,
        final ResolveErrorOptions errorOptions) {
        /*
         * To handle both the case of client needing a new version and someone
         * wanting to re-get the latest (/all or /force), we need to specify
         * All.
         */
        GetOptions getOptions = GetOptions.GET_ALL;

        final List<List<Conflict>> resolveGroups = new ArrayList<List<Conflict>>();
        resolveGroups.add(new ArrayList<Conflict>());

        for (final Conflict conflict : conflicts) {
            /*
             * For source or target writable conflicts, the user may choose to
             * overwrite the files.
             */
            if (conflict.getReason() == OperationStatus.SOURCE_WRITABLE.getValue()
                || conflict.getReason() == OperationStatus.TARGET_WRITABLE.getValue()) {
                if (Resolution.OVERWRITE_LOCAL.equals(conflict.getResolution())) {
                    if (conflict.getReason() == OperationStatus.SOURCE_WRITABLE.getValue()
                        && null != conflict.getSourceLocalItem()) {
                        new File(conflict.getSourceLocalItem()).delete();
                    } else if (conflict.getReason() == OperationStatus.TARGET_WRITABLE.getValue()
                        && null != conflict.getTargetLocalItem()) {
                        new File(conflict.getTargetLocalItem()).delete();
                    }

                    // Just in case
                    getOptions = getOptions.combine(GetOptions.OVERWRITE);
                } else {
                    String writablePath = conflict.getTargetLocalItem();

                    if (conflict.getReason() == OperationStatus.SOURCE_WRITABLE.getValue()) {
                        writablePath = conflict.getSourceLocalItem();
                    }

                    /*
                     * If the file exists and is writable, stop here if the user
                     * specified a resolution other than overwrite. The caller
                     * will see that the conflict was not resolved.
                     */
                    if (writablePath != null
                        && new File(writablePath).exists()
                        && FileSystemUtils.getInstance().getAttributes(writablePath).isReadOnly() == false) {
                        return;
                    }
                }
            }

            // put the conflicts into batches based on the batch size.
            List<Conflict> conflictGroup = resolveGroups.get(resolveGroups.size() - 1);

            if (resolveGroups.size() >= 200) {
                conflictGroup = new ArrayList<Conflict>();
                resolveGroups.add(conflictGroup);
            }

            conflictGroup.add(conflict);
        }

        /* Now resolve the conflicts in each group separately */
        for (final List<Conflict> conflictsToResolve : resolveGroups) {
            /*
             * First, remove the local conflict to guarantee that this
             * particular conflict gets removed. Then fire the resolve event and
             * try the get again.
             */
            final ResolveLocalConflictHandler handler = new ResolveLocalConflictHandler(this, workspace);

            try {
                webServiceLayer.removeLocalConflicts(
                    workspace.getName(),
                    workspace.getOwnerName(),
                    conflictsToResolve,
                    errorOptions,
                    handler,
                    handler);
            } catch (final Exception e) {
                throw new VersionControlException(e);
            }

            if (handler.getGetRequests().size() > 0) {
                workspace.get(
                    handler.getGetRequests().toArray(new GetRequest[handler.getGetRequests().size()]),
                    getOptions);
            }
        }
    }

    /**
     * Takes a list of candidates for AutoResolve and attempts to resolve them
     * with the correct resolution and performing a content merge if necessary.
     *
     * @param conflicts
     *        The candidate conflicts
     * @param resolveOptions
     *        Resolution options
     * @return unresolved conflicts
     */
    public Conflict[] autoResolveValidConflicts(
        final Workspace workspace,
        final Conflict[] conflicts,
        final AutoResolveOptions resolveOptions) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(conflicts, "conflicts"); //$NON-NLS-1$

        final List<Conflict> unresolvedConflicts = new ArrayList<Conflict>();
        final List<Conflict> conflictsToResolve = new ArrayList<Conflict>();

        for (final Conflict conflict : conflicts) {
            try {
                if (conflict.isResolved()) {
                    /* Nothing to do. */
                    continue;
                }

                if (conflict.isBaseless()) {
                    // We never resolve baseless merge conflicts
                    unresolvedConflicts.add(conflict);
                    continue;
                }

                /*
                 * Determine the proper resolution since we should take the
                 * server version for truly redundant conflict. If it isn't
                 * redundant then make sure it is valid for auto merge.
                 */
                Resolution resolution = Resolution.NONE;

                if (resolveOptions.contains(AutoResolveOptions.REDUNDANT) && conflict.isRedundant(false, workspace)) {
                    resolution = Resolution.ACCEPT_THEIRS;
                } else {
                    /* Make sure this conflict is valid for auto merge. */
                    if (!conflict.isValidForAutoMerge(workspace)) {
                        unresolvedConflicts.add(conflict);
                        continue;
                    }

                    resolution = Resolution.ACCEPT_MERGE;
                }

                /*
                 * If the resolution is accept merge and we can merge content
                 * then let's attempt to do so.
                 */
                if (Resolution.ACCEPT_MERGE.equals(resolution) && conflict.canMergeContent()) {
                    /*
                     * Only perform the content merge if a "basic" merge is
                     * allowed. If it isn't this should end up in the unresolved
                     * conflicts list.
                     */
                    if (conflict.isBasicMergeAllowed(workspace) && !conflict.isEncodingMismatched()) {
                        /*
                         * If there is an edit change, attempt the internal
                         * merge. Otherwise just resolve as accept merge which
                         * will take name and/or encoding changes defined in
                         * Conflict.ResolutionOptions.
                         */
                        conflict.resetChangeSummaryIfLocalFileModified();

                        /*
                         * We merge again if ChangeSummary was not calculated or
                         * MergedFileName does not exist; the latter can happen
                         * if we already tried to resolve this conflict as
                         * AcceptMerge, MergedFileName was moved to workspace
                         * but Resolve failed on the server.
                         */
                        if (conflict.getContentMergeSummary() == null
                            || conflict.getMergedFileName() == null
                            || conflict.getMergedFileName().length() == 0
                            || !new File(conflict.getMergedFileName()).exists()) {
                            final MergeEngine mergeEngine = new MergeEngine(workspace, this);
                            mergeEngine.mergeContent(conflict, false, null, null, null);
                        }
                    }

                    /*
                     * Make sure we have a merge summary and don't have a
                     * conflicting change. Right now the ContentMergeSummary ==
                     * null check is just for safety, if there is a case to
                     * remove it then that is acceptable but
                     * HasConflictingChange will always return false if there is
                     * no ContentMergeSummary.
                     */
                    if (conflict.getContentMergeSummary() == null
                        || conflict.hasConflictingContentChange()
                        || !conflict.isAutoMergeApplicable(resolveOptions)) {
                        /* Since there is a conflict mark it as unresolved. */
                        unresolvedConflicts.add(conflict);
                        continue;
                    }
                }

                /*
                 * Generate the properties merge summary if this is a property
                 * conflict.
                 */
                if (Resolution.ACCEPT_MERGE.equals(resolution) && conflict.isPropertyConflict()) {
                    conflict.mergeProperties(workspace);

                    if (conflict.hasConflictingPropertyChange()) {
                        // Since there is a conflict mark it as unresolved.
                        unresolvedConflicts.add(conflict);
                        continue;
                    }

                    conflict.getResolutionOptions().setAcceptMergeProperties(
                        conflict.getPropertiesMergeSummary().getMergedProperties());
                }

                if (conflict.isYourNameChanged() && conflict.isTheirNameChanged()) {
                    /*
                     * Explicitly case sensitive so that we don't auto resolve a
                     * case-only difference on both sides.
                     */
                    if (!conflict.isNameChangeIsRedundant()) {
                        unresolvedConflicts.add(conflict);
                        continue;
                    }

                    conflict.getResolutionOptions().setNewPath(conflict.getYourServerItem());
                }

                /* Actually resolve the conflict. */
                conflict.setResolution(resolution);
                conflict.setAutoResolved(true);
                conflictsToResolve.add(conflict);
            } catch (final Exception e) {
                /*
                 * Eat all exceptions here because failing to auto resolve a
                 * conflict should be silent.
                 */
                log.info("Caught exception while auto resolving conflicts", e); //$NON-NLS-1$

                conflict.setResolution(Resolution.NONE);
                conflict.setAutoResolved(false);
                unresolvedConflicts.add(conflict);
            }
        }

        /*
         * Ideally we won't have any failures while resolving conflicts but if
         * we do we will just eat them since we don't want to show errors on
         * auto resolve and the result will just be that the conflicts are still
         * present. It is important that we call ResolveConflictsInternal here
         * and not ResolveConflicts because we may be passing in conflicts with
         * different resolution options.
         */
        final AtomicReference<Conflict[]> resolvedConflicts = new AtomicReference<Conflict[]>();
        final ResolveErrorOptions errorOptions = resolveOptions.contains(AutoResolveOptions.SILENT)
            ? ResolveErrorOptions.NONE : ResolveErrorOptions.RAISE_WARNINGS_FOR_ERROR;

        workspace.resolveConflicts(
            conflictsToResolve.toArray(new Conflict[conflictsToResolve.size()]),
            null,
            errorOptions,
            resolvedConflicts);

        for (final Conflict conflict : conflictsToResolve) {
            if (!conflict.isResolved()) {
                /*
                 * We couldn't resolve the conflict so add the conflict to the
                 * list of unresolve conflicts.
                 */
                conflict.setResolution(Resolution.NONE);
                conflict.setAutoResolved(false);
                unresolvedConflicts.add(conflict);
            }
        }

        return unresolvedConflicts.toArray(new Conflict[unresolvedConflicts.size()]);
    }

    public void resolveConflicts(
        final Workspace workspace,
        final Conflict[] conflicts,
        String[] itemPropertyFilters,
        final ResolveErrorOptions errorOptions,
        final AtomicReference<Conflict[]> resolvedConflicts) {
        itemPropertyFilters = mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        resolvedConflicts.set(new Conflict[0]);

        final List<Conflict> allResolvedConflicts = new ArrayList<Conflict>();
        ChangePendedFlags flags = ChangePendedFlags.UNKNOWN;

        final List<Conflict> localConflicts = new ArrayList<Conflict>();
        final List<List<Conflict>> conflictResolveGroups = new ArrayList<List<Conflict>>();
        conflictResolveGroups.add(new ArrayList<Conflict>());

        for (final Conflict conflict : conflicts) {
            try {
                /*
                 * If the resolution is to ignore the conflict, there's nothing
                 * to do.
                 */
                if (Resolution.NONE.equals(conflict.getResolution())) {
                    continue;
                }

                /*
                 * For local conflicts, we must try the get again. For all other
                 * conflicts, we must tell the server the conflict resolution
                 * details.
                 */
                if (ConflictType.LOCAL.equals(conflict.getType())
                    && !Resolution.DELETE_CONFLICT.equals(conflict.getResolution())) {
                    localConflicts.add(conflict);
                    continue;
                }

                /* Determine which conflict group to add this to. */
                List<Conflict> conflictGroup = conflictResolveGroups.get(conflictResolveGroups.size() - 1);

                // If it is on a folder then force this resolution to run it its
                // own group to prevent
                // possible collisions on getops with children
                if (ItemType.FOLDER.equals(conflict.getYourItemType())
                    || ItemType.FOLDER.equals(conflict.getTheirItemType())
                    || ItemType.FOLDER.equals(conflict.getBaseItemType())) {
                    conflictGroup = new ArrayList<Conflict>();
                    conflictResolveGroups.add(conflictGroup);

                    // Create a new group for the next conflict so that it
                    // doesn't get batched with this one.
                    conflictResolveGroups.add(new ArrayList<Conflict>());
                } else if (conflictGroup.size() >= 200) {
                    conflictGroup = new ArrayList<Conflict>();
                    conflictResolveGroups.add(conflictGroup);
                }

                conflictGroup.add(conflict);
            } catch (final RuntimeException e) {
                if (ResolveErrorOptions.THROW_ON_ERROR.equals(errorOptions)) {
                    throw e;
                } else if (ResolveErrorOptions.RAISE_WARNINGS_FOR_ERROR.equals(errorOptions)) {
                    eventEngine.fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));
                }
            }
        }

        if (localConflicts.size() != 0) {
            resolveLocalConflicts(workspace, localConflicts.toArray(new Conflict[localConflicts.size()]), errorOptions);
        }

        for (final List<Conflict> conflictGroup : conflictResolveGroups) {
            if (conflictGroup.size() == 0) {
                continue;
            }

            final WorkspaceLock lock = workspace.lock();

            /*
             * Lock over the call to resolve (which may undo a pending change)
             * and the completion of the processing of those get operations.
             * This prevents a scan from occurring in between these two
             * transactions. If a scan were to run, it would pend an edit on the
             * item whose pending change was just undone by Resolve. This also
             * prevents the movement of merged file content into the workspace
             * from triggering a scan.
             */
            try {
                /*
                 * We are doing local operation first, so if it fails, we still
                 * have conflict on the server. Bugs 317070 and 395476 If server
                 * fails to resolve, we have incorrect file content in the
                 * workspace, but this is content user wanted anyway Otherwise
                 * (server call first, local operation later) we could resolve
                 * the conflict but fail to update the local workspace, so user
                 * would really do AcceptYours instead of AcceptMerge
                 */
                final Map<Integer, String> updatedSourceLocalItems = new HashMap<Integer, String>();

                /*
                 * Replace the user's file with the merged version if we
                 * accepted the merge and it's valid to merge the content. The
                 * caller may set the merged file name even when it's not valid
                 * to copy it over (GUI does this).
                 */
                for (final Conflict conflict : conflictGroup) {
                    String updatedSourceLocalItem = null;

                    if (Resolution.ACCEPT_MERGE.equals(conflict.getResolution())
                        && conflict.canMergeContent()
                        && conflict.getMergedFileName() != null
                        && conflict.getMergedFileName().length() > 0) {
                        if (conflict.getSourceLocalItem() != null) {
                            /*
                             * Rename the temp file to the real file name.
                             * Delete the target since we know source and target
                             * are different (avoids exception handling).
                             */
                            new File(conflict.getSourceLocalItem()).delete();

                            try {
                                FileHelpers.rename(conflict.getMergedFileName(), conflict.getSourceLocalItem());
                            } catch (final IOException e) {
                                throw new RuntimeException(e);
                            }

                            updatedSourceLocalItem = conflict.getSourceLocalItem();
                        } else {
                            Check.isTrue(
                                conflict.getMergedFileName().toLowerCase().contains("temp"), //$NON-NLS-1$
                                "conflict.getMergedFileName().toLowerCase().contains(\"temp\")"); //$NON-NLS-1$

                            /*
                             * Since we don't have a SourceLocalItem, use the
                             * merged file as the source local item
                             */
                            updatedSourceLocalItem = conflict.getMergedFileName();
                        }
                    }

                    if (null != updatedSourceLocalItem) {
                        /*
                         * In a local workspace, we have the following problem:
                         * Resolve requires a reconcile, and reconcile does a
                         * disk scan We have just placed new content on this
                         * local item, which will cause the disk scan to pend an
                         * edit on the item. Then reconcile sends the edit to
                         * the server, and reconcile undoes the "merge" change
                         * to replace it with "merge, edit". This causes the
                         * conflict to disappear. So, prevent the scanner from
                         * pending an edit on the item, just for this call.
                         */

                        workspace.getWorkspaceWatcher().addSkippedItem(updatedSourceLocalItem);
                        updatedSourceLocalItems.put(conflict.getConflictID(), updatedSourceLocalItem);
                    }
                }

                final ResolveConflictHandler handler =
                    new ResolveConflictHandler(this, workspace, updatedSourceLocalItems);

                try {
                    webServiceLayer.resolve(
                        workspace.getName(),
                        workspace.getOwnerName(),
                        conflicts,
                        null, /* itemAttributeFilters */
                        itemPropertyFilters,
                        errorOptions,
                        handler,
                        handler);
                } catch (final Exception e) {
                    for (final Conflict conflict : conflicts) {
                        final String updatedSourceLocalItem = updatedSourceLocalItems.get(conflict.getConflictID());

                        if (updatedSourceLocalItem != null) {
                            workspace.getWorkspaceWatcher().removeSkippedItem(updatedSourceLocalItem);

                            conflict.setSourceLocalItem(updatedSourceLocalItem);
                        }
                    }

                    throw new VersionControlException(e);
                }

                /* Process the resolved conflicts */
                final UpdateLocalVersionQueue ulvq = new UpdateLocalVersionQueue(workspace);

                try {
                    for (final ResolveConflictHandler.UpdateLocalVersionSpec ulvSpec : handler.getUpdateLocalVersionSpecs()) {
                        ulvq.queueUpdate(
                            ulvSpec.getSourceServerItem(),
                            ulvSpec.getItemID(),
                            ulvSpec.getSourceLocalItem(),
                            ulvSpec.getVersionServer(),
                            ulvSpec.getProperyValues());
                    }
                } finally {
                    ulvq.close();
                }

                final GetEngine getEngine = new GetEngine(this);

                for (final ResolveConflictHandler.GetOperationGroup group : handler.getGetOpGroups()) {
                    getEngine.processGetOperations(
                        workspace,
                        ProcessType.UNDO,
                        group.getUndoOps(),
                        GetOptions.NONE,
                        handler.getFlags());

                    getEngine.processGetOperations(
                        workspace,
                        ProcessType.GET,
                        group.getGetOps(),
                        GetOptions.NONE,
                        handler.getFlags());
                }

                allResolvedConflicts.addAll(handler.getResolvedConflicts());
                flags = flags.combine(handler.getFlags());
            }

            finally {
                if (lock != null) {
                    lock.close();
                }
            }
        }

        resolvedConflicts.set(allResolvedConflicts.toArray(new Conflict[allResolvedConflicts.size()]));

        if (flags.contains(ChangePendedFlags.WORKING_FOLDER_MAPPINGS_UPDATED)) {
            workspace.invalidateMappings();
        }
    }

    /**
     * Asks the server to update the authenticated user's display name. No
     * client state is changed by this method, but subsequent requests to the
     * server will reveal any changes made to the user's full name.
     */
    public void updateUserName() {
        getWebServiceLayer().refreshIdentityDisplayName();
    }

    /**
     * @see #determineWorkspaceNameAndOwner(ItemSpec[], AtomicReference<String>,
     *      AtomicReference<String>)
     */
    public void determineWorkspaceNameAndOwner(
        final ItemSpec itemSpec,
        final AtomicReference<String> workspaceName,
        final AtomicReference<String> workspaceOwner) {
        if (itemSpec == null) {
            workspaceName.set(null);
            workspaceOwner.set(null);
        } else {
            determineWorkspaceNameAndOwner(new ItemSpec[] {
                itemSpec
            }, workspaceName, workspaceOwner);
        }
    }

    /**
     * @see #determineWorkspaceNameAndOwner(ItemSpec[], AtomicReference<String>,
     *      AtomicReference<String>)
     */
    public void determineWorkspaceNameAndOwner(
        final String path,
        final AtomicReference<String> workspaceName,
        final AtomicReference<String> workspaceOwner) {
        if (path == null || path.length() == 0) {
            workspaceName.set(null);
            workspaceOwner.set(null);
        } else {
            determineWorkspaceNameAndOwner(new ItemSpec(path, RecursionType.NONE), workspaceName, workspaceOwner);
        }
    }

    /**
     * @see #determineWorkspaceNameAndOwner(ItemSpec[], AtomicReference<String>,
     *      AtomicReference<String>)
     */
    public void determineWorkspaceNameAndOwner(
        final LabelItemSpec[] labelItemSpecs,
        final AtomicReference<String> workspaceName,
        final AtomicReference<String> workspaceOwner) {
        if (labelItemSpecs == null) {
            workspaceName.set(null);
            workspaceOwner.set(null);
        } else {
            final ItemSpec[] itemSpecs = new ItemSpec[labelItemSpecs.length];
            for (int i = 0; i < itemSpecs.length; i++) {
                itemSpecs[i] = labelItemSpecs[i].getItemSpec();
            }

            determineWorkspaceNameAndOwner(itemSpecs, workspaceName, workspaceOwner);
        }
    }

    /**
     * @see #determineWorkspaceNameAndOwner(ItemSpec[], AtomicReference<String>,
     *      AtomicReference<String>)
     */
    public void determineWorkspaceNameAndOwner(
        final SecurityChange[] securityChanges,
        final AtomicReference<String> workspaceName,
        final AtomicReference<String> workspaceOwner) {
        if (securityChanges == null) {
            workspaceName.set(null);
            workspaceOwner.set(null);
        } else {
            final ItemSpec[] itemSpecs = new ItemSpec[securityChanges.length];
            for (int i = 0; i < itemSpecs.length; i++) {
                itemSpecs[i] = new ItemSpec(securityChanges[i].getItem(), RecursionType.NONE);
            }

            determineWorkspaceNameAndOwner(itemSpecs, workspaceName, workspaceOwner);
        }
    }

    /**
     * @see #determineWorkspaceNameAndOwner(ItemSpec[], AtomicReference<String>,
     *      AtomicReference<String>)
     */
    public void determineWorkspaceNameAndOwner(
        final String[] paths,
        final AtomicReference<String> workspaceName,
        final AtomicReference<String> workspaceOwner) {
        if (paths == null) {
            workspaceName.set(null);
            workspaceOwner.set(null);
        } else {
            determineWorkspaceNameAndOwner(
                ItemSpec.fromStrings(paths, RecursionType.NONE),
                workspaceName,
                workspaceOwner);
        }
    }

    /**
     * Determine the workspace name and owner for all given items which are
     * local paths. Throws if any two paths are in different workspaces. Does
     * not throw if no workspace was found ({@link AtomicReference<String>}
     * values are set to <code>null</code>).
     *
     * @param itemSpecs
     *        the {@link ItemSpec}s to determine name and owner for (if
     *        <code>null</code>, both {@link AtomicReference<String>} values are
     *        set to <code>null</code>)
     * @param workspaceName
     *        the {@link AtomicReference<String>} to update with the workspace
     *        name (must not be <code>null</code>)
     * @param workspaceOwner
     *        the {@link AtomicReference<String>} to update with the workspace
     *        owner (must not be <code>null</code>)
     * @throws OnlyOneWorkspaceException
     *         if any two paths are in different workspaces
     */
    public void determineWorkspaceNameAndOwner(
        final ItemSpec[] itemSpecs,
        final AtomicReference<String> workspaceName,
        final AtomicReference<String> workspaceOwner) {
        if (itemSpecs == null) {
            workspaceName.set(null);
            workspaceOwner.set(null);
        } else {
            Workspace workspace = null;

            for (int i = 0; i < itemSpecs.length; i++) {
                final ItemSpec spec = itemSpecs[i];

                if (ServerPath.isServerPath(spec.getItem()) == false) {
                    final Workspace itemWorkspace = getWorkspace(spec.getItem());

                    if (workspace == null) {
                        workspace = itemWorkspace;
                    } else if (workspace.equals(itemWorkspace) == false) {
                        throw new OnlyOneWorkspaceException(itemWorkspace, spec.getItem());
                    }
                }
            }

            if (workspace == null) {
                workspaceName.set(null);
                workspaceOwner.set(null);
            } else {
                workspaceName.set(workspace.getName());
                workspaceOwner.set(workspace.getOwnerName());
            }
        }
    }

    /**
     * Resolve the specified user name to an actual, fully qualified unique user
     * name. This may involve contacting the server to find out the current
     * authorized identity.
     *
     * @param user
     *        the user name to resolve (if <code>null</code>, <code>null</code>
     *        is returned)
     * @return the resolved display name, <code>null</code> if <code>null</code>
     *         was given
     */
    public String resolveUserUniqueName(String user) {
        if (VersionControlConstants.AUTHENTICATED_USER.equals(user)) {
            user = getConnection().getAuthorizedIdentity().getUniqueName();
        }

        return user;
    }

    /**
     * Resolve the specified user name to an actual, fully qualified user name.
     * This may involve contacting the server to find out the current authorized
     * identity.
     *
     * @param user
     *        the user name to resolve (if <code>null</code>, <code>null</code>
     *        is returned)
     * @return the resolved display name, <code>null</code> if <code>null</code>
     *         was given
     */
    public String resolveUserDisplayName(String user) {
        if (VersionControlConstants.AUTHENTICATED_USER.equals(user)) {
            user = getConnection().getAuthorizedIdentity().getDisplayName();
        }

        return user;
    }

    /**
     * @return true if the passed in name represents the authorized user.
     */
    public boolean isAuthorizedUser(final String name) {
        if (VersionControlConstants.AUTHENTICATED_USER.equals(name)) {
            return true;
        }

        // if (getWebServiceLayer().getServiceLevel().getValue() >=
        // WebServiceLevel.TFS_2012.getValue())
        // {
        return IdentityHelper.identityHasName(getConnection().getAuthorizedIdentity(), name);
        // }
        //
        // return
        // Workspace.matchOwner(getConnection().getAuthorizedAccountName(),
        // name);
    }

    /**
     * Removes a cached workspace that matches the given name and owner and this
     * client's server's GUID from the {@link Workstation}'s cache. The caller
     * is responsible for saving the {@link Workstation} cache.
     */
    public WorkspaceInfo removeCachedWorkspace(final String workspaceName, String workspaceOwner) {
        workspaceOwner = resolveUserUniqueName(workspaceOwner);

        final WorkspaceInfo workspaceInfo =
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).getLocalWorkspaceInfo(
                getServerGUID(),
                workspaceName,
                workspaceOwner);

        if (workspaceInfo != null) {
            // Remove it from the cache file.
            Workstation.getCurrent(getConnection().getPersistenceStoreProvider()).getCache().removeWorkspace(
                workspaceInfo);
        }

        return workspaceInfo;
    }

    public FileType queryCachedFileType(final String extension) {
        synchronized (cachedFileTypesLock) {
            if (cachedFileTypes == null) {
                cachedFileTypes = new TreeMap<String, FileType>(String.CASE_INSENSITIVE_ORDER);

                final FileType[] serverFileTypes = getFileTypes();

                for (final FileType serverFileType : serverFileTypes) {
                    for (final String fileTypeExtension : serverFileType.getExtensions()) {
                        cachedFileTypes.put(fileTypeExtension, serverFileType);
                    }
                }
            }

            return cachedFileTypes.get(extension);
        }
    }

    public FileType[] getFileTypes() {
        return webServiceLayer.queryFileTypes();
    }

    /**
     * Merges the specified filters with the default item property filters
     * configured on this {@link VersionControlClient}. Only unique filters are
     * returned (case insensitive) and order is not preserved.
     *
     * @param filters
     *        some item property filters or <code>null</code>
     * @return a new array of filters including the specified filters (if any)
     *         and the default filters, or <code>null</code> if the specified
     *         filters were null and there are no default filters
     * @see #getDefaultItemPropertyFilters()
     * @see #setDefaultItemPropertyFilters(String[])
     */
    public String[] mergeWithDefaultItemPropertyFilters(final String[] filters) {
        return PropertyUtils.mergePropertyFilters(getDefaultItemPropertyFilters(), filters);
    }

    public SecurityNamespace getWorkspaceSecurity() {
        if (workspaceSecurity == null) {
            synchronized (workspaceSecurityLock) {
                if (workspaceSecurity == null) {
                    try {
                        final ISecurityService securityService = new SecurityService(connection);

                        if (securityService != null) {
                            workspaceSecurity = securityService.getSecurityNamespace(
                                VersionControlClient.WORKSPACE_SECURITY_NAMESPACE_ID);
                        }
                    } catch (final Exception e) {
                    }
                }
            }
        }

        return workspaceSecurity;
    }
}
