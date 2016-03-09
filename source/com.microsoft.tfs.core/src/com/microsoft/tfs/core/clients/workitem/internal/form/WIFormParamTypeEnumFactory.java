// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import com.microsoft.tfs.core.clients.workitem.form.WIFormParamTypeEnum;

public class WIFormParamTypeEnumFactory {
    public static WIFormParamTypeEnum fromType(final String type) {
        if (WIFormParamTypeEnum.CURRENT.getTypeString().equalsIgnoreCase(type)) {
            return WIFormParamTypeEnum.CURRENT;
        }
        if (WIFormParamTypeEnum.ORIGINAL.getTypeString().equalsIgnoreCase(type)) {
            return WIFormParamTypeEnum.ORIGINAL;
        }

        return null;
    }
}
