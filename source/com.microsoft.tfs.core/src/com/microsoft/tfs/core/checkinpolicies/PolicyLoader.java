// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

/**
 * <p>
 * A {@link PolicyLoader} performs all loading and instantiation of
 * {@link PolicyInstance} objects, as well as discover of available
 * {@link PolicyType}s.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface PolicyLoader {
    /**
     * Loads a policy instance that is appropriate for the given policy type ID.
     *
     * @param policyTypeID
     *        the string that identifies the type of policy to load (must not be
     *        <code>null</code> or empty)
     * @return a new policy instance whose type ID matches the given ID, or
     *         <code>null</code> if no matching {@link PolicyInstance}s could be
     *         found.
     * @throws PolicyLoaderException
     *         if an I/O error occurred loading the policy.
     */
    public PolicyInstance load(String policyTypeID) throws PolicyLoaderException;

    /**
     * Returns all the policy type ID strings that could be loaded by this
     * loader. It's possible that an ID returned by this method will fail to
     * load for other reasons when {@link #load(String)} is invoked.
     *
     * @return an array of policy type ID strings that could be loaded by this
     *         loader.
     * @throws PolicyLoaderException
     *         if an I/O error occurred searching for policies.
     */
    public String[] getAvailablePolicyTypeIDs() throws PolicyLoaderException;
}
