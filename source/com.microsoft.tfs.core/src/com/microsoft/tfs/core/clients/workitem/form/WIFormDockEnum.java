// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * @since TEE-SDK-10.1
 */
public class WIFormDockEnum {
    public static final WIFormDockEnum RIGHT = new WIFormDockEnum("Right"); //$NON-NLS-1$
    public static final WIFormDockEnum TOP = new WIFormDockEnum("Top"); //$NON-NLS-1$
    public static final WIFormDockEnum FILL = new WIFormDockEnum("Fill"); //$NON-NLS-1$
    public static final WIFormDockEnum LEFT = new WIFormDockEnum("Left"); //$NON-NLS-1$
    public static final WIFormDockEnum BOTTOM = new WIFormDockEnum("Bottom"); //$NON-NLS-1$

    private final String type;

    private WIFormDockEnum(final String type) {
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
