// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.logging.config;

/**
 * A very simple emergency logger for communicating problems encountered while
 * configuring log4j through {@link Config}. Enabled only when either the
 * <code>log4j.debug</code> or <code>com.microsoft.tfs.logging.debug</code>
 * system properties are set to <code>true</code>. Prints to {@link System#out}
 * and {@link System#err}.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class DebugLogger {
    private static boolean VERBOSE_ENABLED = Boolean.getBoolean("log4j.debug") //$NON-NLS-1$
        || Boolean.getBoolean("com.microsoft.tfs.logging.debug"); //$NON-NLS-1$

    public static void verbose(final String x) {
        if (VERBOSE_ENABLED) {
            System.out.println("DebugLogger: " + x); //$NON-NLS-1$
        }
    }

    public static void error(final String x) {
        System.err.println("ERROR (DebugLogger): " + x); //$NON-NLS-1$
    }
}
