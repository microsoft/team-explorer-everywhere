// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

public class LinkStateEnum {
    /**
     * The link is newly created (either by the user or by the object model) and
     * is not associated with a link collection
     */
    public static final LinkStateEnum NEW = new LinkStateEnum("NEW"); //$NON-NLS-1$

    /**
     * The link is associated with a link collection
     */
    public static final LinkStateEnum ASSOCIATED = new LinkStateEnum("ASSOCIATED"); //$NON-NLS-1$

    /**
     * The link is newly created (not yet saved) and is associated with a link
     * collection
     */
    public static final LinkStateEnum ASSOCIATED_NEW = new LinkStateEnum("ASSOCIATED_NEW"); //$NON-NLS-1$

    /**
     * The link is associated with a link collection and is marked for deletion
     * on the next update
     */
    public static final LinkStateEnum ASSOCIATED_DELETED = new LinkStateEnum("ASSOCIATED_DELETED"); //$NON-NLS-1$

    /**
     * The link is associated with a link collection and has been modified - it
     * will participate in the next update
     */
    public static final LinkStateEnum ASSOCIATED_MODIFIED = new LinkStateEnum("ASSOCIATED_MODIFIED"); //$NON-NLS-1$

    private final String value;

    private LinkStateEnum(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
