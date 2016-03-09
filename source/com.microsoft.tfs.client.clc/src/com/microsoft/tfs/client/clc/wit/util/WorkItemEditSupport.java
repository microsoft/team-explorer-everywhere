// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.util;

import java.io.File;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.wit.options.OptionAddAttachment;
import com.microsoft.tfs.client.clc.wit.options.OptionAddExternalLink;
import com.microsoft.tfs.client.clc.wit.options.OptionAddHyperlink;
import com.microsoft.tfs.client.clc.wit.options.OptionAddRelated;
import com.microsoft.tfs.client.clc.wit.options.OptionForce;
import com.microsoft.tfs.client.clc.wit.options.OptionRemoveAttachment;
import com.microsoft.tfs.client.clc.wit.options.OptionRemoveLink;
import com.microsoft.tfs.client.clc.wit.options.OptionRemoveRelated;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.artifact.MalformedURIException;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnableToSaveException;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;
import com.microsoft.tfs.core.clients.workitem.files.Attachment;
import com.microsoft.tfs.core.clients.workitem.files.AttachmentFactory;
import com.microsoft.tfs.core.clients.workitem.files.FileAttachmentMaxLengths;
import com.microsoft.tfs.core.clients.workitem.files.LocalFileStatus;
import com.microsoft.tfs.core.clients.workitem.files.WorkItemAttachmentUtils;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Hyperlink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.LinkTextMaxLengths;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkUtils;

public class WorkItemEditSupport {
    public static final Class[] EDIT_OPTIONAL_OPTION_TYPES = new Class[] {
        OptionForce.class,
        OptionAddRelated.class,
        OptionAddHyperlink.class,
        OptionAddExternalLink.class,
        OptionRemoveRelated.class,
        OptionRemoveLink.class,
        OptionAddAttachment.class,
        OptionRemoveAttachment.class
    };

    public static final Class[] CREATE_OPTIONAL_OPTION_TYPES = new Class[] {
        OptionForce.class,
        OptionAddRelated.class,
        OptionAddHyperlink.class,
        OptionAddExternalLink.class,
        OptionAddAttachment.class,
    };

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    public static int editWorkItem(
        final WorkItem workItem,
        final String[] fieldValuePairs,
        final Command command,
        final Display display) throws InvalidFreeArgumentException, CLCException {
        modifyFields(workItem, fieldValuePairs, display);
        modifyLinks(workItem, command, display);
        modifyAttachments(workItem, command, display);

        return saveWorkItem(workItem, display, command.findOptionType(OptionForce.class) != null);
    }

    private static void modifyLinks(final WorkItem workItem, final Command command, final Display display)
        throws CLCException {
        final Option[] addRelatedOptions = command.findAllOptionTypes(OptionAddRelated.class);
        for (int i = 0; i < addRelatedOptions.length; i++) {
            addRelatedLink(workItem, (OptionAddRelated) addRelatedOptions[i], display);
        }

        final Option[] removeRelatedOptions = command.findAllOptionTypes(OptionRemoveRelated.class);
        for (int i = 0; i < removeRelatedOptions.length; i++) {
            removeRelatedLink(workItem, (OptionRemoveRelated) removeRelatedOptions[i], display);
        }

        final Option[] removeLinkOptions = command.findAllOptionTypes(OptionRemoveLink.class);
        for (int i = 0; i < removeLinkOptions.length; i++) {
            removeLink(workItem, (OptionRemoveLink) removeLinkOptions[i], display);
        }

        final Option[] addHyperlinkOptions = command.findAllOptionTypes(OptionAddHyperlink.class);
        for (int i = 0; i < addHyperlinkOptions.length; i++) {
            addHyperlink(workItem, (OptionAddHyperlink) addHyperlinkOptions[i], display);
        }

        final Option[] addExternalLinkOptions = command.findAllOptionTypes(OptionAddExternalLink.class);
        for (int i = 0; i < addExternalLinkOptions.length; i++) {
            addExternalLink(workItem, (OptionAddExternalLink) addExternalLinkOptions[i], display);
        }
    }

