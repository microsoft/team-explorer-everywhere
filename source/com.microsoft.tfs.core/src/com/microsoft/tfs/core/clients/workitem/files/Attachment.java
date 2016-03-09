// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.files;

import java.io.File;
import java.net.URL;
import java.util.Date;

/**
 * Represents a file attached to a work item.
 *
 * @since TEE-SDK-10.1
 */
public interface Attachment {
    public Date getAttachmentAddedDate();

    public Date getCreatedDate();

    public Date getLastModifiedDate();

    public URL getURL();

    public File getLocalFile();

    public String getFileName();

    public String getComment();

    public long getFileSize();

    public String getFileSizeAsString();

    public boolean isNewlyCreated();

    public boolean isPendingDelete();

    public void downloadTo(File target) throws DownloadException;

    public int getFileID();
}
