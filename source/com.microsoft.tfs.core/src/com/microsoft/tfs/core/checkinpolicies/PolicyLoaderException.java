// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Thrown when a {@link PolicyLoader} encounters a problem.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class PolicyLoaderException extends VersionControlException {
    private final PolicyType policyType;

    /**
     * Creates a {@link PolicyLoaderException} with detailed information and the
     * {@link PolicyType} that could not be loaded.
     *
     * @param message
     *        the message
     * @param cause
     *        the cause
     * @param policyType
     *        the policy type that could not be loaded (must not be
     *        <code>null</code>)
     */
    public PolicyLoaderException(final String message, final Throwable cause, final PolicyType policyType) {
        super(message, cause);

        Check.notNull(policyType, "policyType"); //$NON-NLS-1$
        this.policyType = policyType;
    }

    /**
     * Creates a {@link PolicyLoaderException} with detailed information and the
     * {@link PolicyType} that could not be loaded.
     *
     * @param message
     *        the message
     * @param policyType
     *        the policy type that could not be loaded (must not be
     *        <code>null</code>)
     */
    public PolicyLoaderException(final String message, final PolicyType policyType) {
        super(message);

        Check.notNull(policyType, "policyType"); //$NON-NLS-1$
        this.policyType = policyType;
    }

    /**
     * Creates a {@link PolicyLoaderException} with detailed information and the
     * {@link PolicyType} that could not be loaded.
     *
     * @param cause
     *        the cause
     * @param policyType
     *        the policy type that could not be loaded (must not be
     *        <code>null</code>)
     */
    public PolicyLoaderException(final Throwable cause, final PolicyType policyType) {
        super(cause);

        Check.notNull(policyType, "policyType"); //$NON-NLS-1$
        this.policyType = policyType;
    }

    /**
     * @return the {@link PolicyType} that failed to load. May be null if the
     *         error wasn't caused by a specific policy.
     */
    public PolicyType getPolicyType() {
        return policyType;
    }
}
