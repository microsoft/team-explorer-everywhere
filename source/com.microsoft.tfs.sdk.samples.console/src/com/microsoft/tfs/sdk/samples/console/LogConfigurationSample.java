// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.logging.config.ClassloaderConfigurationProvider;
import com.microsoft.tfs.logging.config.Config;
import com.microsoft.tfs.logging.config.EnableReconfigurationPolicy;
import com.microsoft.tfs.logging.config.ResetConfigurationPolicy;

/**
 * This sample demonstrates how to configure run-time logging behavior of the
 * TFS SDK for Java programmatically, through the {@link Config} class. Read the
 * {@link Config} class's Javadoc for full information on configuring logging,
 * including the simpler method of inserting static configuration resources on
 * the classpath.
 */
public class LogConfigurationSample {
    public static void main(final String[] args) throws InterruptedException {
        /*
         * Use Apache Commons Logging to get a logger for test purposes. All TEE
         * SDK classes obtain loggers this way.
         */

        final Log log = LogFactory.getLog(LogConfigurationSample.class);

        /*
         * Log once using the default TFS SDK for Java log4j configuration,
         * which is usually the log4j.properties resource contained in the TEE
         * SDK JAR (but may be another configuration file found first on the
         * classpath by log4j).
         */

        logAllLevels(log, "default TFS SDK for Java log configuration"); //$NON-NLS-1$

        /*
         * Configure with a resource from this sample project to show TRACE and
         * above (most verbose).
         */

        Config.configure(
            new ClassloaderConfigurationProvider(LogConfigurationSample.class.getClassLoader(), new String[] {
                "com/microsoft/tfs/sdk/samples/console/log4j-trace.properties" //$NON-NLS-1$
        }), EnableReconfigurationPolicy.DISABLE_WHEN_EXTERNALLY_CONFIGURED, ResetConfigurationPolicy.RESET_EXISTING);

        logAllLevels(log, "TRACE and above shown"); //$NON-NLS-1$

        /*
         * Configure with a resource from this sample project to show WARN and
         * above (less verbose).
         */

        Config.configure(
            new ClassloaderConfigurationProvider(LogConfigurationSample.class.getClassLoader(), new String[] {
                "com/microsoft/tfs/sdk/samples/console/log4j-warn.properties" //$NON-NLS-1$
        }), EnableReconfigurationPolicy.DISABLE_WHEN_EXTERNALLY_CONFIGURED, ResetConfigurationPolicy.RESET_EXISTING);

        logAllLevels(log, "WARN and above shown"); //$NON-NLS-1$
    }

    private static void logAllLevels(final Log log, final String message) {
        log.trace(message);
        log.debug(message);
        log.info(message);
        log.warn(message);
        log.error(message);
        log.fatal(message);
    }
}
