// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import com.microsoft.tfs.core.pendingcheckin.CheckinConflict;

/**
 * An exception thrown when a checkin fails. Whether the problem can be resolved
 * or not can be determined by invoking the {@link #isAnyResolvable()} method.
 *
 * @since TEE-SDK-10.1
 */
public final class CheckinException extends VersionControlException {
    private final CheckinConflict[] checkinConflicts;
    private final boolean anyResolvable;
    private final boolean allConflictsResolved;

    public CheckinException(
        final CheckinConflict[] checkinConflicts,
        final boolean anyResolvable,
        final boolean allConflictsResolved,
        final String message) {
        this(checkinConflicts, anyResolvable, allConflictsResolved, message, null);
    }

    public CheckinException(
        final CheckinConflict[] checkinConflicts,
        final boolean anyResolvable,
        final boolean allConflictsResolved,
        final Throwable cause) {
        this(checkinConflicts, anyResolvable, allConflictsResolved, null, cause);
    }

    public CheckinException(
        final CheckinConflict[] checkinConflicts,
        final boolean anyResolvable,
        final boolean allConflictsResolved,
        final String message,
        final Throwable cause) {
        super(message, cause);

        this.checkinConflicts = checkinConflicts != null ? checkinConflicts.clone() : null;
        this.anyResolvable = anyResolvable;
        this.allConflictsResolved = allConflictsResolved;
    }

    /**
     * @return the {@link CheckinConflict}s that prevented this checkin from
     *         occurring
     */
    public CheckinConflict[] getCheckinConflicts() {
        return checkinConflicts;
    }

    /**
     * @return true if any of the items preventing the checkin from occurring
     *         are resolvable, false if they are all not resolvable.
     */
    public boolean isAnyResolvable() {
        return anyResolvable;
    }

    /**
     * @return true if the conflicts were all automatically resolved, false
     *         otherwise
     */
    public boolean allConflictsResolved() {
        return allConflictsResolved;
    }
}
