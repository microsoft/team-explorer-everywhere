// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.tfproxy;

import com.microsoft.tfs.core.TFProxyServerSettings;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.config.ConnectionAdvisor;

/**
 * <p>
 * An {@link TFProxyServerSettingsFactory} is used by a
 * {@link TFSTeamProjectCollection} to obtain TF proxy server settings. An
 * {@link TFProxyServerSettingsFactory} is supplied to a
 * {@link TFSTeamProjectCollection} by a {@link ConnectionAdvisor}.
 * </p>
 *
 * <p>
 * {@link TFSTeamProjectCollection} allows only a single thread to use a
 * {@link TFProxyServerSettingsFactory} at a time.
 * </p>
 *
 * <p>
 * For a default implementation, see {@link DefaultTFProxyServerSettings}.
 * </p>
 *
 * @see TFSTeamProjectCollection
 * @see ConnectionAdvisor
 * @see DefaultTFProxyServerSettings
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface TFProxyServerSettingsFactory {
    /**
     * Called to obtain TF proxy server settings.
     *
     * @return a {@link TFProxyServerSettings} instance or <code>null</code> if
     *         there are no TF proxy server settings
     */
    public TFProxyServerSettings newProxyServerSettings();

    /**
     * <p>
     * Called to dispose a {@link TFProxyServerSettings} that was previously
     * obtained from a call to {@link #newProxyServerSettings()}.
     * </p>
     *
     * <p>
     * This method is called by {@link TFSTeamProjectCollection#close()} to
     * clean up any resources. It can be assumed that the specified
     * {@link TFProxyServerSettings} instance will no longer be used after
     * calling this method.
     * </p>
     *
     * @param proxyServerSettings
     *        an {@link TFProxyServerSettings} instance to dispose (must not be
     *        <code>null</code>)
     */
    public void dispose(TFProxyServerSettings proxyServerSettings);
}
