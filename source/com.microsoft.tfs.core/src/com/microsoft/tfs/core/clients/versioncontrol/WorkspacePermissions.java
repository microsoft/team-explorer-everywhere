// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.security.AccessControlEntry;
import com.microsoft.tfs.util.BitField;

/**
 * A set of permissions flags used for workspace permissions. They are most
 * often converted to integers (see {@link #toIntFlags()} and those values given
 * to {@link AccessControlEntry}.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class WorkspacePermissions extends BitField {
    public static final WorkspacePermissions NONE_OR_NOT_SUPPORTED = new WorkspacePermissions(0, "NoneOrNotSupported"); //$NON-NLS-1$

    public static final WorkspacePermissions READ = new WorkspacePermissions(1 << 0, "Read"); //$NON-NLS-1$

    public static final WorkspacePermissions USE = new WorkspacePermissions(1 << 1, "Use"); //$NON-NLS-1$

    public static final WorkspacePermissions CHECK_IN = new WorkspacePermissions(1 << 2, "CheckIn"); //$NON-NLS-1$

    public static final WorkspacePermissions ADMINISTER = new WorkspacePermissions(1 << 3, "Administer"); //$NON-NLS-1$

    public static WorkspacePermissions combine(final WorkspacePermissions[] values) {
        return new WorkspacePermissions(BitField.combine(values));
    }

    public static WorkspacePermissions fromIntFlags(final int flags) {
        return new WorkspacePermissions(flags);
    }

    private WorkspacePermissions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private WorkspacePermissions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final WorkspacePermissions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final WorkspacePermissions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final WorkspacePermissions other) {
        return containsAnyInternal(other);
    }

    public WorkspacePermissions remove(final WorkspacePermissions other) {
        return new WorkspacePermissions(removeInternal(other));
    }

    public WorkspacePermissions retain(final WorkspacePermissions other) {
        return new WorkspacePermissions(retainInternal(other));
    }

    public WorkspacePermissions combine(final WorkspacePermissions other) {
        return new WorkspacePermissions(combineInternal(other));
    }
}
