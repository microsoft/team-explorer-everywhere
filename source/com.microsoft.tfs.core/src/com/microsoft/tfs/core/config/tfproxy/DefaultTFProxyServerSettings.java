// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.tfproxy;

import com.microsoft.tfs.core.TFProxyServerSettings;

/**
 * <p>
 * A default implementation of the {@link TFProxyServerSettings} interface.
 * </p>
 * <p>
 * The user can provide a custom post-failure disabled period, which, when
 * positive, causes the proxy to appear disabled ({@link #isAvailable()} returns
 * false) until a subsequent call to {@link #isAvailable()} occurs after the
 * configured period has elapsed.
 * </p>
 * <p>
 * If no disabled period is provided, {@link #DEFAULT_DISABLE_INTERVAL_MILLIS}
 * is used.
 * </p>
 *
 * @see TFProxyServerSettings
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class DefaultTFProxyServerSettings implements TFProxyServerSettings {
    /**
     * The default length of the the post-failure disabled period, in
     * milliseconds.
     */
    public static final long DEFAULT_DISABLE_INTERVAL_MILLIS = 5 * 60 * 1000;

    private final long disableIntervalMillis;

    private final Object lock = new Object();
    private String url;
    private long disableEndTime;

    public DefaultTFProxyServerSettings(final String url) {
        this(url, DEFAULT_DISABLE_INTERVAL_MILLIS);
    }

    /**
     * Creates a {@link DefaultTFProxyServerSettings} for the given URL with a
     * custom post-failure disabled period.
     *
     * @param url
     *        the URL string of the proxy server (pass null to disable the proxy
     *        server)
     * @param disableIntervalMillis
     *        the length of the post-failure disabled period, in milliseconds
     */
    public DefaultTFProxyServerSettings(final String url, final long disableIntervalMillis) {
        setURL(url);
        this.disableIntervalMillis = disableIntervalMillis;
    }

    /**
     * Sets the proxy server URL.
     *
     * @param url
     *        the URL string of the proxy server (pass null to disable the proxy
     *        server)
     */
    public void setURL(final String url) {
        synchronized (lock) {
            this.url = url;
            disableEndTime = -1;
        }
    }

    /**
     * @return the post-failure disabled period length in milliseconds
     */
    public long getDisableInternalMillis() {
        return disableIntervalMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordFailure() {
        synchronized (lock) {
            disableEndTime = System.currentTimeMillis() + disableIntervalMillis;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailable() {
        synchronized (lock) {
            if (url == null) {
                return false;
            }

            return System.currentTimeMillis() > disableEndTime;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getURL() {
        synchronized (lock) {
            return url;
        }
    }
}
