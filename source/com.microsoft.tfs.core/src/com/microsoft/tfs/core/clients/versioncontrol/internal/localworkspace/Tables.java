// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.util.BitField;

public class Tables extends BitField {
    public static final Tables NONE = new Tables(0, "None"); //$NON-NLS-1$
    public static final Tables WORKSPACE_PROPERTIES = new Tables(1, "WorkspaceProperties"); //$NON-NLS-1$
    public static final Tables LOCAL_VERSION = new Tables(2, "LocalVersion"); //$NON-NLS-1$
    public static final Tables PENDING_CHANGES = new Tables(4, "PendingChanges"); //$NON-NLS-1$
    public static final Tables LOCAL_VERSION_HEADER = new Tables(8, "LocalVersionHeader"); //$NON-NLS-1$
    public static final Tables PENDING_CHANGES_HEADER = new Tables(16, "PendingChangesHeader"); //$NON-NLS-1$

    private Tables(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private Tables(final int flags) {
        super(flags);
    }

    public boolean containsAll(final Tables other) {
        return containsAllInternal(other);
    }

    public boolean contains(final Tables other) {
        return containsInternal(other);
    }

    public boolean containsAny(final Tables other) {
        return containsAnyInternal(other);
    }

    public Tables remove(final Tables other) {
        return new Tables(removeInternal(other));
    }

    public Tables retain(final Tables other) {
        return new Tables(retainInternal(other));
    }

    public Tables combine(final Tables other) {
        return new Tables(combineInternal(other));
    }

}