// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.util.Check;

/**
 * Contains the results of an unshelve operation on a workspace: a
 * {@link Shelveset} and a {@link GetStatus}.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public class UnshelveResult {
    private final Shelveset shelveset;
    private final GetStatus status;
    private final PendingChange[] changes;
    private final Conflict[] conflicts;

    /**
     * Creates an {@link UnshelveResult} for the given result items.
     *
     * @param shelveset
     *        the shelveset which was unshelved (must not be <code>null</code>)
     * @param status
     *        the status of the get operations which were processed as part of
     *        the unshelve (must not be <code>null</code>)
     */
    public UnshelveResult(
        final Shelveset shelveset,
        final GetStatus status,
        final PendingChange[] changes,
        final Conflict[] conflicts) {
        Check.notNull(status, "status"); //$NON-NLS-1$
        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$

        this.shelveset = shelveset;
        this.status = status;
        this.changes = changes;
        this.conflicts = conflicts;
    }

    public Shelveset getShelveset() {
        return shelveset;
    }

    public GetStatus getStatus() {
        return status;
    }

    public PendingChange[] changes() {
        return changes;
    }

    public Conflict[] getConflicts() {
        return conflicts;
    }
}
