// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilterEnum;

public class WIFormLinksControlWITypeFilterEnumFactory {
    public static WIFormLinksControlWITypeFilterEnum fromType(final String type) {
        if (WIFormLinksControlWITypeFilterEnum.INCLUDE.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlWITypeFilterEnum.INCLUDE;
        }
        if (WIFormLinksControlWITypeFilterEnum.EXCLUDE.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlWITypeFilterEnum.EXCLUDE;
        }
        if (WIFormLinksControlWITypeFilterEnum.INCLUDEALL.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlWITypeFilterEnum.INCLUDEALL;
        }

        return null;
    }
}
