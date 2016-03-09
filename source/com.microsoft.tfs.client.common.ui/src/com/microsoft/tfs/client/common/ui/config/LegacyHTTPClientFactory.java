// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.prefs.TFSGlobalProxiesPreferencePage;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.httpclient.DefaultHTTPClientFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HostConfiguration;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;

public class LegacyHTTPClientFactory extends DefaultHTTPClientFactory {
    private static final Log log = LogFactory.getLog(LegacyHTTPClientFactory.class);
    private static final String PREFERENCE_CHANGE_LISTENER_KEY = "HTTP_PROXY_PREFERENCE_CHANGE_LISTENER_KEY"; //$NON-NLS-1$

    public LegacyHTTPClientFactory(final ConnectionInstanceData connectionInstanceData) {
        super(connectionInstanceData);
    }

    @Override
    public void dispose(final HttpClient httpClient) {
        final IPropertyChangeListener preferenceChangeListener =
            (IPropertyChangeListener) httpClient.getParams().getParameter(PREFERENCE_CHANGE_LISTENER_KEY);

        if (preferenceChangeListener != null) {
            if (log.isTraceEnabled()) {
                final String messageFormat = "on dispose of HttpClient [{0}], removing preference change listener"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, httpClient);
                log.trace(message);
            }

            final TFSCommonUIClientPlugin activator = TFSCommonUIClientPlugin.getDefault();
            if (activator != null) {
                final Preferences preferences = activator.getPluginPreferences();
                preferences.removePropertyChangeListener(preferenceChangeListener);
            }
        } else {
            if (log.isTraceEnabled()) {
                final String messageFormat =
                    "on dispose of HttpClient [{0}], preference change listener does not exist"; //$NON-NLS-1$
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
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();

        final IPropertyChangeListener preferenceChangeListener = new PreferenceChangeListener(httpClient);
        preferences.addPropertyChangeListener(preferenceChangeListener);
        httpClient.getParams().setParameter(PREFERENCE_CHANGE_LISTENER_KEY, preferenceChangeListener);

        configureProxy(httpClient);
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

    private void configureProxy(final HttpClient httpClient) {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();

        final HostConfiguration hostConfiguration = httpClient.getHostConfiguration();

        final boolean useHttpProxy = preferences.getBoolean(TFSGlobalProxiesPreferencePage.HTTP_PROXY_ENABLED);

        if (!useHttpProxy) {
            if (log.isDebugEnabled()) {
                if (log.isDebugEnabled()) {
                    final String messageFormat = "HTTP proxy preference off: setting client [{0}] to have no proxy"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, httpClient);
                    log.debug(message);
                }
            }

            hostConfiguration.setProxyHost(null);
            return;
        }

        String httpProxyUrl = preferences.getString(TFSGlobalProxiesPreferencePage.HTTP_PROXY_URL);

        if (httpProxyUrl != null) {
            httpProxyUrl = httpProxyUrl.trim();
            if (httpProxyUrl.length() == 0) {
                httpProxyUrl = null;
            }
        }

        if (httpProxyUrl == null) {
            if (log.isDebugEnabled()) {
                if (log.isDebugEnabled()) {
                    final String messageFormat =
                        "HTTP proxy URL preference empty: setting client [{0}] to have no proxy"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, httpClient);
                    log.debug(message);
                }
            }

            hostConfiguration.setProxyHost(null);
            return;
        }

        URI uri;
        try {
            uri = new URI(httpProxyUrl);
        } catch (final URISyntaxException e) {
            final String messageFormat = "illegal proxy URL: [{0}]: setting client [{1}] to have no proxy"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, httpProxyUrl, httpClient);
            log.warn(message, e);

            hostConfiguration.setProxyHost(null);
            return;
        }

        if (log.isDebugEnabled()) {
            final String messageFormat = "setting client [{0}] to have proxy: host [{1}], port [{2}]"; //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, httpClient, uri.getHost(), Integer.toString(uri.getPort()));
            log.debug(message);
        }

        hostConfiguration.setProxy(uri.getHost(), uri.getPort());

        final HttpState httpState = httpClient.getState();

        String httpProxyUsername = preferences.getString(TFSGlobalProxiesPreferencePage.HTTP_PROXY_USERNAME);
        String httpProxyPassword = preferences.getString(TFSGlobalProxiesPreferencePage.HTTP_PROXY_PASSWORD);

        if (httpProxyUsername != null) {
            httpProxyUsername = httpProxyUsername.trim();
            if (httpProxyUsername.length() == 0) {
                httpProxyUsername = null;
            }
        }

        if (httpProxyPassword != null) {
            httpProxyPassword = httpProxyPassword.trim();
        }

        if (httpProxyUsername == null) {
            if (log.isDebugEnabled()) {
                final String messageFormat = "setting client [{0}] to have no proxy credentials"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, httpClient);
                log.debug(message);
            }

            httpState.clearProxyCredentials();
            return;
        }

        if (log.isDebugEnabled()) {
            final String messageFormat = "setting client [{0}] to have proxy credentials: {1}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, httpClient, httpProxyUsername);
            log.debug(message);
        }

        final Credentials proxyCredentials = new UsernamePasswordCredentials(httpProxyUsername, httpProxyPassword);
        httpState.setProxyCredentials(AuthScope.ANY, proxyCredentials);
    }

    private class PreferenceChangeListener implements IPropertyChangeListener {
        private final HttpClient httpClient;

        public PreferenceChangeListener(final HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            if (event.getProperty().startsWith(TFSGlobalProxiesPreferencePage.HTTP_PROXY_PREFIX)) {
                if (log.isTraceEnabled()) {
                    final String messageFormat = "property change: property=[{0}], old=[{1}], new=[{2}]"; //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        event.getProperty(),
                        event.getOldValue(),
                        event.getNewValue());

                    log.trace(message);
                }

                configureProxy(httpClient);
            }
        }
    }
}
