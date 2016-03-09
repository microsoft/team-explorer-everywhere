// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * @since TEE-SDK-10.1
 */
public class WIFormLabelPositionEnum {
    public static final WIFormLabelPositionEnum RIGHT = new WIFormLabelPositionEnum("Right"); //$NON-NLS-1$
    public static final WIFormLabelPositionEnum TOP = new WIFormLabelPositionEnum("Top"); //$NON-NLS-1$
    public static final WIFormLabelPositionEnum LEFT = new WIFormLabelPositionEnum("Left"); //$NON-NLS-1$
    public static final WIFormLabelPositionEnum BOTTOM = new WIFormLabelPositionEnum("Bottom"); //$NON-NLS-1$

    private final String type;

    private WIFormLabelPositionEnum(final String type) {
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
