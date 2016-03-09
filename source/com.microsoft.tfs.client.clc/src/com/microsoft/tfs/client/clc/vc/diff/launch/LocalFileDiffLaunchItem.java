// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff.launch;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.temp.TempStorageService;

public class LocalFileDiffLaunchItem extends AbstractDiffLaunchItem {
    private final String filePath;
    private final boolean noFileGiven;

    private final int encoding;
    private final long lastModified;
    private final boolean isTemporary;
    private String label;

    /**
     * Creates a {@link LocalFileDiffLaunchItem} with the default label text
     * (generated from the given arguments).
     *
     * @param filePath
     *        the local file path of this item. Pass null or empty for this item
     *        to use a temp file in the system temp location for this item (the
     *        temp file path can be retrieved via {@link #getFilePath()})
     * @param encoding
     *        the encoding for the file.
     * @param lastModified
     *        the last modification date of the file.
     * @param isTemporary
     *        true if the file is a temporary file, false if it is not.
     * @throws IOException
     *         if there was an error creating a temporary file (because the
     *         filePath argument was null or empty).
     */
    public LocalFileDiffLaunchItem(
        final String filePath,
        final int encoding,
        final long lastModified,
        final boolean isTemporary) throws IOException {
        if (filePath == null || new File(filePath).exists() == false) {
            noFileGiven = true;
            this.isTemporary = true;
            this.filePath = TempStorageService.getInstance().createTempFile().getAbsolutePath();
        } else {
            noFileGiven = false;
            this.isTemporary = isTemporary;
            this.filePath = filePath;
        }

        this.encoding = encoding;
        this.lastModified = lastModified;
    }

    @Override
    public int getEncoding() {
        return encoding;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public boolean isTemporary() {
        return isTemporary;
    }

    @Override
    public String getLabel() {
        if (label == null || label.length() == 0) {
            if (noFileGiven) {
                label = Messages.getString("LocalFileDiffLaunchItem.NoFilelabel"); //$NON-NLS-1$
            } else {
                final String messageFormat = Messages.getString("LocalFileDiffLaunchItem.FileLocalDateLabelFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    LocalPath.makeRelative(filePath, LocalPath.getCurrentWorkingDirectory()),
                    DiffLaunchItem.SHORT_DATE_TIME_FORMATTER.format(new Date(lastModified)));

                label = message;
            }
        }

        return label;
    }

    @Override
    public void setLabel(final String label) {
        this.label = label;
    }

}
