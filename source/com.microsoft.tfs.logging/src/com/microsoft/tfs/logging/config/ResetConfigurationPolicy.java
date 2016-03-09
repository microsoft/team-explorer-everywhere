// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.logging.config;

/**
 * Specifies whether the TEE logging system should reset the existing logging
 * configuration or leave it intact when processing new configuration data.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public abstract class ResetConfigurationPolicy {
    /**
     * Always reset the existing configuration: {@link #resetConfiguration()}
     * always returns <code>true</code>.
     */
    public static final ResetConfigurationPolicy RESET_EXISTING = new ResetConfigurationPolicy() {
        @Override
        public boolean resetConfiguration() {
            return true;
        }

        @Override
        public String toString() {
            return "RESET_EXISTING"; //$NON-NLS-1$
        }
    };

    /**
     * Does not reset the existing configuration: {@link #resetConfiguration()}
     * always returns <code>false</code>.
     */
    public static final ResetConfigurationPolicy LEAVE_EXISTING = new ResetConfigurationPolicy() {
        @Override
        public boolean resetConfiguration() {
            return false;
        }

        @Override
        public String toString() {
            return "LEAVE_EXISTING"; //$NON-NLS-1$
        }
    };

    /**
     * @return <code>true</code> if the existing logging configuration should be
     *         discarded, <code>false</code> if the existing configuration
     *         should be preserved and the new configuration information added
     *         to it
     */
    public abstract boolean resetConfiguration();
}
