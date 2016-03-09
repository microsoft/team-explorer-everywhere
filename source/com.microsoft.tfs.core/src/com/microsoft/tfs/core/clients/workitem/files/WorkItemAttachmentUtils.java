// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.files;

import java.io.File;

/**
 * @since TEE-SDK-10.1
 */
public class WorkItemAttachmentUtils {
    public static LocalFileStatus validateLocalFileForUpload(final File file) {
        if (file == null) {
            return LocalFileStatus.INVALID_NULL;
        }

        if (!file.exists()) {
            return LocalFileStatus.INVALID_NON_EXISTING;
        }

        if (!file.isFile()) {
            return LocalFileStatus.INVALID_NON_FILE;
        }

        if (!file.canRead()) {
            return LocalFileStatus.INVALID_NON_READABLE;
        }

        if (file.getName().length() > FileAttachmentMaxLengths.FILE_NAME_MAX_LENGTH) {
            return LocalFileStatus.INVALID_NAME_LENGTH;
        }

        return LocalFileStatus.VALID;
    }
}
