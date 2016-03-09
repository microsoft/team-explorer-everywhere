// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.config;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.prefs.TFSGlobalProxiesPreferencePage;
import com.microsoft.tfs.core.TFProxyServerSettings;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettings;
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettingsFactory;

/**
 * Provides {@link TFProxyServerSettings} objects configured from the TEE
 * Eclipse plug-in's preference page. If the page is not configured with a proxy
 * this class defers to the super class.
 */
public class EclipseTFProxyServerSettingsFactory extends DefaultTFProxyServerSettingsFactory {
    private static final Log log = LogFactory.getLog(LegacyHTTPClientFactory.class);

    private final Map<DefaultTFProxyServerSettings, IPropertyChangeListener> listeners =
        new HashMap<DefaultTFProxyServerSettings, IPropertyChangeListener>();

    public EclipseTFProxyServerSettingsFactory(final ConnectionInstanceData connectionInstanceData) {
        super(connectionInstanceData);
    }

    @Override
    public void dispose(final TFProxyServerSettings proxyServerSettings) {
        final IPropertyChangeListener preferenceChangeListener = listeners.remove(proxyServerSettings);

        if (preferenceChangeListener != null) {
            if (log.isTraceEnabled()) {
                final String messageFormat =
                    "on dispose of TFProxyServerSettings [{0}], removing preference change listener"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, proxyServerSettings);
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
                    "on dispose of TFProxyServerSettings [{0}], preference change listener does not exist"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, proxyServerSettings);
                log.trace(message);
            }
        }

        super.dispose(proxyServerSettings);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The object returned is reconfigured with new values when Eclipse
     * preferences change.
     */
    @Override
    public TFProxyServerSettings newProxyServerSettings() {
        final DefaultTFProxyServerSettings proxyServerSettings = new DefaultTFProxyServerSettings(null);

        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();

        final IPropertyChangeListener preferenceChangeListener = new PreferenceChangeListener(proxyServerSettings);
        preferences.addPropertyChangeListener(preferenceChangeListener);
        listeners.put(proxyServerSettings, preferenceChangeListener);

        configureProxySettings(proxyServerSettings);

        return proxyServerSettings;
    }

    /**
     * Called by {@link #newProxyServerSettings()} to configure the settings
     * from Eclipse preferences, and called again by the preference change
     * listener to reconfigure when prefs change.
     *
     * @param proxyServerSettings
     *        the settings to change (must not be <code>null</code>)
     */
    private void configureProxySettings(final DefaultTFProxyServerSettings proxyServerSettings) {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();

        final boolean useTfsProxy = preferences.getBoolean(TFSGlobalProxiesPreferencePage.TFS_PROXY_ENABLED);

        if (!useTfsProxy) {
            if (log.isDebugEnabled()) {
                if (log.isDebugEnabled()) {
                    final String messageFormat =
                        "TFS proxy preference off: using superclass configuration for TFProxyServerSettings [{0}]"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, proxyServerSettings);
                    log.debug(message);
                }
            }

            configureFromSuper(proxyServerSettings);
            return;
        }

        String tfsProxyUrl = preferences.getString(TFSGlobalProxiesPreferencePage.TFS_PROXY_URL);

        if (tfsProxyUrl != null) {
            tfsProxyUrl = tfsProxyUrl.trim();
            if (tfsProxyUrl.length() == 0) {
                tfsProxyUrl = null;
            }
        }

        if (tfsProxyUrl == null) {
            if (log.isDebugEnabled()) {
                if (log.isDebugEnabled()) {
                    final String messageFormat =
                        "TFS proxy URL preference empty: using superclass configuration for TFProxyServerSettings [{0}]"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, proxyServerSettings);
                    log.debug(message);
                }
            }

            configureFromSuper(proxyServerSettings);
            return;
        }

        if (log.isDebugEnabled()) {
            final String messageFormat = "setting TFProxyServerSettings [{0}] to have URL: [{1}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, proxyServerSettings, tfsProxyUrl);
            log.debug(message);
        }

        proxyServerSettings.setURL(tfsProxyUrl);
    }

    private void configureFromSuper(final DefaultTFProxyServerSettings settings) {
        final TFProxyServerSettings superSettings = super.newProxyServerSettings();

        if (superSettings != null && superSettings.getURL() != null) {
            settings.setURL(superSettings.getURL());
        } else {
            settings.setURL(null);
        }
    }

    /**
     * Listens for pref changes in Eclipse and reconfigures the long-lived
     * {@link DefaultTFProxyServerSettings} object.
     */
    private class PreferenceChangeListener implements IPropertyChangeListener {
        private final DefaultTFProxyServerSettings proxyServerSettings;

        public PreferenceChangeListener(final DefaultTFProxyServerSettings proxyServerSettings) {
            this.proxyServerSettings = proxyServerSettings;
        }

        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            if (event.getProperty().startsWith(TFSGlobalProxiesPreferencePage.TFS_PROXY_PREFIX)) {
                if (log.isTraceEnabled()) {
                    final String messageFormat = "property change: property=[{0}], old=[{1}], new=[{2}]"; //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        event.getProperty(),
                        event.getOldValue(),
                        event.getNewValue());

                    log.trace(message);
                }

                configureProxySettings(proxyServerSettings);
            }
        }
    }
}
