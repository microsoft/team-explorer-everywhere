// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.files;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;

/**
 * Exception thrown when a file attachment download fails.
 *
 * @since TEE-SDK-10.1
 */
public class DownloadException extends WorkItemException {
    private static final long serialVersionUID = -8013133053326679324L;

    public DownloadException(final Exception cause) {
        super(cause);
    }

    public DownloadException(final String message) {
        super(message);
    }
}
