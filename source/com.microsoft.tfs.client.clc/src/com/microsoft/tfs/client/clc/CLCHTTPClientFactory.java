// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.IllegalConfigurationException;
import com.microsoft.tfs.core.config.httpclient.DefaultHTTPClientFactory;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.HostConfiguration;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;
import com.microsoft.tfs.core.httpclient.protocol.Protocol;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.CollatorFactory;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;
import com.microsoft.tfs.util.StringUtil;

public class CLCHTTPClientFactory extends DefaultHTTPClientFactory {
    private static final Log log = LogFactory.getLog(CLCHTTPClientFactory.class);

    private final CredentialsManager credentialsManager;

    public CLCHTTPClientFactory(
        final ConnectionInstanceData connectionInstanceData,
        final CredentialsManager credentialsManager) {
        super(connectionInstanceData);

        Check.notNull(credentialsManager, "credentialsManager"); //$NON-NLS-1$
        this.credentialsManager = credentialsManager;
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
        CLCHTTPProxyConfiguration proxyConfiguration = null;

        /*
         * Try using the http.proxyHost / https.proxyHost system properties.
         * These must have been configured for our application by editing the
         * launcher (on Unix/Windows systems) or were set by the JVM from system
         * defaults. (on Mac OS)
         */
        proxyConfiguration =
            configureClientProxyFromProperties(httpClient, hostConfiguration, httpState, connectionInstanceData);

        /*
         * Try environment variables.
         */
        if (proxyConfiguration == null) {
            proxyConfiguration =
                configureClientProxyFromEnvironment(httpClient, hostConfiguration, httpState, connectionInstanceData);
        }

        /*
         * Return early if still no proxy configured.
         */
        if (proxyConfiguration == null) {
            final String messageFormat = "Environment variables {0},{1} not set, no global proxy configured"; //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                EnvironmentVariables.HTTP_PROXY_URL,
                EnvironmentVariables.HTTP_PROXY_URL_ALTERNATE);
            log.debug(message);
            return;
        }

        final String messageFormat = "Using global proxy URL {0}:{1}"; //$NON-NLS-1$
        final String message = MessageFormat.format(
            messageFormat,
            proxyConfiguration.getHost(),
            Integer.toString(proxyConfiguration.getPort()));
        log.debug(message);

        hostConfiguration.setProxy(proxyConfiguration.getHost(), proxyConfiguration.getPort());

