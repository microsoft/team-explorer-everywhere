// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config;

import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.TFSConnection;
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
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.tfproxy.TFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.webservice.DefaultWebServiceFactory;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;
import com.microsoft.tfs.core.ws.runtime.client.TransportRequestHandler;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * The default {@link ConnectionAdvisor} implementation.
 * </p>
 * <p>
 * Non-trivial client applications will almost certainly want to extend this
 * class, if only to override
 * {@link #getBasePersistenceStore(ConnectionInstanceData)} so cache data goes
 * into a custom location (perhaps named for the vendor).
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class DefaultConnectionAdvisor implements ConnectionAdvisor {
    private final Locale locale;
    private final TimeZone timeZone;

    /**
     * Creates a {@link DefaultConnectionAdvisor} that will return the specified
     * {@link Locale} and {@link TimeZone} for all
     * {@link ConnectionInstanceData}s.
     *
     * @param locale
     *        the locale (must not be <code>null</code>)
     * @param timeZone
     *        the time zone (must not be <code>null</code>)
     */
    public DefaultConnectionAdvisor(final Locale locale, final TimeZone timeZone) {
        Check.notNull(locale, "locale"); //$NON-NLS-1$
        Check.notNull(timeZone, "timeZone"); //$NON-NLS-1$

        this.locale = locale;
        this.timeZone = timeZone;
    }

    @Override
    public Locale getLocale(final ConnectionInstanceData instanceData) {
        return locale;
    }

    @Override
    public TimeZone getTimeZone(final ConnectionInstanceData instanceData) {
        return timeZone;
    }

    @Override
    public PersistenceStoreProvider getPersistenceStoreProvider(final ConnectionInstanceData instanceData) {
        return DefaultPersistenceStoreProvider.INSTANCE;
    }

    @Override
    public ClientFactory getClientFactory(final ConnectionInstanceData instanceData) {
        return new DefaultClientFactory();
    }

    @Override
    public HTTPClientFactory getHTTPClientFactory(final ConnectionInstanceData instanceData) {
        return new DefaultHTTPClientFactory(instanceData);
    }

    @Override
    public ServerURIProvider getServerURIProvider(final ConnectionInstanceData instanceData) {
        return new DefaultServerURIProvider(instanceData);
    }

    @Override
    public TFProxyServerSettingsFactory getTFProxyServerSettingsFactory(final ConnectionInstanceData instanceData) {
        return new DefaultTFProxyServerSettingsFactory(instanceData);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Returns a {@link DefaultWebServiceFactory} that uses the {@link Locale}
     * returned by {@link #getLocale(ConnectionInstanceData)} and a
     * {@link DefaultTransportRequestHandler}, which can only fetch OAuth JWT
     * credentials for the {@link TFSConnection} in use. Derived classes that
     * want different federated authentication behavior, but keep the other
     * {@link DefaultWebServiceFactory} behavior, can return a
     * {@link DefaultWebServiceFactory} created with a different
     * {@link TransportRequestHandler}.
     * </p>
     */
    @Override
    public WebServiceFactory getWebServiceFactory(final ConnectionInstanceData instanceData) {
        /*
         * Send this instance's Locale to the server with requests so messages
         * come back in the correct language.
         */
        return new DefaultWebServiceFactory(
            getLocale(instanceData),
            new DefaultTransportRequestHandler(
                instanceData,
                (ConfigurableHTTPClientFactory) getHTTPClientFactory(instanceData)));
    }
}
