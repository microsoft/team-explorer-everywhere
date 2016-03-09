// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.loaders;

import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoader;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoaderException;

/**
 * <p>
 * Loads no checking policies. Can be passed to methods that require a
 * {@link PolicyLoader} when checkin policy evaluation is not desired.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class NullPolicyLoader implements PolicyLoader {
    /**
     * Creates a {@link NullPolicyLoader}, which never loads any policy
     * implementations, but does not error.
     */
    public NullPolicyLoader() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicyInstance load(final String policyTypeID) throws PolicyLoaderException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAvailablePolicyTypeIDs() throws PolicyLoaderException {
        return new String[0];
    }
}
