// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.filters;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

/**
 * Filters a pending change in a pending check-in.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface PendingChangeFilter {
    /**
     * Tests whether the given pending change passes the implementation's filter
     * test.
     *
     * @param change
     *        the change to test (must not be <code>null</code>)
     * @return true if the change passes, false if it does not.
     */
    public boolean passes(PendingChange change);
}
