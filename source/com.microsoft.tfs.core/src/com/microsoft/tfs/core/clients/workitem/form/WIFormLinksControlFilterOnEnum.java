// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * @since TEE-SDK-10.1
 */
public class WIFormLinksControlFilterOnEnum {
    public static final WIFormLinksControlFilterOnEnum FORWARDNAME = new WIFormLinksControlFilterOnEnum("ForwardName"); //$NON-NLS-1$
    public static final WIFormLinksControlFilterOnEnum REVERSENAME = new WIFormLinksControlFilterOnEnum("ReverseName"); //$NON-NLS-1$

    private final String type;

    private WIFormLinksControlFilterOnEnum(final String type) {
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
