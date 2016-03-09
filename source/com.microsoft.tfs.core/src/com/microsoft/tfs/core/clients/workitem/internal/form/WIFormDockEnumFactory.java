// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import com.microsoft.tfs.core.clients.workitem.form.WIFormDockEnum;

public class WIFormDockEnumFactory {
    public static WIFormDockEnum fromType(final String type) {
        if (WIFormDockEnum.RIGHT.getTypeString().equalsIgnoreCase(type)) {
            return WIFormDockEnum.RIGHT;
        }
        if (WIFormDockEnum.TOP.getTypeString().equalsIgnoreCase(type)) {
            return WIFormDockEnum.TOP;
        }
        if (WIFormDockEnum.FILL.getTypeString().equalsIgnoreCase(type)) {
            return WIFormDockEnum.FILL;
        }
        if (WIFormDockEnum.LEFT.getTypeString().equalsIgnoreCase(type)) {
            return WIFormDockEnum.LEFT;
        }
        if (WIFormDockEnum.BOTTOM.getTypeString().equalsIgnoreCase(type)) {
            return WIFormDockEnum.BOTTOM;
        }

        return null;
    }

}
