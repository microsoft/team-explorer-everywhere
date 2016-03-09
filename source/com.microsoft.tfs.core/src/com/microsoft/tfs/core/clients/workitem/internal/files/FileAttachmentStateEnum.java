// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.files;

public class FileAttachmentStateEnum {
    public static final FileAttachmentStateEnum NEW = new FileAttachmentStateEnum("NEW"); //$NON-NLS-1$
    public static final FileAttachmentStateEnum ASSOCIATED_NEW = new FileAttachmentStateEnum("ASSOCIATED_NEW"); //$NON-NLS-1$
    public static final FileAttachmentStateEnum ASSOCIATED_NEW_UPLOADED =
        new FileAttachmentStateEnum("ASSOCIATED_NEW_UPLOADED"); //$NON-NLS-1$
    public static final FileAttachmentStateEnum ASSOCIATED = new FileAttachmentStateEnum("ASSOCIATED"); //$NON-NLS-1$
    public static final FileAttachmentStateEnum ASSOCIATED_DELETED = new FileAttachmentStateEnum("ASSOCIATED_DELETED"); //$NON-NLS-1$
    public static final FileAttachmentStateEnum DETACHED = new FileAttachmentStateEnum("DETACHED"); //$NON-NLS-1$

    private final String value;

    private FileAttachmentStateEnum(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
