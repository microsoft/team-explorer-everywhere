// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLabelPositionEnum;

public class WIFormLabelPositionEnumFactory {
    public static WIFormLabelPositionEnum fromType(final String type) {
        if (WIFormLabelPositionEnum.RIGHT.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLabelPositionEnum.RIGHT;
        }
        if (WIFormLabelPositionEnum.TOP.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLabelPositionEnum.TOP;
        }
        if (WIFormLabelPositionEnum.LEFT.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLabelPositionEnum.LEFT;
        }
        if (WIFormLabelPositionEnum.BOTTOM.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLabelPositionEnum.BOTTOM;
        }

        return null;
    }

}
