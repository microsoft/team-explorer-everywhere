// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

/**
 * Describes the implementation of a link.
 *
 * @since TEE-SDK-10.1
 */
public interface Link {
    public RegisteredLinkType getLinkType();

    public String getComment();

    public void setComment(String comment);

    public String getDescription();

    public boolean isNewlyCreated();

    public boolean isPendingDelete();

    public int getLinkID();

    public boolean isReadOnly();
}
