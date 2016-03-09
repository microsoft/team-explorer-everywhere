// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Simply holds a {@link CheckinConflict} array and some aggregate status
 * information (were any resolvable, etc.).
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety conditionally thread-safe
 */
public class CheckinConflictContainer {
    private final CheckinConflict[] conflicts;
    private final boolean anyResolvable;

    /**
     * Creates a {@link CheckinConflictContainer} for the given conflicts.
     *
     * @param conflicts
     *        the conflicts detected (must not be <code>null</code>)
     * @param anyResolvable
     *        true if any of the conflicts can be resolved, false if none can be
     *        resolved.
     */
    public CheckinConflictContainer(final CheckinConflict[] conflicts, final boolean anyResolvable) {
        Check.notNull(conflicts, "conflicts"); //$NON-NLS-1$

        this.conflicts = conflicts;
        this.anyResolvable = anyResolvable;
    }

    /**
     * @return the conflicts this object contains. Do not modify the returned
     *         array to maintain thread-safety.
     */
    public CheckinConflict[] getConflicts() {
        return conflicts;
    }

    public boolean isAnyResolvable() {
        return anyResolvable;
    }
}
