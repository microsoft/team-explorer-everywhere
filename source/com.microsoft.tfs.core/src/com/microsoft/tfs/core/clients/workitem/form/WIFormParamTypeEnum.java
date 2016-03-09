// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * @since TEE-SDK-10.1
 */
public class WIFormParamTypeEnum {
    public static final WIFormParamTypeEnum ORIGINAL = new WIFormParamTypeEnum("Original"); //$NON-NLS-1$
    public static final WIFormParamTypeEnum CURRENT = new WIFormParamTypeEnum("Current"); //$NON-NLS-1$

    private final String type;

    private WIFormParamTypeEnum(final String type) {
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
