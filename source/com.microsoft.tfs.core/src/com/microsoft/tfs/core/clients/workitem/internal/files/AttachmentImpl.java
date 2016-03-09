// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.files;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnableToSaveException;
import com.microsoft.tfs.core.clients.workitem.files.Attachment;
import com.microsoft.tfs.core.clients.workitem.files.DownloadException;
import com.microsoft.tfs.core.clients.workitem.files.FileAttachmentMaxLengths;
import com.microsoft.tfs.core.clients.workitem.files.LocalFileStatus;
import com.microsoft.tfs.core.clients.workitem.files.WorkItemAttachmentUtils;
import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemUtils;
import com.microsoft.tfs.core.clients.workitem.internal.links.WITComponent;
import com.microsoft.tfs.core.clients.workitem.internal.update.UpdateXMLConstants;
import com.microsoft.tfs.util.xml.DOMUtils;

public class AttachmentImpl extends WITComponent implements Attachment {
    private static final String URL_FILE_ID_PARAMETER_NAME = "FileID"; //$NON-NLS-1$
    private static final String URL_FILE_NAME_PARAMETER_NAME = "FileName"; //$NON-NLS-1$
    private static final String URL_ENCODING = "UTF-8"; //$NON-NLS-1$

    /*
     * the date this attachment was added to the server
     */
    private final Date attachmentAddedDate;

    /*
     * the created date of the attached file
     */
    private final Date fileCreatedDate;

    /*
     * the modified date of the attached file
     */
    private final Date fileLastModifiedDate;

    /*
     * comment, if any
     */
    private final String comment;

    /*
     * attached file size in bytes
     */
    private final long fileSize;

    /*
     * The friendly name of the file this attachment represents
     */
    private final String fileName;

    /*
     * the GUID associated with this file attachment on the server
     */
    private String serverGuid;

    /*
     * An associated local file
     */
    private File localFile;

    public AttachmentImpl(final File localFile, final String comment) {
        super(true);
        final LocalFileStatus status = WorkItemAttachmentUtils.validateLocalFileForUpload(localFile);
        if (status != LocalFileStatus.VALID) {
            throw new IllegalArgumentException(status.getErrorMessage(localFile, null));
        }

        validateTextMaxLength(comment, "comment", FileAttachmentMaxLengths.COMMENT_MAX_LENGTH); //$NON-NLS-1$

        this.localFile = localFile;
        this.comment = comment;
        attachmentAddedDate = new Date();
        fileLastModifiedDate = new Date(localFile.lastModified());
        fileCreatedDate = fileLastModifiedDate;
        fileName = localFile.getName();
        fileSize = localFile.length();
    }

    public AttachmentImpl(
        final Date attachmentAddedDate,
        final Date fileCreatedDate,
        final Date fileLastModifiedDate,
        final String fileName,
        final String comment,
        final int fileSize,
        final int id) {
        super(false);
        this.attachmentAddedDate = attachmentAddedDate;
        this.fileCreatedDate = fileCreatedDate;
        this.fileLastModifiedDate = fileLastModifiedDate;
        this.fileName = fileName;
        this.comment = comment;
        this.fileSize = fileSize;
        setExtID(id);
    }

    /***************************************************************************
     * START of implementation of FileAttachment interface
     **************************************************************************/

    @Override
    public int getFileID() {
        return getExtID();
    }

    @Override
    public String getFileSizeAsString() {
        // The grouping separator (",") gets localized.
        final DecimalFormat decimalFormat = new DecimalFormat("#,###"); //$NON-NLS-1$
        final long bytes = getFileSize();

        /*
         * The following algorithm to convert the file size in bytes into a
         * friendly display size is exactly what Visual Studio's client does.
         * Note especially: -- they round up any values under 1000 bytes -- they
         * display decimal prefixes (1 KB = 1000 bytes) -- they drop fractions:
         * 112998 = 112 KB, not 113 KB
         */

        if (bytes == 0) {
            return Messages.getString("FileAttachmentImpl.ZeroKilobytes"); //$NON-NLS-1$
        }

        if (bytes < 1000) {
            return Messages.getString("FileAttachmentImpl.OneKilobyte"); //$NON-NLS-1$
        }

        final float kb = (float) bytes / (float) 1000;
        return MessageFormat.format(
            Messages.getString("FileAttachmentImpl.CountKiloBytesFormat"), //$NON-NLS-1$
            decimalFormat.format(Math.floor(kb)));
    }

