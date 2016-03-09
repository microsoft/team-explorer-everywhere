// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.DefaultConnectionAdvisor;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.config.auth.DefaultTransportRequestHandler;
import com.microsoft.tfs.core.config.client.ClientFactory;
import com.microsoft.tfs.core.config.client.DefaultClientFactory;
import com.microsoft.tfs.core.config.httpclient.ConfigurableHTTPClientFactory;
import com.microsoft.tfs.core.config.httpclient.DefaultHTTPClientFactory;
import com.microsoft.tfs.core.config.httpclient.HTTPClientFactory;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.config.serveruri.DefaultServerURIProvider;
import com.microsoft.tfs.core.config.serveruri.ServerURIProvider;
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettings;
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.tfproxy.TFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.webservice.DefaultWebServiceFactory;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.HostConfiguration;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.core.persistence.VersionedVendorFilesystemPersistenceStore;
import com.microsoft.tfs.core.util.CredentialsUtils;
import com.microsoft.tfs.core.util.URIUtils;

/**
 * This sample demonstrates using a custom {@link ConnectionAdvisor} to change
 * how the TFS SDK for Java connects to the server and performs other work.
 * <p>
 * Most applications should <b>not</b> specify a {@link ConnectionAdvisor} when
 * constructing {@link TFSTeamProjectCollection} and
 * {@link TFSConfigurationServer} objects. Instead, applications should use the
 * constructors that use a default {@link ConnectionAdvisor}, which is
 * sufficient for most applications.
 * <p>
 * However, you may choose to implement a custom {@link ConnectionAdvisor} if
 * your application needs to:
 * <p>
 * <ul>
 * <li>Configure where the TFS SDK for Java stores settings and cache files</li>
 * <li>Control how authentication to the server happens</li>
 * <li>Specify whether an HTTP proxy should be used</li>
 * <li>Specify which TFS version control proxy should be used</li>
 * <li>Set locale and time zone information that is sent to the server and is
 * used to format output</li>
 * <li>Customize the HTTP user agent string that gets sent to the server</li>
 * </ul>
 * <p>
 * These tasks are demonstrated in this sample.
 */
public class ConnectionAdvisorSample {
    public static void main(final String[] args) throws InterruptedException {
        /*
         * Our sample advisor uses a custom persistence store location.
         */
        URI httpProxyURI = null;

        if (ConsoleSettings.HTTP_PROXY_URL != null && ConsoleSettings.HTTP_PROXY_URL.length() > 0) {
            try {
                httpProxyURI = new URI(ConsoleSettings.HTTP_PROXY_URL);
            } catch (final URISyntaxException e) {
                // Do Nothing
            }
        }
        final SampleConnectionAdvisor advisor = new SampleConnectionAdvisor(httpProxyURI);

        /*
         * Connect with the sample advisor and use the version control client to
         * create some cache and settings files.
         */
        final TFSTeamProjectCollection tpc =
            new TFSTeamProjectCollection(URIUtils.newURI(ConsoleSettings.COLLECTION_URL), createCredentials(), advisor);
        tpc.getVersionControlClient().getItem("$/"); //$NON-NLS-1$

        System.out.println("Connection cache persistence store: " //$NON-NLS-1$
            + ((VersionedVendorFilesystemPersistenceStore) tpc.getPersistenceStoreProvider().getCachePersistenceStore()).getStoreFile());

        System.out.println("Connection configuration persistence store: " //$NON-NLS-1$
            + ((VersionedVendorFilesystemPersistenceStore) tpc.getPersistenceStoreProvider().getConfigurationPersistenceStore()).getStoreFile());

        System.out.println("Log file persistence store: " //$NON-NLS-1$
            + ((VersionedVendorFilesystemPersistenceStore) tpc.getPersistenceStoreProvider().getLogPersistenceStore()).getStoreFile());

        System.out.println("Connection time zone: " //$NON-NLS-1$
            + tpc.getTimeZone().getDisplayName(false, TimeZone.SHORT, Locale.US));

        System.out.println("Connection locale: " + tpc.getLocale().toString()); //$NON-NLS-1$
    }

    private static Credentials createCredentials() {
        final Credentials credentials;

        if ((ConsoleSettings.USERNAME == null || ConsoleSettings.USERNAME.length() == 0)
            && (CredentialsUtils.supportsDefaultCredentials())) {
            credentials = new DefaultNTCredentials();
        } else {
            credentials = new UsernamePasswordCredentials(ConsoleSettings.USERNAME, ConsoleSettings.PASSWORD);
        }

        return credentials;
    }

