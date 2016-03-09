// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * @since TEE-SDK-10.1
 */
public class WIFormReadOnlyEnum {
    public static final WIFormReadOnlyEnum FALSE = new WIFormReadOnlyEnum("False"); //$NON-NLS-1$
    public static final WIFormReadOnlyEnum TRUE = new WIFormReadOnlyEnum("True"); //$NON-NLS-1$

    private final String type;

    private WIFormReadOnlyEnum(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public String getTypeString() {
        return type;
    }

}
