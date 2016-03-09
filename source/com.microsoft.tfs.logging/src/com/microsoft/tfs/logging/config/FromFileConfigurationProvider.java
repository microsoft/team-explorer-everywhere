// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.logging.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A logging configuration provider which loads configuration data from a
 * {@link File}.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class FromFileConfigurationProvider implements LoggingConfigurationProvider {
    private final File[] locations;

    /**
     * Constructs a {@link FromFileConfigurationProvider} that tests the given
     * {@link File}s for the first item that exists ( {@link File#exists()} ==
     * <code>true</code>) and returns its {@link URL} as the configuration
     * {@link URL}.
     *
     * @param locations
     *        the locations to search (must not be <code>null</code>)
     */
    public FromFileConfigurationProvider(final File[] locations) {
        if (locations == null) {
            throw new IllegalArgumentException("locations must not be null"); //$NON-NLS-1$
        }

        this.locations = locations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public URL getConfigurationURL() {
        for (int i = 0; i < locations.length; i++) {
            if (locations[i].exists()) {
                try {
                    return locations[i].toURL();
                } catch (final MalformedURLException e) {
                    System.err.println("ERROR: " + e.getMessage()); //$NON-NLS-1$
                    e.printStackTrace();
                }
            } else {
                DebugLogger.verbose("configuration file not found at [" + locations[i].getAbsolutePath() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return null;
    }
}