        if (proxyConfiguration.getUsername() != null && proxyConfiguration.getPassword() != null) {
            httpState.setProxyCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(proxyConfiguration.getUsername(), proxyConfiguration.getPassword()));
        } else {
            httpState.setProxyCredentials(AuthScope.ANY, new DefaultNTCredentials());
        }
    }

    private CLCHTTPProxyConfiguration configureClientProxyFromProperties(
        final HttpClient httpClient,
        final HostConfiguration hostConfiguration,
        final HttpState httpState,
        final ConnectionInstanceData connectionInstanceData) {
        String proxyHost = null;
        String proxyPort = null;
        String nonProxyHosts = null;

        log.debug("Trying to configure proxy from JVM properties:"); //$NON-NLS-1$

        if ("http".equalsIgnoreCase(connectionInstanceData.getServerURI().getScheme())) //$NON-NLS-1$
        {
            proxyHost = System.getProperty("http.proxyHost"); //$NON-NLS-1$
            logIfDefined("http.proxyHost", proxyHost); //$NON-NLS-1$
            proxyPort = System.getProperty("http.proxyPort"); //$NON-NLS-1$
            logIfDefined("http.proxyPort", proxyPort); //$NON-NLS-1$
            nonProxyHosts = System.getProperty("http.nonProxyHosts"); //$NON-NLS-1$
            logIfDefined("http.nonProxyHosts", nonProxyHosts); //$NON-NLS-1$
        } else if ("https".equalsIgnoreCase(connectionInstanceData.getServerURI().getScheme())) //$NON-NLS-1$
        {
            proxyHost = System.getProperty("https.proxyHost"); //$NON-NLS-1$
            logIfDefined("https.proxyHost", proxyHost); //$NON-NLS-1$
            proxyPort = System.getProperty("https.proxyPort"); //$NON-NLS-1$
            logIfDefined("https.proxyPort", proxyPort); //$NON-NLS-1$
            nonProxyHosts = System.getProperty("https.nonProxyHosts"); //$NON-NLS-1$
            logIfDefined("https.nonProxyHosts", nonProxyHosts); //$NON-NLS-1$
        }

        if (!StringUtil.isNullOrEmpty(proxyHost)
            && !hostExcludedFromProxyProperties(connectionInstanceData.getServerURI(), nonProxyHosts)) {
            int proxyPortValue = -1;

            if (proxyPort != null && proxyPort.length() > 0) {
                try {
                    proxyPortValue = Integer.parseInt(proxyPort);
                } catch (final NumberFormatException e) {
                    log.warn(MessageFormat.format("Could not parse proxy port {0}, using default", proxyPort), e); //$NON-NLS-1$
                }
            }

            try {
                final URI proxyURI = new URI("http", null, proxyHost, proxyPortValue, "/", null, null); //$NON-NLS-1$ //$NON-NLS-2$

                /* Make sure proxy host is well-formed */
                if (proxyURI.getHost() == null) {
                    final String messageFormat =
                        Messages.getString("CLCHttpClientFactory.ProxyURLDoesNotContainValidHostnameFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, proxyURI.toString());
                    log.warn(message);
                    throw new IllegalConfigurationException(message);
                }

                /* See if we have credentials cached for this proxy */
                final CachedCredentials proxyCredentials = credentialsManager.getCredentials(proxyURI);

                final String username = proxyCredentials != null ? proxyCredentials.getUsername() : null;
                final String password = proxyCredentials != null ? proxyCredentials.getPassword() : null;

                return new CLCHTTPProxyConfiguration(proxyHost, proxyPortValue, username, password);
            } catch (final URISyntaxException e) {
                log.warn("Could not parse proxy URI, proxy will not be configured", e); //$NON-NLS-1$
            }
        }

        log.debug("    proxy is not defined in JVM properties or excluded"); //$NON-NLS-1$
        return null;
    }

    private CLCHTTPProxyConfiguration configureClientProxyFromEnvironment(
        final HttpClient httpClient,
        final HostConfiguration hostConfiguration,
        final HttpState httpState,
        final ConnectionInstanceData connectionInstanceData) {
        String proxyUrl = null;
        String nonProxyHosts;

        log.debug("Trying to configure proxy from environment variables:"); //$NON-NLS-1$

        /*
         * If we're doing HTTPS, check for the presence of an HTTPS proxy
         * environment variable.
         */
        if ("https".equalsIgnoreCase(connectionInstanceData.getServerURI().getScheme())) //$NON-NLS-1$
        {
            proxyUrl = PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.HTTPS_PROXY_URL);
            logIfDefined(EnvironmentVariables.HTTPS_PROXY_URL, proxyUrl);

            if (StringUtil.isNullOrEmpty(proxyUrl)) {
                proxyUrl = PlatformMiscUtils.getInstance().getEnvironmentVariable(
                    EnvironmentVariables.HTTPS_PROXY_URL_ALTERNATE);
                logIfDefined(EnvironmentVariables.HTTPS_PROXY_URL_ALTERNATE, proxyUrl);
            }
        }

        /*
         * Check for the presence of an HTTP proxy environment variable and use
         * that as the global proxy. (lynx documented the environment variable
         * as lower case "http_proxy", so we need to check both the variable and
         * its alternate.)
         *
         * (Note, we have always tried using the HTTP_PROXY environment variable
         * for HTTPS connections, so continue to support this.)
         */
        if (StringUtil.isNullOrEmpty(proxyUrl)) {
            proxyUrl = PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.HTTP_PROXY_URL);
            logIfDefined(EnvironmentVariables.HTTP_PROXY_URL, proxyUrl);

            if (StringUtil.isNullOrEmpty(proxyUrl)) {
                proxyUrl = PlatformMiscUtils.getInstance().getEnvironmentVariable(
                    EnvironmentVariables.HTTP_PROXY_URL_ALTERNATE);
                logIfDefined(EnvironmentVariables.HTTP_PROXY_URL_ALTERNATE, proxyUrl);
            }
        }

        if (StringUtil.isNullOrEmpty(proxyUrl)) {
            log.debug("    proxy is not defined in environment variables"); //$NON-NLS-1$
            return null;
        }

        /*
         * Check against the NO_PROXY environment variable. (lynx also
         * documented "no_proxy" as lower case here, so we need to check both
         * the variable and its alternate.)
         */
        nonProxyHosts = PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.NO_PROXY_HOSTS);
        logIfDefined(EnvironmentVariables.NO_PROXY_HOSTS, nonProxyHosts);

        if (StringUtil.isNullOrEmpty(nonProxyHosts)) {
            nonProxyHosts =
                PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.NO_PROXY_HOSTS_ALTERNATE);
            logIfDefined(EnvironmentVariables.NO_PROXY_HOSTS_ALTERNATE, nonProxyHosts);
        }

        if (hostExcludedFromProxyEnvironment(connectionInstanceData.getServerURI(), nonProxyHosts)) {
            log.debug("    proxy is defined, but excluded in environment variables"); //$NON-NLS-1$
            return null;
        }

        URI proxyURI;

        try {
            proxyURI = new URI(proxyUrl);
        } catch (final URISyntaxException e) {
            final String messageFormat = Messages.getString("CLCHttpClientFactory.IllegalProxyURLFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, proxyUrl);
            log.warn(message, e);
            throw new IllegalConfigurationException(message, e);
        }

        if (proxyURI.getHost() == null) {
            final String messageFormat = Messages.getString("CLCHttpClientFactory.IllegalProxyURLFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, proxyUrl);
            log.warn(message);
            throw new IllegalConfigurationException(message);
        }

        String username = null, password = null;
        if (proxyURI.getRawUserInfo() != null) {
            final String[] userInfo = proxyURI.getRawUserInfo().split(":", 2); //$NON-NLS-1$

            try {
                username = URLDecoder.decode(userInfo[0], "UTF-8"); //$NON-NLS-1$
                password = URLDecoder.decode(userInfo[1], "UTF-8"); //$NON-NLS-1$
            } catch (final Exception e) {
                log.warn("Could not decode user info as UTF-8", e); //$NON-NLS-1$
            }
        } else {
            /*
             * If the proxy credentials were NOT specified in the URI itself,
             * look up the credentials
             */
            final CachedCredentials proxyCredentials = credentialsManager.getCredentials(proxyURI);

            username = proxyCredentials != null ? proxyCredentials.getUsername() : null;
            password = proxyCredentials != null ? proxyCredentials.getPassword() : null;
        }

        return new CLCHTTPProxyConfiguration(proxyURI.getHost(), proxyURI.getPort(), username, password);
    }

    private void logIfDefined(final String name, final String value) {
        if (!StringUtil.isNullOrEmpty(value)) {
            log.debug("    " + name + "=" + value); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Determines whether the given host should be proxied or not, based on the
     * pipe-separated list of wildcards to not proxy (generally taken from the
     * <code>http.nonProxyHosts</code> system property.)
     *
     * @param host
     *        the host to query (not <code>null</code>)
     * @param nonProxyHosts
     *        the pipe-separated list of hosts (or wildcards) that should not be
     *        proxied, or <code>null</code> if all hosts are proxied
     * @return <code>true</code> if the host should be proxied,
     *         <code>false</code> otherwise
     */
    static boolean hostExcludedFromProxyProperties(final URI serverURI, final String nonProxyHosts) {
        if (serverURI == null || serverURI.getHost() == null || nonProxyHosts == null) {
            return false;
        }

        for (final String nonProxyHost : nonProxyHosts.split("\\|")) //$NON-NLS-1$
        {
            /*
             * Note: for wildcards, the java specification says that the host
             * "may start OR end with a *" (emphasis: mine).
             */
            if (nonProxyHost.startsWith("*") //$NON-NLS-1$
                && LocaleInvariantStringHelpers.caseInsensitiveEndsWith(
                    serverURI.getHost(),
                    nonProxyHost.substring(1))) {
                return true;
            } else if (nonProxyHost.endsWith("*") //$NON-NLS-1$
                && LocaleInvariantStringHelpers.caseInsensitiveStartsWith(
                    serverURI.getHost(),
                    nonProxyHost.substring(0, nonProxyHost.length() - 1))) {
                return true;
            } else if (CollatorFactory.getCaseInsensitiveCollator().equals(serverURI.getHost(), nonProxyHost)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether the given host should be proxied or not, based on the
     * comma-separated list of wildcards to not proxy (generally taken from the
     * <code>http.nonProxyHosts</code> system property.
     *
     * @param host
     *        the host to query (not <code>null</code>)
     * @param nonProxyHosts
     *        the pipe-separated list of hosts (or wildcards) that should not be
     *        proxied, or <code>null</code> if all hosts are proxied
     * @return <code>true</code> if the host should be proxied,
     *         <code>false</code> otherwise
     */
    static boolean hostExcludedFromProxyEnvironment(final URI serverURI, String nonProxyHosts) {
        if (serverURI == null || serverURI.getHost() == null || nonProxyHosts == null) {
            return false;
        }

        nonProxyHosts = nonProxyHosts.trim();
        if (nonProxyHosts.length() == 0) {
            return false;
        }

        /*
         * The no_proxy setting may be '*' to indicate nothing is proxied.
         * However, this is the only allowable use of a wildcard.
         */
        if ("*".equals(nonProxyHosts)) //$NON-NLS-1$
        {
            return true;
        }

        final String serverHost = serverURI.getHost();

        /* Map default ports to the appropriate default. */
        int serverPort = serverURI.getPort();

        if (serverPort == -1) {
            try {
                serverPort = Protocol.getProtocol(serverURI.getScheme().toLowerCase()).getDefaultPort();
            } catch (final IllegalStateException e) {
                serverPort = 80;
            }
        }

        for (String nonProxyHost : nonProxyHosts.split(",")) //$NON-NLS-1$
        {
            int nonProxyPort = -1;

            if (nonProxyHost.contains(":")) //$NON-NLS-1$
            {
                final String[] nonProxyParts = nonProxyHost.split(":", 2); //$NON-NLS-1$

                nonProxyHost = nonProxyParts[0];

                try {
                    nonProxyPort = Integer.parseInt(nonProxyParts[1]);
                } catch (final Exception e) {
                    log.warn(MessageFormat.format(
                        "Could not parse port in non_proxy setting: {0}, ignoring port", //$NON-NLS-1$
                        nonProxyParts[1]));
                }
            }

            /*
             * If the no_proxy entry specifies a port, match it exactly. If it
             * does not, this means to match all ports.
             */
            if (nonProxyPort != -1 && serverPort != nonProxyPort) {
                continue;
            }

            /*
             * Otherwise, the nonProxyHost portion is treated as the trailing
             * DNS entry
             */
            if (LocaleInvariantStringHelpers.caseInsensitiveEndsWith(serverHost, nonProxyHost)) {
                return true;
            }
        }

        return false;
    }

    private static class CLCHTTPProxyConfiguration {
        private final String host;
        private final int port;

        private final String username;
        private final String password;

        public CLCHTTPProxyConfiguration(
            final String host,
            final int port,
            final String username,
            final String password) {
            Check.notNull(host, "host"); //$NON-NLS-1$

            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
