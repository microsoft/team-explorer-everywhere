// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlFilterOnEnum;

public class WIFormLinksControlFilterOnEnumFactory {
    public static WIFormLinksControlFilterOnEnum fromType(final String type) {
        if (WIFormLinksControlFilterOnEnum.FORWARDNAME.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlFilterOnEnum.FORWARDNAME;
        }
        if (WIFormLinksControlFilterOnEnum.REVERSENAME.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlFilterOnEnum.REVERSENAME;
        }

        return null;
    }
}
