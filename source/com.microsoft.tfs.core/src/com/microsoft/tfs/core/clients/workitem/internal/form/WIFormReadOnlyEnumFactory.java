// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import com.microsoft.tfs.core.clients.workitem.form.WIFormReadOnlyEnum;

public class WIFormReadOnlyEnumFactory {
    public static WIFormReadOnlyEnum fromType(final String type) {
        if (WIFormReadOnlyEnum.FALSE.getTypeString().equalsIgnoreCase(type)) {
            return WIFormReadOnlyEnum.FALSE;
        }
        if (WIFormReadOnlyEnum.TRUE.getTypeString().equalsIgnoreCase(type)) {
            return WIFormReadOnlyEnum.TRUE;
        }

        return null;
    }

}
