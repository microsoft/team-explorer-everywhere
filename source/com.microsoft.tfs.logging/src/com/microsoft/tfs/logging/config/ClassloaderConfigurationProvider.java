// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.logging.config;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * A logging configuration provider which finds configuration resources through
 * a designated classloader.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class ClassloaderConfigurationProvider implements LoggingConfigurationProvider {
    private final ClassLoader classLoader;
    private final String[] resourceNames;

    /**
     * Constructs a {@link ClassloaderConfigurationProvider} that uses the given
     * {@link ClassLoader} to resolve the given resource names into
     * configuration {@link URL}s.
     *
     * @param classLoader
     *        the {@link ClassLoader} to use (must not be <code>null</code>)
     * @param resourceNames
     *        the resource names to search for in the desired search order (must
     *        not be <code>null</code>)
     */
    public ClassloaderConfigurationProvider(final ClassLoader classLoader, final String[] resourceNames) {
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader must not be null"); //$NON-NLS-1$
        }
        if (resourceNames == null) {
            throw new IllegalArgumentException("resourceNames must not be null"); //$NON-NLS-1$
        }
        this.classLoader = classLoader;
        this.resourceNames = resourceNames;
    }

    /**
     * {@inheritDoc}
     *
     * {@link ClassloaderConfigurationProvider} searches its configured
     * {@link ClassLoader} for each configured resource name, returning the URL
     * for the first resource name found. As the {@link ClassLoader} is queried
     * for each resource name with {@link ClassLoader#getResources(String)}, it
     * may return multiple matches in an {@link Enumeration}. The last element
     * of the enumeration is returned, which gives child {@link ClassLoader}
     * resources priority over parent or delegating {@link ClassLoader}s.
     */
    @Override
    public URL getConfigurationURL() {
        URL url = null;

        for (int i = 0; i < resourceNames.length; i++) {
            /*
             * Implementation note: Calling getResources() and using the last
             * element in the Enumeration allows child classloader resources to
             * be given priority over parent or delegating classloader
             * resources. At least the standard implementation of Classloader
             * returns the Enumeration sorted such that parent classloader
             * resources come first.
             *
             * To be absolutely certain that the proper resource is being
             * loaded, ensure that there are no resource name collisions
             * anywhere in the classloader hierarchy.
             */

            Enumeration<URL> availableResources = null;
            try {
                availableResources = classLoader.getResources(resourceNames[i]);
            } catch (final IOException ex) {
                System.err.println("ERROR: " + ex.getMessage()); //$NON-NLS-1$
                ex.printStackTrace();
            }
            if (availableResources != null) {
                while (availableResources.hasMoreElements()) {
                    url = availableResources.nextElement();
                }
            }
            if (url != null) {
                break;
            }
        }

        return url;
    }
}
