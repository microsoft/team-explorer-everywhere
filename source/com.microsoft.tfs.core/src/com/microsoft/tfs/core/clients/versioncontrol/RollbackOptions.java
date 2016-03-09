// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * Options for rollback.
 *
 * @since TEE-SDK-10.1
 */
public final class RollbackOptions extends BitField {
    public final static RollbackOptions NONE = new RollbackOptions(0, "None"); //$NON-NLS-1$

    public final static RollbackOptions TO_VERSION = new RollbackOptions(1, "ToVersion"); //$NON-NLS-1$

    public final static RollbackOptions SILENT = new RollbackOptions(2, "Silent"); //$NON-NLS-1$

    public final static RollbackOptions KEEP_MERGE_HISTORY = new RollbackOptions(4, "KeepMergeHistory"); //$NON-NLS-1$

    public final static RollbackOptions NO_AUTO_RESOLVE = new RollbackOptions(8, "NoAutoResolve"); //$NON-NLS-1$

    public static RollbackOptions combine(final RollbackOptions[] changeTypes) {
        return new RollbackOptions(BitField.combine(changeTypes));
    }

    private RollbackOptions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private RollbackOptions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final RollbackOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final RollbackOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final RollbackOptions other) {
        return containsAnyInternal(other);
    }

    public RollbackOptions remove(final RollbackOptions other) {
        return new RollbackOptions(removeInternal(other));
    }

    public RollbackOptions retain(final RollbackOptions other) {
        return new RollbackOptions(retainInternal(other));
    }

    public RollbackOptions combine(final RollbackOptions other) {
        return new RollbackOptions(combineInternal(other));
    }
}