    /**
     * This class implements all methods of {@link ConnectionAdvisor} for
     * demonstration purposes. Applications are encouraged to extend
     * {@link DefaultConnectionAdvisor} instead and change the minimum amount of
     * behavior. In turn, most {@link ConnectionAdvisor} methods return types
     * for which there is a default implementation. Read the documentation on
     * these default types carefully for hints on implementation.
     */
    static class SampleConnectionAdvisor implements ConnectionAdvisor {
        private static final TimeZone UTC = TimeZone.getTimeZone("UTC"); //$NON-NLS-1$

        private final URI httpProxyURI;

        public SampleConnectionAdvisor() {
            this(null);
        }

        public SampleConnectionAdvisor(final URI httpProxyURI) {

            this.httpProxyURI = httpProxyURI;
        }

        /**
         * Always returns UTC. The {@link DefaultConnectionAdvisor} returns the
         * default time zone.
         *
         * The {@link TimeZone} is used by the core libraries to calculate time
         * offsets between the client and server (for build scheduling, version
         * control history, and more). A {@link ConnectionAdvisor} can get its
         * time zone from any source. For example, an application may wish to
         * connect to TFS on behalf of a user in a time zone far from where the
         * application is running.
         */
        @Override
        public TimeZone getTimeZone(final ConnectionInstanceData instanceData) {
            return UTC;
        }

        /**
         * Always returns {@link Locale#US} (en_US). The
         * {@link DefaultConnectionAdvisor} returns the default locale.
         *
         * The {@link Locale} is used by the core libraries to sort and compare
         * strings (via {@link Collator}s). An application may wish to set a
         * different {@link Locale} for each connection servicing different
         * users.
         */
        @Override
        public Locale getLocale(final ConnectionInstanceData instanceData) {
            return Locale.US;
        }

        /**
         * Returns a {@link SamplePersistenceStoreProvider} that changes where
         * the cache, configuration, and log files are stored.
         *
         * Applications should generally not override this method so the
         * {@link DefaultPersistenceStoreProvider} is used. If applications
         * return a different {@link PersistenceStoreProvider} important cache
         * and configuration files will not be shared with Visual Studio and the
         * TEE programs.
         */
        @Override
        public PersistenceStoreProvider getPersistenceStoreProvider(final ConnectionInstanceData instanceData) {
            return new SamplePersistenceStoreProvider();
        }

        /**
         * Returns a {@link SampleHTTPClientFactory} that customizes the HTTP
         * user agent string.
         *
         * Applications can return a {@link DefaultHTTPClientFactory} if
         * customization is not desired.
         *
         * @see DefaultHTTPClientFactory
         */
        @Override
        public HTTPClientFactory getHTTPClientFactory(final ConnectionInstanceData instanceData) {
            return new SampleHTTPClientFactory(instanceData, httpProxyURI);
        }

        /**
         * Returns a {@link DefaultServerURIProvider}, which simply gets the
         * server URI from the connection profile.
         *
         * Most applications will not need to provide their own implementation
         * or extend {@link DefaultServerURIProvider}.
         */
        @Override
        public ServerURIProvider getServerURIProvider(final ConnectionInstanceData instanceData) {
            return new DefaultServerURIProvider(instanceData);
        }

        /**
         * Returns a {@link DefaultWebServiceFactory}.
         *
         * Applications that wish to extend federated authentication beyond
         * service credential authentication (handled by
         * {@link DefaultTransportAuthHandler}) can return a
         * {@link DefaultWebServiceFactory} with a different
         * {@link TransportAuthHandler}.
         */
        @Override
        public WebServiceFactory getWebServiceFactory(final ConnectionInstanceData instanceData) {
            /*
             * Send this instance's Locale to the server with requests so
             * messages come back in the correct language.
             *
             * To use DefaultTransportAuthHandler you must give it a
             * ConfigurableHTTPClientFactory (which the implementation's
             * getHTTPClientFactory() will always return).
             */
            return new DefaultWebServiceFactory(
                getLocale(instanceData),
                new DefaultTransportRequestHandler(
                    instanceData,
                    (ConfigurableHTTPClientFactory) getHTTPClientFactory(instanceData)));
        }

        /**
         * Returns a {@link DefaultClientFactory} that creates version control,
         * work item tracking, build, and other high-level client objects.
         *
         * Most applications will not need to provide their own implementation
         * or extend {@link DefaultClientFactory}.
         */
        @Override
        public ClientFactory getClientFactory(final ConnectionInstanceData instanceData) {
            return new DefaultClientFactory();
        }