    @Override
    public File getLocalFile() {
        return localFile;
    }

    @Override
    public URL getURL() {
        if (isNewlyCreated()) {
            try {
                return localFile.toURL();
            } catch (final MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            try {
                final StringBuffer buffer = new StringBuffer();
                buffer.append(getAssociatedCollection().getWorkItemInternal().getContext().getAttachmentServerURL());
                buffer.append("?"); //$NON-NLS-1$
                buffer.append(URL_FILE_ID_PARAMETER_NAME);
                buffer.append("="); //$NON-NLS-1$
                buffer.append(getExtID());
                buffer.append("&"); //$NON-NLS-1$
                buffer.append(URL_FILE_NAME_PARAMETER_NAME);
                buffer.append("="); //$NON-NLS-1$
                buffer.append(URLEncoder.encode(fileName, URL_ENCODING));
                return new URL(buffer.toString());
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public Date getAttachmentAddedDate() {
        return attachmentAddedDate;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public Date getCreatedDate() {
        return fileCreatedDate;
    }

    @Override
    public Date getLastModifiedDate() {
        return fileLastModifiedDate;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public void downloadTo(final File target) throws DownloadException {
        if (target == null) {
            throw new IllegalArgumentException("target must be non-null"); //$NON-NLS-1$
        }
        if (isNewlyCreated()) {
            throw new IllegalStateException("you cannot call downloadTo on newly created attachments"); //$NON-NLS-1$
        }
        final TFSTeamProjectCollection connection = getAssociatedCollection().getWorkItemInternal().getConnection();
        AttachmentUpDownHelper.download(getURL(), target, connection);
    }

    /***************************************************************************
     * END of implementation of FileAttachment interface
     **************************************************************************/

    /***************************************************************************
     * START of internal (FileAttachmentImpl) methods
     **************************************************************************/

    public void upload(final String areaNodeUri, final String projectUri) throws UnableToSaveException {
        final TFSTeamProjectCollection connection = getAssociatedCollection().getWorkItemInternal().getConnection();
        String uploadUrl;
        try {
            uploadUrl = getAssociatedCollection().getWorkItemInternal().getContext().getAttachmentServerURL();
        } catch (final Exception ex) {
            throw new UnableToSaveException(ex);
        }

        serverGuid = AttachmentUpDownHelper.upload(areaNodeUri, projectUri, localFile, uploadUrl, connection);
        localFile = null;
    }

    @Override
    protected void createXMLForAdd(final Element parentElement) {
        final SimpleDateFormat metadataDateFormat = InternalWorkItemUtils.newMetadataDateFormat();

        final Element element = DOMUtils.appendChild(parentElement, UpdateXMLConstants.ELEMENT_NAME_INSERT_FILE);
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_FIELD_NAME, CoreFieldReferenceNames.ATTACHED_FILES);
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_ORIGINAL_NAME, fileName);
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_FILE_NAME, serverGuid);
        element.setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_CREATION_DATE,
            metadataDateFormat.format(fileCreatedDate));
        element.setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_LAST_WRITE_DATE,
            metadataDateFormat.format(fileLastModifiedDate));
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_FILE_NAME, serverGuid);
        if (comment != null && comment.trim().length() > 0) {
            DOMUtils.appendChildWithText(element, UpdateXMLConstants.ELEMENT_NAME_COMMENT, comment.trim());
        }
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_FILE_SIZE, String.valueOf(fileSize));
    }

    @Override
    protected void createXMLForRemove(final Element parentElement) {
        final Element element = DOMUtils.appendChild(parentElement, UpdateXMLConstants.ELEMENT_NAME_REMOVE_FILE);
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_FILE_ID, String.valueOf(getExtID()));
    }

    @Override
    protected String getInsertTagName() {
        return UpdateXMLConstants.ELEMENT_NAME_INSERT_FILE;
    }

    @Override
    protected boolean isEquivalentTo(final WITComponent other) {
        return false;
    }

    /***************************************************************************
     * START of internal (FileAttachmentImpl) methods
     **************************************************************************/
}
