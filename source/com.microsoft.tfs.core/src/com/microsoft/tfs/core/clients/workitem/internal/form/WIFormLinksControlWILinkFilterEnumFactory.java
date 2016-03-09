// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilterEnum;

public class WIFormLinksControlWILinkFilterEnumFactory {
    public static WIFormLinksControlWILinkFilterEnum fromType(final String type) {
        if (WIFormLinksControlWILinkFilterEnum.INCLUDE.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlWILinkFilterEnum.INCLUDE;
        }
        if (WIFormLinksControlWILinkFilterEnum.EXCLUDE.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlWILinkFilterEnum.EXCLUDE;
        }
        if (WIFormLinksControlWILinkFilterEnum.INCLUDEALL.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlWILinkFilterEnum.INCLUDEALL;
        }
        if (WIFormLinksControlWILinkFilterEnum.EXCLUDEALL.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlWILinkFilterEnum.EXCLUDEALL;
        }

        return null;
    }
}