        /**
         * Returns a {@link DefaultTFProxyServerSettingsFactory}, which checks
         * the profile for Team Foundation Server download proxy settings and
         * uses those if present. If the profile allows a global download proxy
         * to be used, checks for the {@link EnvironmentVariables#TF_PROXY}
         * environment variable and uses that if set.
         *
         * Applications might extend {@link DefaultTFProxyServerSettingsFactory}
         * to return a subclass of {@link DefaultTFProxyServerSettings} that
         * handles connection failures in a different way.
         *
         * @see DefaultTFProxyServerSettingsFactory
         * @see DefaultTFProxyServerSettings
         */
        @Override
        public TFProxyServerSettingsFactory getTFProxyServerSettingsFactory(final ConnectionInstanceData instanceData) {
            return new DefaultTFProxyServerSettingsFactory(instanceData);
        }
    }

    /**
     * Extends {@link DefaultPersistenceStoreProvider} to change where cache,
     * configuration, and log files are stored.
     *
     * Applications should generally not change the location of these files. If
     * they are changed, your application will not be able to share important
     * files like the version control workspace cache with Visual Studio, TEE
     * Plug-in for Eclipse, and TEE Command-line Client.
     *
     * @see PersistenceStoreProvider
     */
    static class SamplePersistenceStoreProvider extends DefaultPersistenceStoreProvider {
        private final FilesystemPersistenceStore cacheStore;
        private final FilesystemPersistenceStore configurationStore;
        private final FilesystemPersistenceStore logStore;

        public SamplePersistenceStoreProvider() {
            super();

            /*
             * Build a {@link PersistenceStore} which maps to a subdirectory
             * inside the user's home directory (more precisely, where the Java
             * system property "user.home" points). See the Javadoc on {@link
             * VersionedVendorFilesystemPersistenceStore} for how the vendor
             * name, application name, and version are mixed into the path on
             * each platform.
             */

            final FilesystemPersistenceStore baseStore =
                new VersionedVendorFilesystemPersistenceStore(
                    "Microsoft-TEE-SDK-Sample-Vendor", //$NON-NLS-1$
                    "Connection Advisor Sample", //$NON-NLS-1$
                    "1.0"); //$NON-NLS-1$

            /*
             * Use custom child store names.
             */
            cacheStore = (FilesystemPersistenceStore) baseStore.getChildStore("SampleCacheFiles"); //$NON-NLS-1$
            configurationStore = (FilesystemPersistenceStore) baseStore.getChildStore("SampleConfigurationFiles"); //$NON-NLS-1$
            logStore = (FilesystemPersistenceStore) baseStore.getChildStore("SampleLogs"); //$NON-NLS-1$
        }

        /**
         * Returns a {@link FilesystemPersistenceStore} for storing cache data.
         */
        @Override
        public FilesystemPersistenceStore getCachePersistenceStore() {
            return cacheStore;
        }

        /**
         * Returns a {@link FilesystemPersistenceStore} for storing
         * configuration data.
         */
        @Override
        public FilesystemPersistenceStore getConfigurationPersistenceStore() {
            return configurationStore;
        }

        /**
         * Returns a {@link FilesystemPersistenceStore} for storing log files.
         */
        @Override
        public FilesystemPersistenceStore getLogPersistenceStore() {
            return logStore;
        }
    }

    /**
     * Extends {@link DefaultHTTPClientFactory} to customize the HTTP user agent
     * string.
     *
     * Applications can override other methods to influence HTTP connection
     * behavior. For example, the TEE cross-platform command line client
     * overrides configureClientGlobalProxy() to set the value of the
     * "http_proxy" environment variable on the connection.
     */
    static class SampleHTTPClientFactory extends DefaultHTTPClientFactory {
        private final URI httpProxyURI;

        public SampleHTTPClientFactory(final ConnectionInstanceData connectionInstanceData, final URI httpProxyURI) {
            super(connectionInstanceData);
            this.httpProxyURI = httpProxyURI;
        }

        @Override
        protected String getUserAgentExtraString(
            final HttpClient httpClient,
            final ConnectionInstanceData connectionInstanceData) {
            // This string is included at the end of the parenthetical part of
            // the user agent header. The string can be at most
            // DefaultHTTPClientFactory.USER_AGENT_EXTRA_TEXT_MAX_CHARS chars.
            return "ConnectionAdvisorSample"; //$NON-NLS-1$
        }

        /**
         * Overrides configureClientProxy method to set the value of the
         * http_proxy on the HTTP Connection.
         */
        @Override
        public void configureClientProxy(
            final HttpClient httpClient,
            final HostConfiguration hostConfiguration,
            final HttpState httpState,
            final ConnectionInstanceData connectionInstanceData) {
            if (httpProxyURI == null) {
                return;
            }

            if (httpProxyURI.getHost() == null) {

                return;
            }

            hostConfiguration.setProxy(
                httpProxyURI.getHost(),
                httpProxyURI.getPort() == -1 ? 80 : httpProxyURI.getPort());
        }
    }

}
