// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.config;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Preferences;
import org.osgi.util.tracker.ServiceTracker;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.httpclient.DefaultHTTPClientFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HostConfiguration;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;
import com.microsoft.tfs.util.Check;

public class ProxyServiceHTTPClientFactory extends DefaultHTTPClientFactory {
    private static final Log log = LogFactory.getLog(ProxyServiceHTTPClientFactory.class);

    private static final String PROXY_CHANGE_LISTENER_KEY = "PROXY_CHANGE_LISTENER_KEY"; //$NON-NLS-1$

    private final ServiceTracker serviceTracker;

    public ProxyServiceHTTPClientFactory(
        final ConnectionInstanceData connectionInstanceData,
        final ServiceTracker serviceTracker) {
        super(connectionInstanceData);

        Check.notNull(serviceTracker, "serviceTracker"); //$NON-NLS-1$
        this.serviceTracker = serviceTracker;
    }

    @Override
    public void dispose(final HttpClient httpClient) {
        final IProxyChangeListener proxyChangeListener =
            (IProxyChangeListener) httpClient.getParams().getParameter(PROXY_CHANGE_LISTENER_KEY);
        if (proxyChangeListener != null) {
            if (log.isTraceEnabled()) {
                final String messageFormat = "on dispose of HttpClient [{0}], removing proxy change listener"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, httpClient);
                log.trace(message);
            }

            final IProxyService proxyService = (IProxyService) serviceTracker.getService();
            if (proxyService != null) {
                proxyService.removeProxyChangeListener(proxyChangeListener);
            }
        } else {
            if (log.isTraceEnabled()) {
                final String messageFormat = "on dispose of HttpClient [{0}], proxy change listener does not exist"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, httpClient);
                log.trace(message);
            }
        }

        super.dispose(httpClient);
    }

    @Override
    public void configureClientProxy(
        final HttpClient httpClient,
        final HostConfiguration hostConfiguration,
        final HttpState httpState,
        final ConnectionInstanceData connectionInstanceData) {
        final IProxyService proxyService = (IProxyService) serviceTracker.getService();

        if (proxyService == null) {
            return;
        }

        final String host = connectionInstanceData.getServerURI().getHost();
        final String type = connectionInstanceData.getServerURI().getScheme().equals("http") //$NON-NLS-1$
            ? IProxyData.HTTP_PROXY_TYPE : IProxyData.HTTPS_PROXY_TYPE;

        final IProxyChangeListener changeListener = new ProxyChangeListener(httpClient, host, type);

        proxyService.addProxyChangeListener(changeListener);

        httpClient.getParams().setParameter(PROXY_CHANGE_LISTENER_KEY, changeListener);

        configureProxy(httpClient, host, type);
    }

    @Override
    protected boolean shouldAcceptUntrustedCertificates(final ConnectionInstanceData connectionInstanceData) {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();

        if (preferences.getBoolean(UIPreferenceConstants.ACCEPT_UNTRUSTED_CERTIFICATES)) {
            return true;
        }

        // Let the base class test for environment variables, sysprops, etc.
        return super.shouldAcceptUntrustedCertificates(connectionInstanceData);
    }

    private void configureProxy(final HttpClient httpClient, final String host, final String type) {
        final IProxyService proxyService = (IProxyService) serviceTracker.getService();
        final HostConfiguration hostConfiguration = httpClient.getHostConfiguration();

        if (proxyService == null) {
            if (log.isDebugEnabled()) {
                if (log.isDebugEnabled()) {
                    final String messageFormat =
                        "IProxyService not available: setting client [{0}] (host [{1}] type [{2}]) to have no proxy"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, httpClient, host, type);
                    log.debug(message);
                }
            }

            hostConfiguration.setProxyHost(null);
            return;
        }

        if (!proxyService.isProxiesEnabled()) {
            if (log.isDebugEnabled()) {
                final String messageFormat =
                    "IProxyService.isProxiesEnabled() == false: setting client [{0}] (host [{1}] type [{2}]) to have no proxy"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, httpClient, host, type);
                log.debug(message);
            }

            hostConfiguration.setProxyHost(null);
            return;
        }

        final IProxyData proxyData = proxyService.getProxyDataForHost(host, type);

        if (proxyData == null || proxyData.getHost() == null) {
            if (log.isDebugEnabled()) {
                if (log.isDebugEnabled()) {
                    final String messageFormat =
                        "IProxyService.getProxyDataForHost() == null or empty: setting client [{0}] (host [{1}] type [{2}]) to have no proxy"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, httpClient, host, type);
                    log.debug(message);
                }
            }

            hostConfiguration.setProxyHost(null);
            return;
        }

        if (log.isDebugEnabled()) {
            final String messageFormat =
                "setting client [{0}] (host [{1}] type [{2}]) to have proxy: host [{3}], port [{4}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                httpClient,
                host,
                type,
                proxyData.getHost(),
                Integer.toString(proxyData.getPort()));

            log.debug(message);
        }

        hostConfiguration.setProxy(proxyData.getHost(), proxyData.getPort());

        final HttpState httpState = httpClient.getState();
        if (proxyData.getUserId() != null) {
            if (log.isDebugEnabled()) {
                final String messageFormat =
                    "setting client [{0}] (host [{1}] type [{2}]) to have proxy credentials: {3}"; //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, httpClient, host, type, proxyData.getUserId());

                log.debug(message);
            }

            final Credentials proxyCredentials =
                new UsernamePasswordCredentials(proxyData.getUserId(), proxyData.getPassword());
            httpState.setProxyCredentials(AuthScope.ANY, proxyCredentials);
        } else {
            final String messageFormat = "setting client [{0}] (host [{1}] type [{2}]) to have no proxy credentials"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, httpClient, host, type);
            if (log.isDebugEnabled()) {
                log.debug(message);
            }

            httpState.clearProxyCredentials();
        }
    }

    private class ProxyChangeListener implements IProxyChangeListener {
        private final HttpClient httpClient;
        private final String host;
        private final String type;

        public ProxyChangeListener(final HttpClient httpClient, final String host, final String type) {
            this.httpClient = httpClient;
            this.host = host;
            this.type = type;
        }

        @Override
        public void proxyInfoChanged(final IProxyChangeEvent event) {
            if (log.isTraceEnabled()) {
                final String messageFormat = "proxyInfoChanged: {0}"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, Integer.toString(event.getChangeType()));
                log.trace(message);
            }

            configureProxy(httpClient, host, type);
        }
    }
}
