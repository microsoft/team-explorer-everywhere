// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

public class ChangeItemType {
    public static final ChangeItemType PENDING = new ChangeItemType("pending"); //$NON-NLS-1$
    public static final ChangeItemType CHANGESET = new ChangeItemType("changeset"); //$NON-NLS-1$
    public static final ChangeItemType SHELVESET = new ChangeItemType("shelveset"); //$NON-NLS-1$

    private final String type;

    private ChangeItemType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
