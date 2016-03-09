// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.logging;

import java.io.File;

import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.logging.config.ClassloaderConfigurationProvider;
import com.microsoft.tfs.logging.config.Config;
import com.microsoft.tfs.logging.config.EnableReconfigurationPolicy;
import com.microsoft.tfs.logging.config.FromFileConfigurationProvider;
import com.microsoft.tfs.logging.config.MultiConfigurationProvider;
import com.microsoft.tfs.logging.config.ResetConfigurationPolicy;

/**
 * This class implements Team Exporer-specific logging configuration.
 *
 * Logging is provided through the com.microsoft.tfs.logging plugin, which
 * itself is not Team Explorer-specific. The purpose of this class is to call
 * into com.microsoft.tfs.logging and pass it Team Explorer-specific logging
 * information (names to use for files, locations for files, etc.).
 */
public class TELoggingConfiguration {
    private static boolean configured = false;

    public synchronized static void configure() {
        if (configured) {
            return;
        }

        configured = true;

        /*
         * Find the Configuration directory under the default config location.
         */
        final FilesystemPersistenceStore logConfLocation =
            DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore();

        /*
         * The MultiConfigurationProvider holds multiple configuration methods
         * for the logging system. Each method is tried in sequence until one
         * successfully produces a logging configuration file.
         *
         * This allows us to look for a logging configuration file in the file
         * system, and then fall back to a built-in configuration if no custom
         * file is present.
         */
        final MultiConfigurationProvider mcp = new MultiConfigurationProvider();

        /*
         * Look for log4j-teamexplorer.properties and log4j-teamexplorer.xml in
         * the "common" directory.
         */
        mcp.addConfigurationProvider(new FromFileConfigurationProvider(new File[] {
            logConfLocation.getItemFile("log4j-teamexplorer.properties"), //$NON-NLS-1$
            logConfLocation.getItemFile("log4j-teamexplorer.xml"), //$NON-NLS-1$
        }));

        /*
         * Load log4j-teamexplorer.properties from the classloader that loaded
         * TELoggingConfiguration (the classloader for Core)
         */
        mcp.addConfigurationProvider(
            new ClassloaderConfigurationProvider(TELoggingConfiguration.class.getClassLoader(), new String[] {
                "log4j-teamexplorer.properties" //$NON-NLS-1$
        }));

        /*
         * Safe off the current thread context classloader since we need to
         * re-set it temporarily
         */
        final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            /*
             * Setting the thread context classloader to the class loader who
             * loaded TELoggingConfiguration allows Log4J to load custom
             * Appenders from this class loader (eg TEAppender)
             */
            Thread.currentThread().setContextClassLoader(TELoggingConfiguration.class.getClassLoader());

            /*
             * Call into the configuration API in com.microsoft.tfs.logging
             */
            Config.configure(
                mcp,
                EnableReconfigurationPolicy.DISABLE_WHEN_EXTERNALLY_CONFIGURED,
                ResetConfigurationPolicy.RESET_EXISTING);
        } finally {
            /*
             * Always reset the thread context classloader
             */
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
    }
}
