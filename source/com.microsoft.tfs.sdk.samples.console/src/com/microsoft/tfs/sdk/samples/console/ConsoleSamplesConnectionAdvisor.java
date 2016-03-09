// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import java.net.URI;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
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
import com.microsoft.tfs.core.config.tfproxy.TFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.webservice.DefaultWebServiceFactory;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;
import com.microsoft.tfs.core.httpclient.HostConfiguration;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.core.persistence.VersionedVendorFilesystemPersistenceStore;

public class ConsoleSamplesConnectionAdvisor implements ConnectionAdvisor {
    private static final Log log = LogFactory.getLog(ConsoleSamplesConnectionAdvisor.class);

    private final URI httpProxyURI;

    public ConsoleSamplesConnectionAdvisor() {
        this(null);
    }

    public ConsoleSamplesConnectionAdvisor(final URI httpProxyURI) {

        this.httpProxyURI = httpProxyURI;
    }

    @Override
    public HTTPClientFactory getHTTPClientFactory(final ConnectionInstanceData instanceData) {
        return new SamplesHTTPClientFactory(instanceData);
    }

    private class SamplesHTTPClientFactory extends DefaultHTTPClientFactory {
        public SamplesHTTPClientFactory(final ConnectionInstanceData connectionInstanceData) {
            super(connectionInstanceData);
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
                log.warn("Proxy URL does not contain hostname"); //$NON-NLS-1$
                return;
            }

            log.info("Proxy URL set to " //$NON-NLS-1$
                + httpProxyURI.getHost()
                + ":" //$NON-NLS-1$
                + (httpProxyURI.getPort() == -1 ? 80 : httpProxyURI.getPort()));

            hostConfiguration.setProxy(
                httpProxyURI.getHost(),
                httpProxyURI.getPort() == -1 ? 80 : httpProxyURI.getPort());
        }
    }

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

    @Override
    public ClientFactory getClientFactory(final ConnectionInstanceData arg0) {
        return new DefaultClientFactory();
    }

    @Override
    public Locale getLocale(final ConnectionInstanceData arg0) {
        return null;
    }

    @Override
    public PersistenceStoreProvider getPersistenceStoreProvider(final ConnectionInstanceData arg0) {
        return new SamplePersistenceStoreProvider();
    }

    @Override
    public ServerURIProvider getServerURIProvider(final ConnectionInstanceData instanceData) {
        return new DefaultServerURIProvider(instanceData);
    }

    @Override
    public TFProxyServerSettingsFactory getTFProxyServerSettingsFactory(final ConnectionInstanceData arg0) {
        return null;
    }

    @Override
    public TimeZone getTimeZone(final ConnectionInstanceData arg0) {
        return null;
    }

    @Override
    public WebServiceFactory getWebServiceFactory(final ConnectionInstanceData instanceData) {
        return new DefaultWebServiceFactory(
            getLocale(instanceData),
            new DefaultTransportRequestHandler(
                instanceData,
                (ConfigurableHTTPClientFactory) getHTTPClientFactory(instanceData)));
    }
}