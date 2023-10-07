// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.logging;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.LogFactory;

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
         * Look for log4j-teamexplorer.json or log4j-teamexplorer.xml in
         * "~/AppData/Local/Microsoft/Team Foundation/4.0/Configuration"
         */
        mcp.addConfigurationProvider(new FromFileConfigurationProvider(new File[] {
            logConfLocation.getItemFile("log4j-teamexplorer.json"), //$NON-NLS-1$
            logConfLocation.getItemFile("log4j-teamexplorer.xml"), //$NON-NLS-1$
        }));

        /*
         * Load teamexplorer-log4j2.xml with the ClassLoader that loaded the
         * TELoggingConfiguration class
         */
        mcp.addConfigurationProvider(
            new ClassloaderConfigurationProvider(TELoggingConfiguration.class.getClassLoader(), new String[] {
                "teamexplorer-log4j2.xml" //$NON-NLS-1$
        }));

        if (mcp.getConfigurationURL().getFile().endsWith("teamexplorer-log4j2.xml")) { //$NON-NLS-1$
            final FilesystemPersistenceStore logsLocation = TELogUtils.getTeamExplorerLogsLocation();
            try {
                logsLocation.initialize();

                for (String logType : new String[] {"teamexplorer","teamexplorer-soap"}) { //$NON-NLS-1$ //$NON-NLS-2$
                    /*
                     * Prune old log files in the shared directory
                     */
                    cleanup(logType, logsLocation);

                    final File logFile = TELogUtils.createLogFileObject(logType, logsLocation.getStoreFile(), true);

                    /**
                     * Share log file path with configuration as system property
                     */
                    System.setProperty(logType + "-log", logFile.getAbsolutePath()); //$NON-NLS-1$
                }
            } catch (final Exception e) {
                LogFactory.getLog(TELoggingConfiguration.class).error("Log setup error", e); //$NON-NLS-1$
            }
        }

        /*
         * Call into the configuration API in com.microsoft.tfs.logging
         */
        Config.configure(mcp, EnableReconfigurationPolicy.DISABLE_WHEN_EXTERNALLY_CONFIGURED, ResetConfigurationPolicy.RESET_EXISTING);
    }

    private static void cleanup(final String logType, final FilesystemPersistenceStore logsLocation) {
        /*
         * The basic algorithm here is to get an exclusive lock on the settings
         * location. If that exclusive lock can't be had, we return without
         * doing any cleanup (some other instance of the application is
         * currently performing cleanup on this directory).
         */

        com.microsoft.tfs.util.locking.AdvisoryFileLock lock = null;

        try {
            lock = logsLocation.getStoreLock(false);

            /*
             * A null lock means the lock was not immediately available.
             */
            if (lock == null) {
                return;
            }

            /*
             * Here's the call to actually perform the cleanup on the directory.
             * At this point we know we have the exclusive lock.
             */
            doCleanup(logType, logsLocation);
        } catch (final InterruptedException e) {
            /*
             * Shouldn't ever happen because we aren't blocking on getting the
             * lock.
             */
            return;
        } catch (final IOException e) {
            /*
             * Exception trying to get lock - return without doing cleanup
             */
            return;
        } finally {
            try {
                /*
                 * Always release the lock
                 */
                if (lock != null) {
                    lock.release();
                }
            } catch (final IOException e) {
            }
        }
    }

    private static void doCleanup(final String logType, final FilesystemPersistenceStore logsLocation) {
        final File[] logFiles = TELogUtils.getAllLogFilesForLogType(logType, logsLocation.getStoreFile(), true);
        final int CLEANUP_THRESHOLD = 5;

        /*
         * If the number of files is not under the cleanup threshold, this
         * method has nothing to do
         */
        if (logFiles.length < CLEANUP_THRESHOLD) {
            return;
        }

        /*
         * Attempt to delete enough files to bring us below the cleanup
         * threshold. If the deletes don't succeed, that's fine, as another
         * instance may currently have the file open and locked.
         */
        final int numToDelete = logFiles.length - CLEANUP_THRESHOLD + 1;
        int numDeleted = 0;
        for (int i = 0; i < logFiles.length && numDeleted < numToDelete; i++) {
            if (logFiles[i].delete()) {
                numDeleted += 1;
            }
        }
    }
}
