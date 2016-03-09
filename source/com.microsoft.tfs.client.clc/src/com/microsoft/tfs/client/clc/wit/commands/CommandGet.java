// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.wit.options.OptionNoAttachments;
import com.microsoft.tfs.client.clc.wit.options.OptionNoHistory;
import com.microsoft.tfs.client.clc.wit.options.OptionNoLinks;
import com.microsoft.tfs.client.clc.wit.options.OptionSkipResolveLinks;
import com.microsoft.tfs.client.clc.wit.util.HistoryDisplay;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.files.Attachment;
import com.microsoft.tfs.core.clients.workitem.internal.link.LinkCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateErrorCallback;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkCollection;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public class CommandGet extends BaseWITCommand {
    private static final Log log = LogFactory.getLog(CommandGet.class);

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandGet.WITHelpText1") //$NON-NLS-1$
        };
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        return new AcceptedOptionSet[] {
            /*
             * wit get <id> [/nohistory] [/nolinks] [/skipresolvelinks]
             * [/noattachments] [fieldName]...
             */
            new AcceptedOptionSet(new Class[] {
                OptionNoHistory.class,
                OptionNoLinks.class,
                OptionNoAttachments.class,
                OptionSkipResolveLinks.class
            }, "<id> [fieldName]...") //$NON-NLS-1$
        };
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (getFreeArguments().length < 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandGet.MustSpecifyWorkItemID")); //$NON-NLS-1$
        }
        int id = -1;
        try {
            id = Integer.parseInt(getFreeArguments()[0]);
        } catch (final NumberFormatException ex) {
            final String messageFormat = Messages.getString("CommandGet.ArgumentIsNotAWorkItemIDFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getFreeArguments()[0]);

            throw new InvalidFreeArgumentException(message);
        }

        final WorkItem workItem = getWorkItemByID(id);

        String[] displayFieldNames = null;
        if (getFreeArguments().length > 1) {
            displayFieldNames = new String[getFreeArguments().length - 1];
            System.arraycopy(getFreeArguments(), 1, displayFieldNames, 0, displayFieldNames.length);
        }

        displayHeader(workItem);
        displayFields(workItem, displayFieldNames);

        if (findOptionType(OptionNoHistory.class) == null) {
            HistoryDisplay.displayHistory(workItem, getDisplay());
        }

        if (findOptionType(OptionNoLinks.class) == null) {
            displayLinks(workItem);
        }

        if (findOptionType(OptionNoAttachments.class) == null) {
            displayFileAttachments(workItem);
        }
    }

    private void displayFileAttachments(final WorkItem workItem) {
        if (workItem.getAttachments().size() > 0) {
            getDisplay().printLine(Messages.getString("CommandGet.FileAttachmentsColon")); //$NON-NLS-1$
            getDisplay().printLine(""); //$NON-NLS-1$

            final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
            table.setOverallIndent(2);
            table.setWrapColumnText(true);
            table.setHeadingsVisible(true);

            table.setColumns(new TextOutputTable.Column[] {
                new TextOutputTable.Column(Messages.getString("CommandGet.FileID"), Sizing.TIGHT), //$NON-NLS-1$
                new TextOutputTable.Column(Messages.getString("CommandGet.Name"), Sizing.TIGHT), //$NON-NLS-1$
                new TextOutputTable.Column(Messages.getString("CommandGet.Size"), Sizing.TIGHT), //$NON-NLS-1$
                new TextOutputTable.Column(Messages.getString("CommandGet.Comments"), Sizing.EXPAND) //$NON-NLS-1$
            });

            for (final Attachment attachment : workItem.getAttachments()) {
                table.addRow(new String[] {
                    String.valueOf(attachment.getFileID()),
                    attachment.getFileName(),
                    attachment.getFileSizeAsString(),
                    attachment.getComment()
                });
            }

            table.print(getDisplay().getPrintStream());
            getDisplay().printLine(""); //$NON-NLS-1$
        }
    }

    private void displayLinks(final WorkItem workItem) {
        final LinkCollection linkCollection = workItem.getLinks();
        final boolean allComputed = ((LinkCollectionImpl) linkCollection).allDescriptionsComputed();

        if (!allComputed && findOptionType(OptionSkipResolveLinks.class) == null) {
            computeLinkDescriptions(linkCollection);
        }

        if (linkCollection.size() > 0) {
            getDisplay().printLine(Messages.getString("CommandGet.LinksColon")); //$NON-NLS-1$
            getDisplay().printLine(""); //$NON-NLS-1$

            final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
            table.setOverallIndent(2);
            table.setWrapColumnText(true);
            table.setHeadingsVisible(true);

            table.setColumns(new TextOutputTable.Column[] {
                new TextOutputTable.Column(Messages.getString("CommandGet.LinkID"), Sizing.TIGHT), //$NON-NLS-1$
                new TextOutputTable.Column(Messages.getString("CommandGet.LinkType"), Sizing.TIGHT), //$NON-NLS-1$
                new TextOutputTable.Column(Messages.getString("CommandGet.Description"), Sizing.TIGHT), //$NON-NLS-1$
                new TextOutputTable.Column(Messages.getString("CommandGet.Comments"), Sizing.EXPAND) //$NON-NLS-1$
            });

            for (final Link link : linkCollection) {
                String linkId = Messages.getString("CommandGet.NotApplicable"); //$NON-NLS-1$
                if (!(link instanceof RelatedLink)) {
                    linkId = String.valueOf(link.getLinkID());
                }
                table.addRow(new String[] {
                    linkId,
                    link.getLinkType().getName(),
                    link.getDescription(),
                    link.getComment()
                });
            }

            table.print(getDisplay().getPrintStream());
            getDisplay().printLine(""); //$NON-NLS-1$
        }
    }

    private void computeLinkDescriptions(final LinkCollection linkCollection) {
        final DescriptionUpdateErrorCallback errorCallback = new DescriptionUpdateErrorCallback() {
            @Override
            public void onDescriptionUpdateError(final Throwable error) {
                log.error(Messages.getString("CommandGet.ErrorUpdatingWorkItemLinkDescriptions"), error); //$NON-NLS-1$
            }
        };

        final LinkCollectionImpl linkCollectionImpl = (LinkCollectionImpl) linkCollection;
        final Runnable runnable = linkCollectionImpl.getDescriptionUpdateRunnable(errorCallback, null);

        /*
         * In a GUI application, the link description updater would probably be
         * run in a background thread Since we're CLI, we just run it
         * synchronously
         */
        runnable.run();
    }

    private void displayFields(final WorkItem workItem, final String[] displayFieldNames) throws CLCException {
        FieldDefinition[] fieldDefinitions;

        if (displayFieldNames != null && displayFieldNames.length > 0) {
            final Map<String, FieldDefinition> referenceNameToFieldDefinition = new HashMap<String, FieldDefinition>();
            for (final FieldDefinition fieldDefinition : workItem.getType().getFieldDefinitions()) {
                referenceNameToFieldDefinition.put(fieldDefinition.getReferenceName(), fieldDefinition);
            }

            final Set<String> invalidFieldNames = new HashSet<String>();
            fieldDefinitions = new FieldDefinition[displayFieldNames.length];

            for (int i = 0; i < displayFieldNames.length; i++) {
                fieldDefinitions[i] = referenceNameToFieldDefinition.get(displayFieldNames[i]);
                if (fieldDefinitions[i] == null) {
                    invalidFieldNames.add(displayFieldNames[i]);
                }
            }

            if (invalidFieldNames.size() > 0) {
                final StringBuffer sb = new StringBuffer();
                sb.append(Messages.getString("CommandGet.FollowingFieldNamesAreNotValid")).append(NEWLINE); //$NON-NLS-1$
                for (final String invalidFieldName : invalidFieldNames) {
                    sb.append(invalidFieldName).append(NEWLINE);
                }
                throw new CLCException(sb.toString());
            }
        } else {
            fieldDefinitions = workItem.getType().getFieldDefinitions().getFieldDefinitions();
            Arrays.sort(fieldDefinitions, new FieldDefinitionComparator());
        }

        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
        table.setOverallIndent(2);
        table.setWrapColumnText(true);
        table.setHeadingsVisible(true);

        table.setColumns(new TextOutputTable.Column[] {
            new TextOutputTable.Column(Messages.getString("CommandGet.Field"), Sizing.TIGHT), //$NON-NLS-1$
            new TextOutputTable.Column(Messages.getString("CommandGet.Value"), Sizing.EXPAND) //$NON-NLS-1$
        });

        for (int i = 0; i < fieldDefinitions.length; i++) {
            if (fieldDefinitions[i].getReferenceName().equals(CoreFieldReferenceNames.HISTORY)
                || fieldDefinitions[i].getReferenceName().equals(CoreFieldReferenceNames.LINK_TYPE)) {
                continue;
            }

            final Object fieldValue = workItem.getFields().getField(fieldDefinitions[i].getReferenceName()).getValue();
            table.addRow(new String[] {
                fieldDefinitions[i].getName(),
                (fieldValue != null ? fieldValue.toString() : "") //$NON-NLS-1$
            });
        }

        table.print(getDisplay().getPrintStream());
        getDisplay().printLine(""); //$NON-NLS-1$
    }

    private void displayHeader(final WorkItem workItem) {
        final String header = workItem.getType().getName()
            + " " //$NON-NLS-1$
            + workItem.getFields().getID()
            + " : " //$NON-NLS-1$
            + workItem.getFields().getField(CoreFieldReferenceNames.TITLE).getValue();
        getDisplay().printLine(header);
        getDisplay().printLine(""); //$NON-NLS-1$
    }

    private static class FieldDefinitionComparator implements Comparator<FieldDefinition> {
        @Override
        public int compare(final FieldDefinition f1, final FieldDefinition f2) {
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }
}
