// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.logging.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A composite logging configuration provider which iterates over its contained
 * providers until it finds one that can provide a non-<code>null</code>
 * configuration URL and returns that one.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class MultiConfigurationProvider implements LoggingConfigurationProvider {
    private final List<LoggingConfigurationProvider> configurationProviders =
        new ArrayList<LoggingConfigurationProvider>();

    /**
     * Adds a provider to this {@link MultiConfigurationProvider}.
     *
     * @param configurationProvider
     *        the configuration provider to add (must not be <code>null</code>)
     */
    public synchronized void addConfigurationProvider(final LoggingConfigurationProvider configurationProvider) {
        if (configurationProvider == null) {
            throw new IllegalArgumentException("configurationProvider must not be null"); //$NON-NLS-1$
        }

        configurationProviders.add(configurationProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized URL getConfigurationURL() {
        URL url = null;

        for (final Iterator<LoggingConfigurationProvider> it = configurationProviders.iterator(); it.hasNext();) {
            final LoggingConfigurationProvider configurationProvider = it.next();
            url = configurationProvider.getConfigurationURL();
            if (url != null) {
                break;
            }
        }

        return url;
    }
}