    private static void addExternalLink(
        final WorkItem workItem,
        final OptionAddExternalLink option,
        final Display display) throws CLCException {
        if (option.getComment() != null && option.getComment().length() > LinkTextMaxLengths.COMMENT_MAX_LENGTH) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.LinkCommentsCannotBeLongerThanFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, LinkTextMaxLengths.COMMENT_MAX_LENGTH);

            throw new CLCException(message);
        }

        final RegisteredLinkType linkType = workItem.getClient().getRegisteredLinkTypes().get(option.getLinkType());

        if (linkType == null) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.LinkTypeIsNotValidFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, option.getLinkType());

            throw new CLCException(message);
        }

        try {
            ArtifactID.checkURIIsWellFormed(option.getURI());
        } catch (final MalformedURIException ex) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.URINotWellFormedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, option.getURI());

            throw new CLCException(message);
        }

        final ExternalLink link = LinkFactory.newExternalLink(linkType, option.getURI(), option.getComment(), false);

        if (!workItem.getLinks().add(link)) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.ExternalLinkAlreadyExistsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, link.getURI());

            throw new CLCException(message);
        }

        String comment = ""; //$NON-NLS-1$
        if (link.getComment() != null && link.getComment().trim().length() > 0) {
            comment = link.getComment().trim();
        }

        final String messageFormat = Messages.getString("WorkItemEditSupport.AddedExternalLinkFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, linkType.getName(), link.getURI(), comment);

        display.getPrintStream().println(message);
    }

    private static void addHyperlink(final WorkItem workItem, final OptionAddHyperlink option, final Display display)
        throws CLCException {
        if (option.getComment() != null && option.getComment().length() > LinkTextMaxLengths.COMMENT_MAX_LENGTH) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.LinkCommentsCannotBeLongerThanFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, LinkTextMaxLengths.COMMENT_MAX_LENGTH);

            throw new CLCException(message);
        }

        if (option.getLocation() != null
            && option.getLocation().length() > LinkTextMaxLengths.HYPERLINK_LOCATION_MAX_LENGTH) {
            final String messageFormat =
                Messages.getString("WorkItemEditSupport.HyperlinkLocationsCannotBeLongerThanFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, LinkTextMaxLengths.HYPERLINK_LOCATION_MAX_LENGTH);

            throw new CLCException(message);
        }

        final Hyperlink link = LinkFactory.newHyperlink(option.getLocation(), option.getComment(), false);

        if (!workItem.getLinks().add(link)) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.HyperlinkAlreadyExistsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, link.getLocation());

            throw new CLCException(message);
        }

        String comment = ""; //$NON-NLS-1$
        if (link.getComment() != null && link.getComment().trim().length() > 0) {
            comment = link.getComment().trim();
        }

        final String messageFormat = Messages.getString("WorkItemEditSupport.AddedHyperlinkFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, link.getLocation(), comment);

        display.getPrintStream().println(message);
    }

    private static void removeLink(final WorkItem workItem, final OptionRemoveLink option, final Display display)
        throws CLCException {
        for (final Link link : workItem.getLinks()) {
            if (!(link instanceof RelatedLink)) {
                if (link.getLinkID() == option.getNumber()) {
                    workItem.getLinks().remove(link);
                    final String messageFormat =
                        Messages.getString("WorkItemEditSupport.RemovedLinkFromWorkItemFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, Integer.toString(option.getNumber()));

                    display.getPrintStream().println(message);
                    return;
                }
            }
        }

        final String messageFormat = Messages.getString("WorkItemEditSupport.LinkIDDoesNotExistForThisWorkItemFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Integer.toString(option.getNumber()));

        throw new CLCException(message);
    }

    private static void removeRelatedLink(
        final WorkItem workItem,
        final OptionRemoveRelated option,
        final Display display) throws CLCException {
        final int targetWorkItemId = option.getNumber();

        for (final Link link : workItem.getLinks()) {
            if (link instanceof RelatedLink) {
                final RelatedLink relatedLink = (RelatedLink) link;
                if (relatedLink.getTargetWorkItemID() == targetWorkItemId) {
                    workItem.getLinks().remove(link);

                    final String messageFormat =
                        Messages.getString("WorkItemEditSupport.RemovedRelatedLinkFromWorkItemFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, Integer.toString(targetWorkItemId));

                    display.getPrintStream().println(message);
                    return;
                }
            }
        }

        final String messageFormat =
            Messages.getString("WorkItemEditSupport.RelatedLinkDoesNotExistForThisWorkItemFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Integer.toString(targetWorkItemId));

        throw new CLCException(message);
    }

    private static void addRelatedLink(final WorkItem workItem, final OptionAddRelated option, final Display display)
        throws CLCException {
        if (option.getComment() != null && option.getComment().length() > LinkTextMaxLengths.COMMENT_MAX_LENGTH) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.LinkCommentsCannotBeLongerThanFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, LinkTextMaxLengths.COMMENT_MAX_LENGTH);

            throw new CLCException(message);
        }

        final WorkItem relatedWorkItem = workItem.getClient().getWorkItemByID(option.getWorkItemID());
        if (relatedWorkItem == null) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.CannotAddRelatedDoesNotExistFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(option.getWorkItemID()));

            throw new CLCException(message);
        }

        final RelatedLink link = LinkFactory.newRelatedLink(workItem, relatedWorkItem, option.getComment(), false);

        if (!workItem.getLinks().add(link)) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.RelatedLinkAlreadyExistsFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, WorkItemLinkUtils.buildDescriptionFromWorkItem(relatedWorkItem));

            throw new CLCException(message);
        }

        String comment = ""; //$NON-NLS-1$
        if (link.getComment() != null && link.getComment().trim().length() > 0) {
            comment = link.getComment().trim();
        }

        final String messageFormat = Messages.getString("WorkItemEditSupport.AddedRelatedLinkFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(
            messageFormat,
            WorkItemLinkUtils.buildDescriptionFromWorkItem(relatedWorkItem),
            comment);

        display.getPrintStream().println(message);
    }

    private static void modifyAttachments(final WorkItem workItem, final Command command, final Display display)
        throws CLCException {
        final Option[] addAttachmentOptions = command.findAllOptionTypes(OptionAddAttachment.class);
        for (int i = 0; i < addAttachmentOptions.length; i++) {
            addAttachment(workItem, (OptionAddAttachment) addAttachmentOptions[i], display);
        }

        final Option[] removeAttachmentOptions = command.findAllOptionTypes(OptionRemoveAttachment.class);
        for (int i = 0; i < removeAttachmentOptions.length; i++) {
            removeAttachment(workItem, (OptionRemoveAttachment) removeAttachmentOptions[i], display);
        }
    }

    private static void removeAttachment(
        final WorkItem workItem,
        final OptionRemoveAttachment option,
        final Display display) throws CLCException {
        final int fileId = option.getNumber();
        final Attachment attachment = workItem.getAttachments().getAttachmentByFileID(fileId);
        if (attachment == null) {
            final String messageFormat = Messages.getString("WorkItemEditSupport.FileIDNotValidForThisWorkItemFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(fileId));

            throw new CLCException(message);
        }

        workItem.getAttachments().remove(attachment);

        final String messageFormat = Messages.getString("WorkItemEditSupport.RemovedFileAttachmentFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Integer.toString(fileId));

        display.getPrintStream().println(message);
    }

    private static void addAttachment(final WorkItem workItem, final OptionAddAttachment option, final Display display)
        throws CLCException {
        final File file = option.getLocalFile();
        final String comment = option.getComment();

        if (comment != null && comment.length() > FileAttachmentMaxLengths.COMMENT_MAX_LENGTH) {
            final String messageFormat =
                Messages.getString("WorkItemEditSupport.FileAttachmentCommentsCannotBeLongerThanFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, FileAttachmentMaxLengths.COMMENT_MAX_LENGTH);

            throw new CLCException(message);
        }

        final LocalFileStatus status = WorkItemAttachmentUtils.validateLocalFileForUpload(file);
        if (status != LocalFileStatus.VALID) {
            throw new CLCException(status.getErrorMessage(file, option.getSpecifiedFilePath()));
        }

        final Attachment attachment = AttachmentFactory.newAttachment(file, comment);
        workItem.getAttachments().add(attachment);

        String attachmentComment = ""; //$NON-NLS-1$
        if (attachment.getComment() != null && attachment.getComment().trim().length() > 0) {
            attachmentComment = attachment.getComment().trim();
        }

        final String messageFormat = Messages.getString("WorkItemEditSupport.AddedFileAttachmentFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, file.getAbsolutePath(), attachmentComment);

        display.getPrintStream().println(message);
    }

    private static int saveWorkItem(final WorkItem workItem, final Display display, final boolean force)
        throws InvalidFreeArgumentException {
        if (!workItem.isDirty()) {
            display.getPrintStream().println(
                Messages.getString("WorkItemEditSupport.WorkItemNotChangedAfterEditNoSave")); //$NON-NLS-1$
            return ExitCode.SUCCESS;
        } else if (workItem.isValid() || force) {
            try {
                workItem.save();
            } catch (final UnableToSaveException ex) {
                final String messageFormat = Messages.getString("WorkItemEditSupport.UnableToSaveFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, ex.getLocalizedMessage());

                display.getErrorPrintStream().println(message);
                return ExitCode.FAILURE;
            }

            final String messageFormat = Messages.getString("WorkItemEditSupport.SavedNewRevisionFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                Integer.toString(workItem.getFields().getRevision()),
                Integer.toString(workItem.getFields().getID()));

            display.getPrintStream().println(message);
            display.getPrintStream().println();
            HistoryDisplay.displayHistory(workItem, display, 1, false);
            return ExitCode.SUCCESS;
        } else {
            display.getErrorPrintStream().println(
                Messages.getString("WorkItemEditSupport.WorkItemInvalidAfterEditNoSave")); //$NON-NLS-1$
            for (final Field field : workItem.getFields()) {
                if (field.getStatus() != FieldStatus.VALID) {
                    final String messageFormat = Messages.getString("WorkItemEditSupport.ChangedFieldFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        field.getReferenceName(),
                        getValueAsStringForDisplay(field.getValue()),
                        field.getStatus().getInvalidMessage(field));

                    display.getErrorPrintStream().println(message);
                }
                if (field.getStatus() == FieldStatus.INVALID_LIST_VALUE) {
                    final StringBuffer sb = new StringBuffer();
                    sb.append("\t--" + Messages.getString("WorkItemEditSupport.PickListValues") + ":").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    for (final String value : field.getAllowedValues()) {
                        sb.append("\t" + value).append(NEWLINE); //$NON-NLS-1$
                    }
                    display.getErrorPrintStream().println(sb.toString());
                } else if (field.getStatus() == FieldStatus.INVALID_TYPE) {
                    final String messageFormat = "\t--" + Messages.getString("WorkItemEditSupport.TypeOfFieldFormat"); //$NON-NLS-1$ //$NON-NLS-2$
                    final String message = MessageFormat.format(
                        messageFormat,
                        field.getReferenceName(),
                        field.getFieldDefinition().getFieldType().getDisplayName());

                    display.getErrorPrintStream().println(message);
                }
            }
            return ExitCode.FAILURE;
        }
    }

    private static void modifyFields(final WorkItem workItem, final String[] fieldValuePairs, final Display display)
        throws InvalidFreeArgumentException {
        if (fieldValuePairs != null) {
            for (int i = 0; i < fieldValuePairs.length; i++) {
                final String argument = fieldValuePairs[i];
                final String[] parts = argument.split("="); //$NON-NLS-1$

                if (parts.length != 2) {
                    final String messageFormat =
                        Messages.getString("WorkItemEditSupport.ArgumentMustBeInFieldValuePairSyntaxFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, argument);

                    throw new InvalidFreeArgumentException(message);
                }

                final String fieldName = parts[0];
                final String value = parts[1];

                if (!workItem.getFields().contains(fieldName)) {
                    final String messageFormat =
                        Messages.getString("WorkItemEditSupport.ArgumentDoesNotReferenceExistingFieldFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, argument);

                    throw new InvalidFreeArgumentException(message);
                }

                final Field field = workItem.getFields().getField(fieldName);
                field.setValue(value);

                final String messageFormat = Messages.getString("WorkItemEditSupport.SetFieldFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, field.getReferenceName(), value);

                display.getPrintStream().println(message);
            }
        }
    }

    private static String getValueAsStringForDisplay(final Object value) {
        return (value != null ? value.toString() : ""); //$NON-NLS-1$
    }
}
