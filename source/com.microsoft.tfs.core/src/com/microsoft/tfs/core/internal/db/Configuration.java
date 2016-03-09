// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class that manages a configuration and looks up configuration data.
 *
 * Every piece of configuration data is treated as a String (by this class), and
 * has a key. Each piece of configuration data must also have a default value.
 *
 * The configuration is loaded from a properties file. If this file isn't
 * present or a value is missing, the default value is used instead. Any of the
 * configuration data can be overridden by setting a Java system property whose
 * key is the configuration property key.
 */
public class Configuration {
    private static final Log log = LogFactory.getLog(Configuration.class);

    private final Properties props = new Properties();

    public Configuration(final Class cls, final String resourceName) {
        InputStream input = null;
        try {
            input = cls.getResourceAsStream(resourceName);
            if (input != null) {
                props.load(input);
            } else {
                log.warn(MessageFormat.format(
                    "configuration [{0}] from class [{1}] does not exist", //$NON-NLS-1$
                    resourceName,
                    cls.getName()));
            }
        } catch (final IOException ex) {
            log.warn(
                MessageFormat.format("error loading configuration [{0}] from class [{1}]", resourceName, cls.getName()), //$NON-NLS-1$
                ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (final IOException e) {
                    log.warn(MessageFormat.format(
                        "error closing configuration [{0}] from class [{1}]", //$NON-NLS-1$
                        resourceName,
                        cls.getName()), e);
                }
            }
        }
    }

    public Configuration(final File inputFile) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(inputFile);
            props.load(input);
        } catch (final IOException ex) {
            log.warn(MessageFormat.format("error loading configuration [{0}]", inputFile.getAbsolutePath()), ex); //$NON-NLS-1$
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (final IOException e) {
                    log.warn(MessageFormat.format("error closing configuration [{0}]", inputFile.getAbsolutePath()), e); //$NON-NLS-1$
                }
            }
        }
    }

    public String getConfiguration(final String key, final String defaultValue) {
        final String sysProp = System.getProperty(key);
        if (sysProp != null) {
            return sysProp;
        }

        if (props.containsKey(key)) {
            return props.getProperty(key);
        }

        return defaultValue;
    }
}
