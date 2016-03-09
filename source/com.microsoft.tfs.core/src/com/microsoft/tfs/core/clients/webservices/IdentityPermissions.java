// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.util.BitField;

public class IdentityPermissions extends BitField {
    public static final IdentityPermissions READ = new IdentityPermissions(1);

    public static final IdentityPermissions WRITE = new IdentityPermissions(2);

    public static final IdentityPermissions DELETE = new IdentityPermissions(4);

    public static final IdentityPermissions MANAGE_MEMBERSHIP = new IdentityPermissions(8);

    public static final IdentityPermissions ALL_PERMISSIONS =
        new IdentityPermissions(combine(new IdentityPermissions[] {
            READ,
            WRITE,
            DELETE,
            MANAGE_MEMBERSHIP
    }).toIntFlags());

    public static IdentityPermissions combine(final IdentityPermissions[] values) {
        return new IdentityPermissions(BitField.combine(values));
    }

    private IdentityPermissions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private IdentityPermissions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final IdentityPermissions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final IdentityPermissions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final IdentityPermissions other) {
        return containsAnyInternal(other);
    }

    public IdentityPermissions remove(final IdentityPermissions other) {
        return new IdentityPermissions(removeInternal(other));
    }

    public IdentityPermissions retain(final IdentityPermissions other) {
        return new IdentityPermissions(retainInternal(other));
    }

    public IdentityPermissions combine(final IdentityPermissions other) {
        return new IdentityPermissions(combineInternal(other));
    }
}
