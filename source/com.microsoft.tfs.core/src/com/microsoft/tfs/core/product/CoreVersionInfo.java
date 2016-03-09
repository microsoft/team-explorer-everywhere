// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.product;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import com.microsoft.tfs.core.Messages;

/**
 * Contains build version information about the core packages generated at
 * build-time.
 * <p>
 * This class is for internal use only.
 *
 * @threadsafety thread-safe
 */
public abstract class CoreVersionInfo {
    /**
     * This file used to be called "/version.properties" but IBM ships a
     * resource with that name in an extension with their 1.5 JDK on AIX that
     * always beats us to the classpath, so we use a distinct name.
     *
     * If you change this resource, make sure to update the build script that
     * writes the version info into the file.
     */
    private static final String VERSION_PROPERTIES_RESOURCE = "/com.microsoft.tfs.core-version.properties"; //$NON-NLS-1$

    private static String major = ""; //$NON-NLS-1$
    private static String minor = ""; //$NON-NLS-1$
    private static String service = ""; //$NON-NLS-1$
    private static String build = ""; //$NON-NLS-1$

    private static Throwable loadException;

    static {
        final InputStream in = CoreVersionInfo.class.getResourceAsStream(VERSION_PROPERTIES_RESOURCE);

        if (in != null) {
            try {
                final Properties props = new Properties();
                try {
                    props.load(in);
                    major = props.getProperty("number.version.major"); //$NON-NLS-1$
                    minor = props.getProperty("number.version.minor"); //$NON-NLS-1$
                    service = props.getProperty("number.version.service"); //$NON-NLS-1$
                    build = props.getProperty("number.version.build"); //$NON-NLS-1$
                } catch (final IOException e) {
                    loadException = e;
                }
            } finally {
                try {
                    in.close();
                } catch (final IOException e) {
                    loadException = e;
                }
            }
        } else {
            loadException = new Exception(
                MessageFormat.format(
                    Messages.getString("CoreVersionInfo.UnableToLoadVersionPropertiesResourceFormat"), //$NON-NLS-1$
                    VERSION_PROPERTIES_RESOURCE));
        }
    }

    public static String getMajorVersion() {
        if (loadException != null) {
            throw new RuntimeException(loadException);
        }
        return major;
    }

    public static String getMinorVersion() {
        if (loadException != null) {
            throw new RuntimeException(loadException);
        }
        return minor;
    }

    public static String getServiceVersion() {
        if (loadException != null) {
            throw new RuntimeException(loadException);
        }
        return service;
    }

    public static String getBuildVersion() {
        if (loadException != null) {
            throw new RuntimeException(loadException);
        }
        return build;
    }

    public static String getVersion() {
        return MessageFormat.format(
            "{0}.{1}.{2}.{3}", //$NON-NLS-1$
            getMajorVersion(),
            getMinorVersion(),
            getServiceVersion(),
            getBuildVersion());
    }
}
