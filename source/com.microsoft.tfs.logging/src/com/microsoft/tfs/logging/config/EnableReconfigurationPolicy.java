// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.logging.config;

/**
 * Specifies whether the TEE logging system should accept a new logging
 * configuration.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public abstract class EnableReconfigurationPolicy {
    /**
     * Always allows reconfiguration: {@link #allowReconfiguration()} always
     * returns <code>true</code>, allowing the logging system to accept a new
     * configuration.
     */
    public static final EnableReconfigurationPolicy ALWAYS = new EnableReconfigurationPolicy() {
        @Override
        public boolean allowReconfiguration() {
            return true;
        }

        @Override
        public String toString() {
            return "ALWAYS"; //$NON-NLS-1$
        }
    };

    /**
     * Allows reconfiguration only if a log4j is not externally configured:
     * {@link #allowReconfiguration()} returns <code>true</code> only if the
     * system property <code>log4j.configuration</code> is <b>not</b> set.
     */
    public static final EnableReconfigurationPolicy DISABLE_WHEN_EXTERNALLY_CONFIGURED =
        new EnableReconfigurationPolicy() {
            @Override
            public boolean allowReconfiguration() {
                return System.getProperty("log4j.configuration") == null; //$NON-NLS-1$
            }

            @Override
            public String toString() {
                return "DISABLE_WHEN_EXTERNALLY_CONFIGURED"; //$NON-NLS-1$
            }
        };

    /**
     * @return <code>true</code> if the logging system should be reconfigured,
     *         <code>false</code> if it should not be
     */
    public abstract boolean allowReconfiguration();
}
