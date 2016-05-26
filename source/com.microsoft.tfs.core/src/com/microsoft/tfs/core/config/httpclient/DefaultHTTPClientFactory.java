// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.httpclient;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.config.httpclient.internal.DefaultSSLProtocolSocketFactory;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.HostConfiguration;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpConnectionManager;
import com.microsoft.tfs.core.httpclient.HttpMethod;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.JwtCredentials;
import com.microsoft.tfs.core.httpclient.MultiThreadedHttpConnectionManager;
import com.microsoft.tfs.core.httpclient.PreemptiveUsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;
import com.microsoft.tfs.core.httpclient.params.HttpClientParams;
import com.microsoft.tfs.core.httpclient.params.HttpConnectionManagerParams;
import com.microsoft.tfs.core.httpclient.params.HttpMethodParams;
import com.microsoft.tfs.core.httpclient.protocol.Protocol;
import com.microsoft.tfs.core.httpclient.protocol.SecureProtocolSocketFactory;
import com.microsoft.tfs.core.product.CoreVersionInfo;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;
import com.microsoft.tfs.core.ws.runtime.transport.HTTPConnectionCanceller;
import com.microsoft.tfs.core.ws.runtime.transport.IdleHTTPConnectionCloser;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A default implementation of the {@link HTTPClientFactory} interface that uses
 * a {@link ConnectionInstanceData} to configure an {@link HttpClient}. This
 * implementation is intended to be subclassed and provides a number of hooks
 * that subclasses may override.
 * </p>
 *
 * @see HTTPClientFactory
 * @see HttpClient
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class DefaultHTTPClientFactory implements ConfigurableHTTPClientFactory {
    public static final String CONNECT_TIMEOUT_SECONDS_PROPERTY = "com.microsoft.tfs.core.connectTimeoutSeconds"; //$NON-NLS-1$
    public static final int CONNECT_TIMEOUT_SECONDS_DEFAULT = 30;

    public static final String SOCKET_TIMEOUT_SECONDS_PROPERTY = "com.microsoft.tfs.core.socketTimeoutSeconds"; //$NON-NLS-1$
    public static final int SOCKET_TIMEOUT_SECONDS_DEFAULT = 60 * 30;

    public static final String MAX_TOTAL_CONNECTIONS_PROPERTY = "com.microsoft.tfs.core.maxTotalConnections"; //$NON-NLS-1$
    public static final int MAX_TOTAL_CONNECTIONS_DEFAULT = 40;

    public static final String MAX_CONNECTIONS_PER_HOST_PROPERTY = "com.microsoft.tfs.core.maxConnectionsPerHost"; //$NON-NLS-1$
    public static final int MAX_CONNECTIONS_PER_HOST_DEFAULT = 10;

    public static final String DISABLE_HTTP_CANCEL_THREAD_PROPERTY = "com.microsoft.tfs.core.disableCancelThread"; //$NON-NLS-1$

    public static final String ECLIPSE_GROUP_NAME = "Eclipse Platform"; //$NON-NLS-1$

    /**
     * The maximum length in characters of the extra user agent text part.
     */
    public static final int USER_AGENT_EXTRA_TEXT_MAX_CHARS = 30;

    /**
     * The maximum length in characters of the operating system info part.
     */
    public static final int USER_AGENT_OS_INFO_MAX_CHARS = 30;

    private static final Log log = LogFactory.getLog(DefaultHTTPClientFactory.class);

    /**
     * A service thread that closes connections that have been idle a long time
     * to improve quality of service on broken networks (where TCP resets happen
     * often).
     */
    private static final IdleHTTPConnectionCloser closerThread = new IdleHTTPConnectionCloser();

    /**
     * A service thread that aborts {@link HttpMethod}s when a monitor object
     * signals that the method should be canceled.
     */
    private static final HTTPConnectionCanceller cancelThread = new HTTPConnectionCanceller();

    static {
        closerThread.start();

        if (System.getProperty(DISABLE_HTTP_CANCEL_THREAD_PROPERTY) == null) {
            cancelThread.start();
        }
    }

    private final ConnectionInstanceData connectionInstanceData;

    /**
     * Creates a new {@link DefaultHTTPClientFactory} that will data contained
     * in the specified instance data to configure {@link HttpClient}s.
     *
     * @param serverURI
     *        the {@link URI} that will be connected to (must not be
     *        <code>null</code>)
     */
    public DefaultHTTPClientFactory(final ConnectionInstanceData connectionInstanceData) {
        Check.notNull(connectionInstanceData, "connectionInstanceData"); //$NON-NLS-1$
        this.connectionInstanceData = connectionInstanceData;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.config.HttpClientFactory#newHttpClient()
     */
    @Override
    public HttpClient newHTTPClient() {
        final HttpConnectionManager connectionManager = createConnectionManager(connectionInstanceData);

        final HttpClient httpClient = createHTTPClient(connectionManager, connectionInstanceData);

        configureClientParams(httpClient, httpClient.getParams(), connectionInstanceData);

        configureClientCredentials(httpClient, httpClient.getState(), connectionInstanceData);

        configureClientProxy(
            httpClient,
            httpClient.getHostConfiguration(),
            httpClient.getState(),
            connectionInstanceData);

        configureClient(httpClient, connectionInstanceData);

        logHTTPClientConfiguration(httpClient);

        return httpClient;
    }

    private void logHTTPClientConfiguration(final HttpClient httpClient) {
        final StringBuffer configurationMessage = new StringBuffer();

        configurationMessage.append(MessageFormat.format(
            "HttpClient configured for {0}", //$NON-NLS-1$
            connectionInstanceData.getServerURI()));

        final Credentials credentials = httpClient.getState().getCredentials(AuthScope.ANY);
        if (credentials != null) {
            if (credentials instanceof DefaultNTCredentials) {
                configurationMessage.append(", authenticating as logged in user"); //$NON-NLS-1$
            } else if (credentials instanceof UsernamePasswordCredentials) {
                configurationMessage.append(MessageFormat.format(
                    ", authenticating as {0}", //$NON-NLS-1$
                    ((UsernamePasswordCredentials) credentials).getUsername()));
            } else if (credentials instanceof CookieCredentials) {
                configurationMessage.append(", authenticating with ACS token"); //$NON-NLS-1$
            } else if (credentials instanceof JwtCredentials) {
                configurationMessage.append(", authenticating with JWT token"); //$NON-NLS-1$
            }
        }

        if (httpClient.getHostConfiguration().getProxyHost() != null) {
            configurationMessage.append(MessageFormat.format(
                ", proxy={0}", //$NON-NLS-1$
                httpClient.getHostConfiguration().getProxyHost()));

            if (httpClient.getHostConfiguration().getProxyPort() != -1) {
                configurationMessage.append(MessageFormat.format(
                    ":{0}", //$NON-NLS-1$
                    Integer.toString(httpClient.getHostConfiguration().getProxyPort())));
            }
        }

        log.info(configurationMessage.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose(final HttpClient httpClient) {
        log.debug("Disposing"); //$NON-NLS-1$
        final HttpConnectionManager connectionManager = httpClient.getHttpConnectionManager();
        if (!(connectionManager instanceof MultiThreadedHttpConnectionManager)) {
            log.debug("Nothing to dispose: connectionManager is not an instance of MultiThreadedHttpConnectionManager"); //$NON-NLS-1$
            return;
        }

        final MultiThreadedHttpConnectionManager multiThreadedConnectionManager =
            (MultiThreadedHttpConnectionManager) connectionManager;

        log.debug("Shutting down the Multi Threaded Http Connection Manager"); //$NON-NLS-1$
        multiThreadedConnectionManager.shutdown();

        log.debug("Disposed"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpConnectionManager createConnectionManager(final ConnectionInstanceData connectionInstanceData) {
        final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

        final HttpConnectionManagerParams params = connectionManager.getParams();

        /*
         * The max total connection limit is higher than the per-host limit to
         * account for multiple connections (to different TFS servers) that
         * share an HttpClient.
         */
        params.setMaxTotalConnections(
            Integer.getInteger(MAX_TOTAL_CONNECTIONS_PROPERTY, MAX_TOTAL_CONNECTIONS_DEFAULT));
        params.setMaxConnectionsPerHost(
            HostConfiguration.ANY_HOST_CONFIGURATION,
            Integer.getInteger(MAX_CONNECTIONS_PER_HOST_PROPERTY, MAX_CONNECTIONS_PER_HOST_DEFAULT));

        /*
         * Set the connection timeout.
         */
        params.setConnectionTimeout(
            Integer.getInteger(CONNECT_TIMEOUT_SECONDS_PROPERTY, CONNECT_TIMEOUT_SECONDS_DEFAULT) * 1000);

        return connectionManager;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This method will modify the HTTPClient {@link Protocol} registration for
     * "https" protocols to use a custom {@link SecureProtocolSocketFactory}
     * that will allow users to accept untrusted certificates.
     * </p>
     */
    @Override
    public HttpClient createHTTPClient(
        final HttpConnectionManager connectionManager,
        final ConnectionInstanceData connectionInstanceData) {
        /*
         * Register our SSL socket factory with HTTPClient, allowing us to
         * accept untrusted certificates.
         */
        Protocol.registerProtocol("https", new Protocol("https", new DefaultSSLProtocolSocketFactory(), 443)); //$NON-NLS-1$ //$NON-NLS-2$

        return new HttpClient(connectionManager);
    }

    /**
     * Called from
     * {@link #createHTTPClient(HttpConnectionManager, ConnectionInstanceData)}
     * to test whether an {@link HttpClient} created for the specified
     * connection should be configured to accept untrusted SSL certificates.
     * Subclasses may override. The default behavior is to check an environment
     * variable. If the value of the environment variable
     * {@link EnvironmentVariables#ACCEPT_UNTRUSTED_CERTIFICATES} is set,
     * <code>true</code> is returned. Otherwise, <code>false</code> is returned.
     *
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} being used to supply
     *        configuration data (never <code>null</code>)
     * @return <code>true</code> to configure the new {@link HttpClient}
     *         instance to accept untrusted SSL certificates
     */
    protected boolean shouldAcceptUntrustedCertificates(final ConnectionInstanceData connectionInstanceData) {
        /*
         * If the environment variable is set, we accept untrusted certificates.
         */

        if (PlatformMiscUtils.getInstance().getEnvironmentVariable(
            EnvironmentVariables.ACCEPT_UNTRUSTED_CERTIFICATES) != null) {
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureClientParams(
        final HttpClient httpClient,
        final HttpClientParams params,
        final ConnectionInstanceData connectionInstanceData) {
        params.setBooleanParameter("http.protocol.expect-continue", false); //$NON-NLS-1$

        params.setParameter(
            HttpMethodParams.SO_TIMEOUT,
            Integer.getInteger(SOCKET_TIMEOUT_SECONDS_PROPERTY, SOCKET_TIMEOUT_SECONDS_DEFAULT) * 1000);

        /*
         * Setup the user agent
         */
        final String userAgent = getUserAgent(httpClient, connectionInstanceData);

        if (userAgent != null) {
            params.setParameter(HttpMethodParams.USER_AGENT, userAgent);
        }

        /*
         * Set the SSL socket factory to accept untrusted certificates, if
         * requested.
         */
        if (shouldAcceptUntrustedCertificates(connectionInstanceData)) {
            params.setBooleanParameter(
                DefaultSSLProtocolSocketFactory.ACCEPT_UNTRUSTED_CERTIFICATES_PARAMETER,
                Boolean.TRUE);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Use {@link #getUserAgentExtraString(HttpClient, ConnectionInstanceData)}
     * to customize thet user agent string.
     */
    @Override
    public final String getUserAgent(final HttpClient httpClient, final ConnectionInstanceData connectionInstanceData) {
        /*
         * This method is final to ensure the format can be parsed for SQM data
         * on the server. Some fields are truncated to help keep the string
         * under 129 chars for SQM, though truncation should be rare (those
         * fields usually aren't so long).
         */

        final ProductName productName = ProductInformation.getCurrent();

        // Like "Team Explorer Everywhere"
        /*
         * Changed for testing purposes to avoid legacy=1 parameter in the URI
         * generated by TFS as workaround for old clients
         */
        final String applicationName;
        if (EnvironmentVariables.getBoolean(EnvironmentVariables.USE_LEGACY_MSA, false)) {
            /*
             * Like "Team Explorer Everywhere"
             *
             * The legacy=1 parameter in the URI will be generated by TFS as
             * workaround for old clients
             */
            applicationName = productName.getFamilyShortNameNOLOC();
        } else {
            /*
             * The javascriptNotify will be used by TFS to stop redirection
             * chain in the Federated Authentication sequence and provide
             * FedAuth cookies to the new client.
             */
            applicationName = "Team  Explorer  Everywhere"; //$NON-NLS-1$
        }

        // Like "11"
        final String sqmID = Integer.toString(productName.getSQMID());

        // Like "Plugin" or "CLC" or "SDK"
        final String shortName = productName.getProductShortNameNOLOC();

        // Like "11.0.0.201108162203" but could be a bit longer like
        // "12.34.45.201108162203"
        final String version = MessageFormat.format(
            "{0}.{1}.{2}.{3}", //$NON-NLS-1$
            CoreVersionInfo.getMajorVersion(),
            CoreVersionInfo.getMinorVersion(),
            CoreVersionInfo.getServiceVersion(),
            CoreVersionInfo.getBuildVersion());

        // Like "Eclipse_sdk 4.2"
        final String productInfo = getProductInformation(shortName);

        // Like "1.5.2"
        final String javaVersion = System.getProperty("java.version"); //$NON-NLS-1$

        // Like "Linux amd64 2.6.38-8-generic" or "Windows 7 amd64 6.1"
        final String osInfo = truncate(MessageFormat.format(
            "{0} {1} {2}", //$NON-NLS-1$
            System.getProperty("os.name"), //$NON-NLS-1$
            System.getProperty("os.arch"), //$NON-NLS-1$
            System.getProperty("os.version")), USER_AGENT_OS_INFO_MAX_CHARS); //$NON-NLS-1$

        // Format the first part, append more
        final StringBuffer ua = new StringBuffer(MessageFormat.format(
            "{0}, SKU:{1} ({2} {3} {4}{5}; {6}", //$NON-NLS-1$
            applicationName,
            sqmID,
            shortName,
            version,
            productInfo,
            javaVersion,
            osInfo));

        // Extra goes last (if present)
        final String extra = getUserAgentExtraString(httpClient, connectionInstanceData);
        if (extra != null) {
            ua.append("; "); //$NON-NLS-1$
            ua.append(truncate(extra, USER_AGENT_EXTRA_TEXT_MAX_CHARS));
        }

        ua.append(")"); //$NON-NLS-1$

        return ua.toString();
    }

    private String truncate(final String s, final int maxLength) {
        if (s == null || s.length() == 0 || maxLength < 1 || s.length() <= maxLength) {
            return s;
        }

        return s.substring(0, maxLength);
    }

    /**
     * <p>
     * Subclasses can override to provide extra text that gets appended to the
     * parenthetical part of the user agent HTTP header. If <code>null</code> or
     * empty string is returned no extra text is appended. The returned string
     * should be {@value #USER_AGENT_EXTRA_TEXT_MAX_CHARS} characters or less
     * (it will be truncated if it exceeds this limit).
     * </p>
     * <p>
     * The user agent header is formatted like:
     * </p>
     *
     * <pre>
     * ProductFamily, SKU:XX (ProductName 1.2.3.4567890; OS Arch Version<b>; extra text goes here if present</b>)
     * </pre>
     *
     * @param httpClient
     *        the {@link HttpClient} being configured (must not be
     *        <code>null</code>)
     * @param connectionInstanceData
     *        the connection instance data (must not be <code>null</code>)
     * @return a string to put at the end of the parenthetical part of the user
     *         agent header ({@value #USER_AGENT_EXTRA_TEXT_MAX_CHARS}
     *         characaters or less), or <code>null</code> or the emptry string
     *         to omit the extra part
     */
    protected String getUserAgentExtraString(
        final HttpClient httpClient,
        final ConnectionInstanceData connectionInstanceData) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureClientCredentials(
        final HttpClient httpClient,
        final HttpState state,
        final ConnectionInstanceData connectionInstanceData) {
        final Credentials credentials = createCredentials(connectionInstanceData);

        if (credentials != null) {
            state.setCredentials(AuthScope.ANY, credentials);
        }

        /*
         * Only do preemptive authentication for Cookie and JWT Credentials and
         * special PreemptiveUsernamePasswordCredentials. These credentials may
         * be set on the HttpClient in response to federated authentication
         * challenges later.
         */
        httpClient.getParams().setPreemptiveAuthenticationTypes(new Class[] {
            CookieCredentials.class,
            JwtCredentials.class,
            PreemptiveUsernamePasswordCredentials.class
        });
    }

    /**
     * Called from
     * {@link #configureClientCredentials(HttpClient, HttpState, ConnectionInstanceData)}
     * to create a new {@link Credentials} instance for the specified
     * {@link ConnectionInstanceData}. Subclasses may override. The default
     * behavior is to simply return default credentials.
     *
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} to get configuration data from
     *        (never <code>null</code>)
     * @return a {@link Credentials} object or <code>null</code> to not use
     *         {@link Credentials}
     */
    protected Credentials createCredentials(final ConnectionInstanceData connectionInstanceData) {
        return connectionInstanceData.getCredentials() != null ? connectionInstanceData.getCredentials()
            : new DefaultNTCredentials();
    }

    protected Credentials createProxyCredentials(final ConnectionInstanceData connectionInstanceData) {
        return new DefaultNTCredentials();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureClientProxy(
        final HttpClient httpClient,
        final HostConfiguration hostConfiguration,
        final HttpState httpState,
        final ConnectionInstanceData connectionInstanceData) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureClient(final HttpClient httpClient, final ConnectionInstanceData connectionInstanceData) {
        addClientToCloserThread(httpClient);
    }

    /**
     * Called to add the specified {@link HttpClient} instance to a static
     * thread that will handle idle connection closing.
     *
     * @param httpClient
     *        the {@link HttpClient} instance to add (must not be
     *        <code>null</code>)
     */
    protected final void addClientToCloserThread(final HttpClient httpClient) {
        closerThread.addClient(httpClient);
    }

    /**
     * It returns two strings, the first is the eclipse provider and the second
     * is the eclipse version.
     */
    private final String getProductInformation(final String shortName) {

        final String[] productInfo = new String[2];
        String productVendorVersion = ""; //$NON-NLS-1$

        // collect this information only in case of running as eclipse plugin
        if (shortName.equals(ProductName.PLUGIN)) {
            productInfo[0] = Platform.getProduct().getName().replace(' ', '_');

            final IBundleGroupProvider[] providers = Platform.getBundleGroupProviders();

            if (providers != null) {
                for (final IBundleGroupProvider provider : providers) {
                    final IBundleGroup[] groups = provider.getBundleGroups();
                    for (final IBundleGroup group : groups) {
                        final String groupName = group.getName();
                        final String groupVersion = group.getVersion();

                        if (groupName.equalsIgnoreCase(ECLIPSE_GROUP_NAME)) {
                            final int index = groupVersion.indexOf(".v"); //$NON-NLS-1$
                            if (index > 0) {
                                productInfo[1] = groupVersion.substring(0, index);
                            } else {
                                productInfo[1] = groupVersion;
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (productInfo[0] != null && productInfo[1] != null) {
            productVendorVersion = MessageFormat.format("{0} {1} ", productInfo[0], productInfo[1]); //$NON-NLS-1$
        }
        return productVendorVersion;
    }
}
