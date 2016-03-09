// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.logging.config;

import java.net.URL;
import java.text.MessageFormat;

import org.apache.log4j.LogManager;
import org.apache.log4j.helpers.OptionConverter;

/**
 * <h1>TFS SDK for Java Logging Configuration</h1>
 * <p>
 * The TFS SDK for Java uses Apache Commons Logging as the high level logging
 * interface throughout its implementation. Apache Log4j is the default logging
 * implementation, but is not called directly to log messages (Commons is always
 * used instead). Both of these layers can be configured by SDK users.
 * </p>
 * <p>
 * Setting the log4j logging level to <code>DEBUG</code> or <CODE>TRACE</code>
 * is one of the best ways to diagnose problems with the TFS SDK for Java. For
 * example, at the <code>TRACE</code> level every byte of HTTP network traffic
 * can be logged.
 * </p>
 * <h2>Apache Commons Logging Defaults</h2>
 * <p>
 * Apache Commons Logging dynamically detects available logging implementations,
 * including log4j, and there is little configuration required for it. The TEE
 * SDK ships with a <code>/commons-logging.properties</code> resource that sets
 * only one setting, <code>use_tccl</code> to <code>false</code>, in order to
 * work better with Eclipse's custom plug-in {@link ClassLoader}s.
 * </p>
 * <p>
 * The simplest way to use your own Commons Logging configuration file is to
 * configure your application's classpath so your custom
 * <code>commons-logging.properties</code> resource is found before the TEE
 * SDK's resource. See Apache Commons Logging documentation for more information
 * on static configuration.
 * </p>
 * <p>
 * It is recommended to leave the Commons Logging system in its default state.
 * Almost all typical logging customization can be done at the log4j level.
 * </p>
 *
 * <h2>log4j Defaults</h2>
 * <p>
 * The TFS SDK for Java ships with a <code>/log4j.properties</code> resource
 * that configures the root log level to <code>WARN</code> and sends all
 * messages to the console. This configuration is not appropriate for all
 * applications, and can be customized in multiple ways.
 * </p>
 * <p>
 * The simplest way to use your own log4j configuration file is to configure
 * your application's classpath so your custom <code>log4j.xml</code> or
 * <code>log4j.properties</code> resource is found before the TFS SDK for Java's
 * resource. See log4j documentation for more information on static
 * configuration.
 * </p>
 *
 * <h2>Dynamic log4j Configuration</h2>
 * <p>
 * Instead of providing <code>log4j.xml</code> or <code>log4j.properties</code>
 * resources ahead of the TFS SDK for Java in the classpath, you can call
 * methods in this class to load log4j configuration from other places. This
 * avoids classpath ordering problems, can load configuration files from
 * non-classpath resources, and can be used to reset previous configuration
 * values. It can also be used to change the logging configuration multiple
 * times throughout a program's life.
 * </p>
 * <p>
 * <b>Important:</b> If you do not configure log4j in your application by
 * calling log4j methods or by calling
 * {@link #configure(LoggingConfigurationProvider, EnableReconfigurationPolicy, ResetConfigurationPolicy)}
 * , log4j will configure itself using its default process (through which the
 * TFS SDK for Java's <code>/log4j.properties</code> resource might get loaded).
 * You can use the methods in this class and the types in this package to
 * provide configuration files to log4j.
 * </p>
 *
 * <h3>Troubleshooting Logging Configuration</h3>
 * <p>
 * If you're having trouble getting log4j configured, set the
 * <code>log4j.debug</code> system property to <code>true</code>. Setting this
 * property causes log4j to print information about its configuration process to
 * standard output and error streams. TFS SDK for Java logging classes also
 * print extra information when this property is set, through
 * {@link DebugLogger}.
 * </p>
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public abstract class Config {
    /**
     * A flexible interface for configuring log4j. Most TFS SDK for Java client
     * applications will call this before calling other SDK methods to ensure
     * logging at the desired level goes to desired locations.
     *
     *
     * @param provider
     *        provides a {@link URL} to a log4j configuration file (must not be
     *        <code>null</code>)
     * @param enableReconfigurationPolicy
     *        a policy that determines whether this method does any
     *        configuration: if
     *        {@link EnableReconfigurationPolicy#allowReconfiguration()} returns
     *        <code>false</code> this method does no configuration regardless of
     *        the current state of log4j
     * @param resetConfigurationPolicy
     *        a policy that determines whether the existing log configuration is
     *        discarded before the new configuration (from provider) is applied
     *
     * @see ClassloaderConfigurationProvider
     * @see FromFileConfigurationProvider
     * @see MultiConfigurationProvider
     *
     * @see EnableReconfigurationPolicy#ALWAYS
     * @see EnableReconfigurationPolicy#DISABLE_WHEN_EXTERNALLY_CONFIGURED
     */
    public static void configure(
        final LoggingConfigurationProvider provider,
        final EnableReconfigurationPolicy enableReconfigurationPolicy,
        final ResetConfigurationPolicy resetConfigurationPolicy) {
        if (provider == null) {
            throw new IllegalArgumentException("provider must not be null"); //$NON-NLS-1$
        }
        if (enableReconfigurationPolicy == null) {
            throw new IllegalArgumentException("enableReconfigurationPolicy must not be null"); //$NON-NLS-1$
        }
        if (resetConfigurationPolicy == null) {
            throw new IllegalArgumentException("resetConfigurationPolicy must not be null"); //$NON-NLS-1$
        }

        if (!enableReconfigurationPolicy.allowReconfiguration()) {
            DebugLogger.verbose(MessageFormat.format(
                "skipping logging reconfiguration due to policy [{0}]", //$NON-NLS-1$
                enableReconfigurationPolicy));
            return;
        }

        final URL configurationURL = provider.getConfigurationURL();

        if (configurationURL == null) {
            DebugLogger.error(
                MessageFormat.format(
                    "logging configuration URL provided by [{0}] is null, not configuring", //$NON-NLS-1$
                    provider.getClass().getName()));
            return;
        }

        DebugLogger.verbose(MessageFormat.format(
            "logging: reconfiguring logging with URL [{0}] provided by [{1}]", //$NON-NLS-1$
            configurationURL,
            provider.getClass().getName()));

        /*
         * Not sure why we call this method but ignore the result, but I'm
         * afraid to change it. Maybe we need the side-effects of calling
         * LogManager.getLoggerRepository()?
         */
        LogManager.getLoggerRepository();

        if (resetConfigurationPolicy.resetConfiguration()) {
            DebugLogger.verbose(
                MessageFormat.format(
                    "logging: resetting existing logging configuration due to policy [{0}]", //$NON-NLS-1$
                    resetConfigurationPolicy));
            LogManager.resetConfiguration();
        }

        OptionConverter.selectAndConfigure(configurationURL, null, LogManager.getLoggerRepository());
    }
}
