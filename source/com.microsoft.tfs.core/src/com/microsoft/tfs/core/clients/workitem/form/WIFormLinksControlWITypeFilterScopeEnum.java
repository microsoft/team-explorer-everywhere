// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * @since TEE-SDK-10.1
 */
public class WIFormLinksControlWITypeFilterScopeEnum {
    public static final WIFormLinksControlWITypeFilterScopeEnum PROJECT =
        new WIFormLinksControlWITypeFilterScopeEnum("Project"); //$NON-NLS-1$
    public static final WIFormLinksControlWITypeFilterScopeEnum ALL =
        new WIFormLinksControlWITypeFilterScopeEnum("All"); //$NON-NLS-1$

    private final String type;

    private WIFormLinksControlWITypeFilterScopeEnum(final String type) {
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
