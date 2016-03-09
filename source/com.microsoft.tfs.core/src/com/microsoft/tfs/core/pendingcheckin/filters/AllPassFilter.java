// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.filters;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

/**
 * <p>
 * A filter where all submitted changes always pass.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class AllPassFilter implements PendingChangeFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean passes(final PendingChange change) {
        return true;
    }
}
