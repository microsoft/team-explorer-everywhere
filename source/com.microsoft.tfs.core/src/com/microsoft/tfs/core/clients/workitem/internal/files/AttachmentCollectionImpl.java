// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.files;

import java.text.MessageFormat;
import java.util.Iterator;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnableToSaveException;
import com.microsoft.tfs.core.clients.workitem.files.Attachment;
import com.microsoft.tfs.core.clients.workitem.files.AttachmentCollection;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.links.WITComponentCollection;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectImpl;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitemconfiguration.WorkItemConfigurationSettingsClient;

public class AttachmentCollectionImpl extends WITComponentCollection<Attachment> implements AttachmentCollection {
    public AttachmentCollectionImpl(final WorkItemImpl workItem) {
        super(workItem);
    }

    /*
     * ************************************************************************
     * START of implementation of AttachmentCollection interface
     * ***********************************************************************
     */

    @Override
    public Iterator<Attachment> iterator() {
        return getComponentSet().iterator();
    }

    @Override
    public int size() {
        return getComponentSet().size();
    }

    @Override
    public boolean add(final Attachment attachment) {
        return addComponent((AttachmentImpl) attachment);
    }

    @Override
    public void remove(final Attachment attachment) {
        removeComponent((AttachmentImpl) attachment);
    }

    @Override
    public Attachment getAttachmentByFileID(final int fileId) {
        for (final Attachment attachment : getComponentSet()) {
            final AttachmentImpl attachmentImpl = (AttachmentImpl) attachment;
            if (attachment.getFileID() == fileId && attachmentImpl.shouldIncludeAsPartOfPublicCollection()) {
                return attachment;
            }
        }
        return null;
    }

    /*
     * ************************************************************************
     * END of implementation of AttachmentCollection interface
     * ***********************************************************************
     */

    /*
     * ************************************************************************
     * START of internal (AttachmentCollectionImpl) methods
     * ***********************************************************************
     */

    @Override
    public void preSave() throws UnableToSaveException {
        /*
         * check that all attachments are valid for upload
         */
        validateAttachmentSizes();

        /*
         * compute area node URI and project URI
         */
        final ProjectImpl project = getWorkItemInternal().getTypeInternal().getProjectInternal();
        final int areaId = getWorkItem().getFields().getAreaID();

        final String projectUri = project.getURI();
        String areaNodeUri;

        if (areaId == project.getID()) {
            final Node areaRootNode = project.getAreaRootNode();
            areaNodeUri = areaRootNode.getURI();
        } else {
            final Node areaNode = project.getNodeInternal().findNodeDownwards(areaId);
            areaNodeUri = areaNode.getURI();
        }

        /*
         * upload each attachment
         */
        for (final Attachment attachment : getComponentSet()) {
            if (attachment.isNewlyCreated()) {
                final AttachmentImpl attachmentImpl = (AttachmentImpl) attachment;
                attachmentImpl.upload(areaNodeUri, projectUri);
            }
        }
    }

    private void validateAttachmentSizes() throws UnableToSaveException {
        /*
         * Skip the check if we don't have any attachments to be uploaded. This
         * saves a potential server round trip to the config settings service.
         */
        if (!hasNewlyCreatedAttachments()) {
            return;
        }

        try {
            final WorkItemConfigurationSettingsClient settingsClient =
                (WorkItemConfigurationSettingsClient) getWorkItemInternal().getConnection().getClient(
                    WorkItemConfigurationSettingsClient.class);
            long maxSize = settingsClient.getMaxAttachmentSize();
            boolean updatedMaxSize = false;

            for (final Attachment attachment : getComponentSet()) {
                if (attachment.isNewlyCreated()) {
                    if (attachment.getFileSize() > maxSize) {
                        if (!updatedMaxSize) {
                            settingsClient.updateMaxAttachmentSize();
                            updatedMaxSize = true;
                            maxSize = settingsClient.updateMaxAttachmentSize();
                            if (attachment.getFileSize() <= maxSize) {
                                continue;
                            }
                        }

                        throw new UnableToSaveException(MessageFormat.format(
                            //@formatter:off
                            Messages.getString("AttachmentCollectionImpl.FileAttachmentLargerThanMaximumAttachmentSizeFormat"), //$NON-NLS-1$
                            //@formatter:on
                            attachment.getLocalFile().getAbsolutePath(),
                            maxSize));
                    }
                }
            }
        } catch (final Exception ex) {
            throw new UnableToSaveException(ex);
        }
    }

    public boolean hasNewlyCreatedAttachments() {
        for (final Attachment attachment : getComponentSet()) {
            if (attachment.isNewlyCreated()) {
                return true;
            }
        }
        return false;
    }

    /*
     * ************************************************************************
     * END of internal (AttachmentCollectionImpl) methods
     * ***********************************************************************
     */
}
