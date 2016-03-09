// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

/**
 * Specifies the display mode of the user account.
 *
 * @since TEE-SDK-10.1
 */
public class UserDisplayMode {
    public static final UserDisplayMode ACCOUNT_NAME = new UserDisplayMode("ACCOUNT_NAME", 1); //$NON-NLS-1$
    public static final UserDisplayMode FRIENDLY_NAME = new UserDisplayMode("FRIENDLY_NAME", 2); //$NON-NLS-1$

    private final String s;
    private final int ordinal;

    private UserDisplayMode(final String s, final int ordinal) {
        this.s = s;
        this.ordinal = ordinal;
    }

    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String toString() {
        return s;
    }
}
