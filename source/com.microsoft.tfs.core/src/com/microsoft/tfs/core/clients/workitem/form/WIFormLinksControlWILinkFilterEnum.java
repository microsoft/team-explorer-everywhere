// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * @since TEE-SDK-10.1
 */
public class WIFormLinksControlWILinkFilterEnum {
    public static final WIFormLinksControlWILinkFilterEnum INCLUDE = new WIFormLinksControlWILinkFilterEnum("Include"); //$NON-NLS-1$
    public static final WIFormLinksControlWILinkFilterEnum EXCLUDE = new WIFormLinksControlWILinkFilterEnum("Exclude"); //$NON-NLS-1$
    public static final WIFormLinksControlWILinkFilterEnum INCLUDEALL =
        new WIFormLinksControlWILinkFilterEnum("IncludeAll"); //$NON-NLS-1$
    public static final WIFormLinksControlWILinkFilterEnum EXCLUDEALL =
        new WIFormLinksControlWILinkFilterEnum("ExcludeAll"); //$NON-NLS-1$

    private final String type;

    private WIFormLinksControlWILinkFilterEnum(final String type) {
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
