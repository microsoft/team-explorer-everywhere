// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.httpclient;

import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HostConfiguration;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpConnectionManager;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.MultiThreadedHttpConnectionManager;
import com.microsoft.tfs.core.httpclient.params.HttpClientParams;

/**
 * <p>
 * Adds methods to {@link HTTPClientFactory} to allow granular configuration of
 * properties on existing {@link HttpClient} instances.
 * </p>
 */
public interface ConfigurableHTTPClientFactory extends HTTPClientFactory {
    /**
     * Called from {@link #newHTTPClient()} to create and configure a new
     * {@link HttpConnectionManager} as part of creating a new
     * {@link HttpClient} instance. Subclasses may override. The default
     * behavior is to instantiate a {@link MultiThreadedHttpConnectionManager}
     * and configure it with some default parameters.
     *
     * @return a new {@link HttpConnectionManager} (must not be
     *         <code>null</code>)
     */
    public abstract HttpConnectionManager createConnectionManager(final ConnectionInstanceData connectionInstanceData);

    /**
     * Called from {@link #newHTTPClient()} to create a new {@link HttpClient}.
     * Most of the configuration should be done by other methods - see the
     * javadoc on those methods for details. This method is called after
     * {@link #createConnectionManager(ConnectionInstanceData)} and is passed
     * the {@link HttpConnectionManager} returned from that method. Subclasses
     * may override.
     *
     * @param connectionManager
     *        the {@link HttpConnectionManager} returned from
     *        {@link #createConnectionManager(ConnectionInstanceData)} (never
     *        <code>null</code> )
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} being used to supply
     *        configuration data (never <code>null</code>)
     * @return a new {@link HttpClient} instance (must not be <code>null</code>)
     */
    public abstract HttpClient createHTTPClient(
        final HttpConnectionManager connectionManager,
        final ConnectionInstanceData connectionInstanceData);

    /**
     * Called from {@link #newHTTPClient()} to configure a new
     * {@link HttpClient}'s {@link HttpClientParams}. Subclasses may override.
     * The default behavior is to configure the
     * <code>http.protocol.expect-continue</code> parameter to
     * <code>false</code> and set a custom user agent string.
     *
     * @param httpClient
     *        the {@link HttpClient} being configured (never <code>null</code>)
     * @param params
     *        the {@link HttpClientParams} to configure (never <code>null</code>
     *        )
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} being used to supply
     *        configuration data (never <code>null</code>)
     */
    public abstract void configureClientParams(
        final HttpClient httpClient,
        final HttpClientParams params,
        final ConnectionInstanceData connectionInstanceData);

    /**
     * Called from
     * {@link #configureClientParams(HttpClient, HttpClientParams, ConnectionInstanceData)}
     * to obtain a user-agent string to configure a new {@link HttpClient} with.
     *
     * @param httpClient
     *        the {@link HttpClient} being configured (never <code>null</code>)
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} being used to supply
     *        configuration data (never <code>null</code>)
     * @return the user-agent string to use, or <code>null</code> to not
     *         configure a user-agent
     */
    public abstract String getUserAgent(
        final HttpClient httpClient,
        final ConnectionInstanceData connectionInstanceData);

    /**
     * Called from {@link #newHTTPClient()} to configure credentials for a new
     * {@link HttpClient} instance. Subclasses may override. The default
     * behavior is to call {@link #createCredentials(ConnectionInstanceData)}.
     * If that method returns a non-<code>null</code> {@link Credentials}
     * object, then that object is set as the credentials for the
     * {@link HttpState}.
     *
     * @param httpClient
     *        the {@link HttpClient} being configured (never <code>null</code>)
     * @param state
     *        the {@link HttpState} to configure credentials on (never
     *        <code>null</code>)
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} being used to supply
     *        configuration data (never <code>null</code>)
     */
    public abstract void configureClientCredentials(
        final HttpClient httpClient,
        final HttpState state,
        final ConnectionInstanceData connectionInstanceData);

    /**
     * Called from {@link #newHTTPClient()} to configure proxy settings for a
     * new {@link HttpClient} instance. Subclasses may override.
     *
     * @param httpClient
     *        the {@link HttpClient} being configured (never <code>null</code>)
     * @param hostConfiguration
     *        the {@link HostConfiguration} of the new {@link HttpClient}
     *        instance (never <code>null</code>)
     * @param httpState
     *        the {@link HttpState} of the new {@link HttpClient} instance
     *        (never <code>null</code>)
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} being used to supply
     *        configuration data (never <code>null</code>)
     */
    public abstract void configureClientProxy(
        final HttpClient httpClient,
        final HostConfiguration hostConfiguration,
        final HttpState httpState,
        final ConnectionInstanceData connectionInstanceData);

    /**
     * Called from {@link #newHTTPClient()} to perform final configuration of a
     * new {@link HttpClient} instance before it is returned to the caller of
     * that method. Subclasses may override. The default behavior is to call
     * {@link #addClientToCloserThread(HttpClient)} with the new client
     * instance.
     *
     * @param httpClient
     *        the new {@link HttpClient} instance (never <code>null</code>)
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} being used to supply
     *        configuration data (never <code>null</code>)
     */
    public abstract void configureClient(
        final HttpClient httpClient,
        final ConnectionInstanceData connectionInstanceData);
}