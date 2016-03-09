// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilterScopeEnum;

public class WIFormLinksControlWITypeFilterScopeEnumFactory {
    public static WIFormLinksControlWITypeFilterScopeEnum fromType(final String type) {
        if (WIFormLinksControlWITypeFilterScopeEnum.PROJECT.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlWITypeFilterScopeEnum.PROJECT;
        }
        if (WIFormLinksControlWITypeFilterScopeEnum.ALL.getTypeString().equalsIgnoreCase(type)) {
            return WIFormLinksControlWITypeFilterScopeEnum.ALL;
        }

        return null;
    }
}
