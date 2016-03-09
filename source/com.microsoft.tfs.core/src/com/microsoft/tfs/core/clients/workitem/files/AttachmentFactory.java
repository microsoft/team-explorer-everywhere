// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.files;

import java.io.File;

import com.microsoft.tfs.core.clients.workitem.internal.files.AttachmentImpl;

/**
 * @since TEE-SDK-10.1
 */

public class AttachmentFactory {
    public static Attachment newAttachment(final File localFile, final String comment) {
        return new AttachmentImpl(localFile, comment);
    }
}
