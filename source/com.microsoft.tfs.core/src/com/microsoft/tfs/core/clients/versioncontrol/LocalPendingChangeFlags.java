// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;
import com.microsoft.tfs.util.BitField;

/**
 * {@link LocalPendingChangeFlags} describes additional information on
 * {@link LocalPendingChange} objects.
 *
 * @since TEE-SDK-11.0
 */
public class LocalPendingChangeFlags extends BitField {
    public static final LocalPendingChangeFlags NONE = new LocalPendingChangeFlags(0, "None"); //$NON-NLS-1$

    public static final LocalPendingChangeFlags HAS_MERGE_CONFLICT = new LocalPendingChangeFlags(1, "HasMergeConflict"); //$NON-NLS-1$

    public static final LocalPendingChangeFlags IS_CANDIDATE = new LocalPendingChangeFlags(2, "IsCandidate"); //$NON-NLS-1$

    public static final LocalPendingChangeFlags EXECUTABLE = new LocalPendingChangeFlags(4, "Executable"); //$NON-NLS-1$

    public static final LocalPendingChangeFlags NOT_EXECUTABLE = new LocalPendingChangeFlags(8, "NotExecutable"); //$NON-NLS-1$

    public static final LocalPendingChangeFlags SYMLINK = new LocalPendingChangeFlags(16, "Symlink"); //$NON-NLS-1$

    public static final LocalPendingChangeFlags NOT_SYMLINK = new LocalPendingChangeFlags(32, "NotSymlink"); //$NON-NLS-1$

    private LocalPendingChangeFlags(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    public LocalPendingChangeFlags(final int flags) {
        super(flags);
    }

    public boolean containsAll(final LocalPendingChangeFlags other) {
        return containsAllInternal(other);
    }

    public boolean contains(final LocalPendingChangeFlags other) {
        return containsInternal(other);
    }

    public boolean containsAny(final LocalPendingChangeFlags other) {
        return containsAnyInternal(other);
    }

    public LocalPendingChangeFlags remove(final LocalPendingChangeFlags other) {
        return new LocalPendingChangeFlags(removeInternal(other));
    }

    public LocalPendingChangeFlags retain(final LocalPendingChangeFlags other) {
        return new LocalPendingChangeFlags(retainInternal(other));
    }

    public LocalPendingChangeFlags combine(final LocalPendingChangeFlags other) {
        return new LocalPendingChangeFlags(combineInternal(other));
    }

}
