// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.tfproxy;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.TFProxyServerSettings;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.config.RegistryUtils;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.jni.RegistryKey;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * <p>
 * A default implementation of {@link TFProxyServerSettingsFactory} which
 * returns a {@link DefaultTFProxyServerSettings} if a TF download proxy is
 * configured through the {@link EnvironmentVariables#TF_PROXY} environment
 * variable or Windows registry setting, or returns <code>null</code> if no
 * proxy is configured.
 * </p>
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class DefaultTFProxyServerSettingsFactory implements TFProxyServerSettingsFactory {
    private static final Log log = LogFactory.getLog(DefaultTFProxyServerSettings.class);

    private static final String REGISTRY_PATH = "TeamFoundation\\SourceControl\\Proxy"; //$NON-NLS-1$
    private static final String PROXY_ENABLED = "Enabled"; //$NON-NLS-1$
    private static final String PROXY_URL = "Url"; //$NON-NLS-1$
    private static final String PROXY_RETRY_INTERVAL = "RetryInterval"; //$NON-NLS-1$

    public DefaultTFProxyServerSettingsFactory(final ConnectionInstanceData connectionInstanceData) {
        Check.notNull(connectionInstanceData, "connectionInstanceData"); //$NON-NLS-1$
    }

    @Override
    public TFProxyServerSettings newProxyServerSettings() {
        TFProxyServerSettings settings = configureFromEnvironmentVariable();

        if (settings == null && Platform.isCurrentPlatform(Platform.WINDOWS)) {
            settings = configureFromRegistry();
        }

        if (settings == null) {
            log.debug("No TF proxy specified"); //$NON-NLS-1$
        }

        return settings;
    }

    @Override
    public void dispose(final TFProxyServerSettings proxyServerSettings) {
    }

    /**
     * Checks the environment for TF proxy server variable and returns a
     * {@link TFProxyServerSettings} if one was configured.
     *
     * @return the {@link TFProxyServerSettings} created from environment
     *         variable settings, or <code>null</code> if none was configured
     */
    protected TFProxyServerSettings configureFromEnvironmentVariable() {
        final String environmentURL =
            PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.TF_PROXY);

        if (environmentURL != null && environmentURL.length() > 0) {
            log.debug(MessageFormat.format(
                "Environment variable {0} set, using TF proxy URL {1}", //$NON-NLS-1$
                EnvironmentVariables.TF_PROXY,
                environmentURL));

            return new DefaultTFProxyServerSettings(environmentURL);
        }

        return null;
    }

    /**
     * Checks the registry for TF proxy server settings and returns a
     * {@link TFProxyServerSettings} if one was configured.
     *
     * @return the {@link TFProxyServerSettings} created from registry settings,
     *         or <code>null</code> if none was configured
     */
    protected TFProxyServerSettings configureFromRegistry() {
        final RegistryKey userKey = RegistryUtils.openOrCreateRootUserRegistryKey();

        if (userKey != null) {
            final RegistryKey registryKey = userKey.getSubkey(REGISTRY_PATH);

            if (registryKey != null) {
                final String proxyURL = registryKey.getStringValue(PROXY_URL, ""); //$NON-NLS-1$

                final boolean enabled = Boolean.parseBoolean(registryKey.getStringValue(PROXY_ENABLED, "false")); //$NON-NLS-1$

                final int proxyRetryIntervalMinutes = registryKey.getIntegerValue(
                    PROXY_RETRY_INTERVAL,
                    (int) DefaultTFProxyServerSettings.DEFAULT_DISABLE_INTERVAL_MILLIS / 60 / 1000);

                // TODO use these in our settings class?

                // final boolean proxyAutoConfigured =
                // registryKey.getBooleanValue(PROXY_AUTO_CONFIGURED, false);

                // proxyLastConfigureTime =
                // registryKey.GetValue(s_proxyLastConfigureTime,
                // default(DateTime).ToString()) as String;

                // proxyLastCheckTime =
                // registryKey.GetValue(s_proxyLastCheckTime,
                // default(DateTime).ToString()) as String;

                if (enabled && proxyURL != null && proxyURL.length() > 0) {
                    log.debug(MessageFormat.format(
                        "Registry key {0} specifies (enabled) proxy URL {1}", //$NON-NLS-1$
                        registryKey.toString(),
                        proxyURL));

                    return new DefaultTFProxyServerSettings(proxyURL, proxyRetryIntervalMinutes * 60 * 1000);
                }
            }
        }

        return null;
    }
}
