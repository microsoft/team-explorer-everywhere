// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.files;

import java.io.File;
import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * @since TEE-SDK-10.1
 */
public class LocalFileStatus {
    public static final LocalFileStatus VALID = new LocalFileStatus(""); //$NON-NLS-1$

    public static final LocalFileStatus INVALID_NULL =
        new LocalFileStatus(Messages.getString("LocalFileStatus.ArgumentMustNotBeNull")); //$NON-NLS-1$

    public static final LocalFileStatus INVALID_NON_EXISTING =
        new LocalFileStatus(Messages.getString("LocalFileStatus.PathDoesNotReferenceExistingFileFormat")); //$NON-NLS-1$

    public static final LocalFileStatus INVALID_NON_FILE =
        new LocalFileStatus(Messages.getString("LocalFileStatus.PathDoesNotReferenceFileFormat")); //$NON-NLS-1$

    public static final LocalFileStatus INVALID_NON_READABLE =
        new LocalFileStatus(Messages.getString("LocalFileStatus.FileLocatedAtPathIsNotReadableFormat")); //$NON-NLS-1$

    public static final LocalFileStatus INVALID_NAME_LENGTH =
        new LocalFileStatus(Messages.getString("LocalFileStatus.FileNameLengthOfPathIsOverMaximumLengthFormat")); //$NON-NLS-1$

    private final String messageFormat;

    private LocalFileStatus(final String messageFormat) {
        this.messageFormat = messageFormat;
    }

    public String getErrorMessage(final File file, final String path) {
        String messageParamterPath = path;
        if (messageParamterPath == null) {
            messageParamterPath = (file != null ? file.getAbsolutePath() : ""); //$NON-NLS-1$
        }

        return MessageFormat.format(messageFormat, messageParamterPath, FileAttachmentMaxLengths.FILE_NAME_MAX_LENGTH);
    }
}
