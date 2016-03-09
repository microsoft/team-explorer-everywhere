// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.files;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;

/**
 * @since TEE-SDK-10.1
 */
public class FileAttachmentDownloadException extends WorkItemException {
    private static final long serialVersionUID = 3066759403653912669L;

    public FileAttachmentDownloadException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FileAttachmentDownloadException(final String message) {
        super(message);
    }

}
