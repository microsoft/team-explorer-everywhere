// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * @since TEE-SDK-10.1
 */
public class WIFormLinksControlWITypeFilterEnum {
    public static final WIFormLinksControlWITypeFilterEnum INCLUDE = new WIFormLinksControlWITypeFilterEnum("Include"); //$NON-NLS-1$
    public static final WIFormLinksControlWITypeFilterEnum EXCLUDE = new WIFormLinksControlWITypeFilterEnum("Exclude"); //$NON-NLS-1$
    public static final WIFormLinksControlWITypeFilterEnum INCLUDEALL =
        new WIFormLinksControlWITypeFilterEnum("IncludeAll"); //$NON-NLS-1$

    private final String type;

    private WIFormLinksControlWITypeFilterEnum(final String type) {
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
