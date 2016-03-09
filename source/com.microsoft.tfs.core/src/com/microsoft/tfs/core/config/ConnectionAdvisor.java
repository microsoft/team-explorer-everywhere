// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config;

import java.net.URI;
import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.TFProxyServerSettings;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.config.client.ClientFactory;
import com.microsoft.tfs.core.config.httpclient.HTTPClientFactory;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.config.serveruri.ServerURIProvider;
import com.microsoft.tfs.core.config.tfproxy.TFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;
import com.microsoft.tfs.core.httpclient.HttpClient;

/**
 * <p>
 * A {@link ConnectionAdvisor} provides services and configuration data for a
 * {@link TFSConnection} to use. A {@link ConnectionAdvisor} is provided to a
 * {@link TFSConnection} at construction time, and the {@link TFSConnection}
 * retains the {@link ConnectionAdvisor} instance for the lifetime of the
 * {@link TFSConnection}.
 * </p>
 *
 * <p>
 * Any thread may call any of the {@link ConnectionAdvisor} methods.
 * {@link TFSConnection} allows only a single thread at a time to call a given
 * method. However, since a {@link ConnectionAdvisor} is passed to a
 * {@link TFSConnection}, controlling all concurrent usage to the
 * {@link ConnectionAdvisor} is outside the scope of the {@link TFSConnection}'s
 * control. Implementations should document their thread safety, and clients who
 * use the implementations and create {@link TFSConnection}s are responsible for
 * obeying the implementation's threading guidelines.
 * </p>
 *
 * <p>
 * Each method receives a {@link ConnectionInstanceData} object. This object
 * encapsulates all of the instance configuration data that a
 * {@link TFSConnection} has. The return value of each method is cached by the
 * {@link TFSConnection} if the {@link TFSConnection} needs to use it in the
 * future.
 * </p>
 *
 * @see TFSConnection
 * @see ConnectionInstanceData
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface ConnectionAdvisor {
    /**
     * Called by the {@link TFSConnection} the first time that the
     * {@link TFSConnection}'s {@link TimeZone} is requested.
     *
     * @param instanceData
     *        the {@link TFSConnection}'s instance data
     * @return a {@link TimeZone} for the {@link TFSConnection} to use, never
     *         <code>null</code>
     */
    public TimeZone getTimeZone(ConnectionInstanceData instanceData);

    /**
     * <p>
     * Called by the {@link TFSConnection} the first time that the
     * {@link TFSConnection}'s {@link Locale} is requested.
     * </p>
     *
     * <p>
     * This value will be sent to the server with web service requests to
     * identify the client's preferred language and country (via the HTTP
     * Accept-Language header). It is also used by core classes to compare
     * strings, format output, etc., in the correct way for the current
     * connection.
     * </p>
     *
     * @param instanceData
     *        the {@link TFSConnection}'s instance data
     * @return a {@link Locale} for the {@link TFSConnection} to use, never
     *         <code>null</code>
     */
    public Locale getLocale(ConnectionInstanceData instanceData);

    /**
     * Called by the {@link TFSConnection} the first time that the
     * {@link TFSConnection}'s {@link PersistenceStoreProvider} is requested.
     *
     *
     * @param instanceData
     *        the {@link TFSConnection}'s instance data
     * @return a {@link PersistenceStoreProvider} for the {@link TFSConnection}
     *         to use, never <code>null</code>
     */
    public PersistenceStoreProvider getPersistenceStoreProvider(ConnectionInstanceData instanceData);

    /**
     * Called by the {@link TFSConnection} the first time that the
     * {@link TFSConnection}'s {@link HttpClient} is requested.
     *
     * @param instanceData
     *        the {@link TFSConnection}'s instance data
     * @return a {@link HTTPClientFactory} for the {@link TFSConnection} to use
     *         (must not be <code>null</code>)
     */
    public HTTPClientFactory getHTTPClientFactory(ConnectionInstanceData instanceData);

    /**
     * Called by the {@link TFSConnection} the first time that the
     * {@link TFSConnection}'s server {@link URI} is requested.
     *
     * @param instanceData
     *        the {@link TFSConnection}'s instance data
     * @return a {@link ServerURIProvider} for the {@link TFSConnection} to use
     *         (must not be <code>null</code>)
     */
    public ServerURIProvider getServerURIProvider(ConnectionInstanceData instanceData);

    /**
     * Called by the {@link TFSConnection} the first time that a web service is
     * requested.
     *
     * @param instanceData
     *        the {@link TFSConnection}'s instance data
     * @return a {@link WebServiceFactory} for the {@link TFSConnection} to use
     *         (must not be <code>null</code>)
     */
    public WebServiceFactory getWebServiceFactory(ConnectionInstanceData instanceData);

    /**
     * Called by the {@link TFSConnection} the first time that a client is
     * requested.
     *
     * @param instanceData
     *        the {@link TFSConnection}'s instance data
     * @return a {@link ClientFactory} for the {@link TFSConnection} to use
     *         (must not be <code>null</code>)
     */
    public ClientFactory getClientFactory(ConnectionInstanceData instanceData);

    /**
     * Called by the {@link TFSConnection} the first time that the
     * {@link TFSConnection}'s {@link TFProxyServerSettings} is requested.
     *
     * @param instanceData
     *        the {@link TFSConnection}'s instance data (never <code>null</code>
     *        )
     * @return a {@link TFProxyServerSettings} object for the
     *         {@link TFSConnection} to use, or <code>null</code> to not use a
     *         TFS proxy server
     */
    public TFProxyServerSettingsFactory getTFProxyServerSettingsFactory(ConnectionInstanceData instanceData);
}
