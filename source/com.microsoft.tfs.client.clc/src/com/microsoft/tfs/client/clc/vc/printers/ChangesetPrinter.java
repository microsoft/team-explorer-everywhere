// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.printers;

import java.text.DateFormat;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.xml.CommonXMLNames;
import com.microsoft.tfs.client.clc.xml.SimpleXMLWriter;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyFailureInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.Check;

/**
 * Prints changeset details.
 */
public final class ChangesetPrinter {
    public static void printBriefChangesets(
        final Changeset[] changesets,
        final boolean includeChanges,
        final DateFormat dateFormat,
        final Display display) {
        Check.notNull(changesets, "changesets"); //$NON-NLS-1$
        Check.notNull(dateFormat, "dateFormat"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$

        /*
         * Do the brief, tabular display.
         */
        final TextOutputTable table = new TextOutputTable(display.getWidth());
        table.setTruncateLongLines(true);

        if (includeChanges) {
            table.setColumns(new Column[] {
                new Column(Messages.getString("ChangesetPrinter.Changeset"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.Change"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.User"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.Date"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.Comment"), Sizing.EXPAND) //$NON-NLS-1$
            });
        } else {
            table.setColumns(new Column[] {
                new Column(Messages.getString("ChangesetPrinter.Changeset"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.User"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.Date"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.Comment"), Sizing.EXPAND) //$NON-NLS-1$
            });
        }

        for (int i = 0; i < changesets.length; i++) {
            final Changeset cs = changesets[i];
            Check.notNull(cs, "cs"); //$NON-NLS-1$

            if (includeChanges) {
                table.addRow(new String[] {
                    new Integer(cs.getChangesetID()).toString(),
                    getChangeString(cs),
                    cs.getOwnerDisplayName(),
                    dateFormat.format(cs.getDate().getTime()),
                    cs.getComment()
                });
            } else {
                table.addRow(new String[] {
                    new Integer(cs.getChangesetID()).toString(),
                    cs.getOwnerDisplayName(),
                    dateFormat.format(cs.getDate().getTime()),
                    cs.getComment()
                });
            }
        }

        table.print(display.getPrintStream());
    }

    private static String getChangeString(final Changeset cs) {
        Check.notNull(cs, "cs"); //$NON-NLS-1$

        if (cs.getChanges().length == 0) {
            return "none"; //$NON-NLS-1$
        }

        final StringBuffer changesBuffer = new StringBuffer();

        for (int i = 0; i < cs.getChanges().length; i++) {
            if (i > 0) {
                changesBuffer.append(", "); //$NON-NLS-1$
            }

            changesBuffer.append(cs.getChanges()[i].getChangeType().toUIString(true, cs.getChanges()[i]));
        }

        return changesBuffer.toString();
    }

    public static void printXMLChangesets(final Changeset[] changesets, final String elementName, final Display display)
        throws CLCException {
        Check.notNull(changesets, "changesets"); //$NON-NLS-1$
        Check.notNullOrEmpty(elementName, "elementName"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$

        try {
            final SimpleXMLWriter xmlWriter = new SimpleXMLWriter(display);

            xmlWriter.startDocument();
            xmlWriter.startElement("", "", elementName, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$

            for (int i = 0; i < changesets.length; i++) {
                final Changeset cs = changesets[i];
                final AttributesImpl csAttributes = new AttributesImpl();

                csAttributes.addAttribute("", "", CommonXMLNames.ID, "CDATA", Integer.toString(cs.getChangesetID())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                csAttributes.addAttribute("", "", CommonXMLNames.OWNER, "CDATA", cs.getOwner()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                csAttributes.addAttribute("", "", CommonXMLNames.COMMITTER, "CDATA", cs.getCommitter()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                csAttributes.addAttribute(
                    "", //$NON-NLS-1$
                    "", //$NON-NLS-1$
                    CommonXMLNames.DATE,
                    "CDATA", //$NON-NLS-1$
                    SimpleXMLWriter.ISO_DATE_FORMAT.format(cs.getDate().getTime()));

                xmlWriter.startElement("", "", CommonXMLNames.CHANGESET, csAttributes); //$NON-NLS-1$ //$NON-NLS-2$

                if (cs.getComment() != null && cs.getComment().length() > 0) {
                    xmlWriter.startElement("", "", CommonXMLNames.COMMENT, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$
                    xmlWriter.characters(cs.getComment().toCharArray(), 0, cs.getComment().length());
                    xmlWriter.endElement("", "", CommonXMLNames.COMMENT); //$NON-NLS-1$ //$NON-NLS-2$
                }

                /*
                 * Write the changed items.
                 */
                if (cs.getChanges() != null && cs.getChanges().length > 0) {
                    for (int j = 0; j < cs.getChanges().length; j++) {
                        final Change change = cs.getChanges()[j];
                        final AttributesImpl changeAttributes = new AttributesImpl();

                        changeAttributes.addAttribute(
                            "", //$NON-NLS-1$
                            "", //$NON-NLS-1$
                            CommonXMLNames.CHANGE_TYPE,
                            "CDATA", //$NON-NLS-1$
                            change.getChangeType().toUIString(false, change));

                        changeAttributes.addAttribute(
                            "", //$NON-NLS-1$
                            "", //$NON-NLS-1$
                            CommonXMLNames.SERVER_ITEM,
                            "CDATA", //$NON-NLS-1$
                            change.getItem().getServerItem());

                        xmlWriter.startElement("", "", CommonXMLNames.ITEM, changeAttributes); //$NON-NLS-1$ //$NON-NLS-2$
                        xmlWriter.endElement("", "", CommonXMLNames.ITEM); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }

                /*
                 * Print the check in notes as a table.
                 */
                final CheckinNote note = cs.getCheckinNote();
                if (note != null) {
                    final CheckinNoteFieldValue[] values = note.getValues();

                    if (values != null) {
                        for (int j = 0; j < values.length; j++) {
                            final CheckinNoteFieldValue value = values[j];
                            final AttributesImpl noteAttributes = new AttributesImpl();

                            noteAttributes.addAttribute("", "", CommonXMLNames.NAME, "CDATA", value.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                            xmlWriter.startElement("", "", CommonXMLNames.CHECK_IN_NOTE, noteAttributes); //$NON-NLS-1$ //$NON-NLS-2$
                            xmlWriter.characters(value.getValue().toCharArray(), 0, value.getValue().length());
                            xmlWriter.endElement("", "", CommonXMLNames.CHECK_IN_NOTE); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    }
                }

                if (cs.getPolicyOverride() != null
                    && cs.getPolicyOverride().getPolicyFailures() != null
                    && cs.getPolicyOverride().getPolicyFailures().length > 0) {
                    xmlWriter.startElement("", "", CommonXMLNames.POLICY_OVERRIDE, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$

                    xmlWriter.startElement("", "", CommonXMLNames.REASON, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$
                    xmlWriter.characters(
                        cs.getPolicyOverride().getComment().toCharArray(),
                        0,
                        cs.getPolicyOverride().getComment().length());
                    xmlWriter.endElement("", "", CommonXMLNames.REASON); //$NON-NLS-1$ //$NON-NLS-2$

                    final PolicyFailureInfo[] failures = cs.getPolicyOverride().getPolicyFailures();
                    for (int j = 0; j < failures.length; j++) {
                        xmlWriter.startElement("", "", CommonXMLNames.MESSAGE, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$
                        xmlWriter.characters(
                            failures[j].getMessage().toCharArray(),
                            0,
                            failures[j].getMessage().length());
                        xmlWriter.endElement("", "", CommonXMLNames.MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                    }

                    xmlWriter.endElement("", "", CommonXMLNames.POLICY_OVERRIDE); //$NON-NLS-1$ //$NON-NLS-2$
                }

                xmlWriter.endElement("", "", CommonXMLNames.CHANGESET); //$NON-NLS-1$ //$NON-NLS-2$
            }

            xmlWriter.endElement("", "", elementName); //$NON-NLS-1$ //$NON-NLS-2$
            xmlWriter.endDocument();
        } catch (final SAXException e) {
            throw new CLCException(e);
        } catch (final TransformerConfigurationException e) {
            throw new CLCException(e);
        }
    }

    public static void printDetailedChangesets(
        final Changeset[] changesets,
        final DateFormat dateFormat,
        final Display display,
        final WorkItemClient workItemClient) {
        Check.notNull(changesets, "changesets"); //$NON-NLS-1$
        Check.notNull(dateFormat, "dateFormat"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        for (int i = 0; i < changesets.length; i++) {
            if (i > 0) {
                BasicPrinter.printSeparator(display, '-');
            }

            printGeneral(changesets[i], dateFormat, display);

            try {
                printWorkItems(changesets[i].getWorkItems(workItemClient), display);
            } catch (final TECoreException e) {
                display.printErrorLine(e.getMessage());
            }

            printCheckinNotes(changesets[i].getCheckinNote(), display);
            printPolicyOverride(changesets[i].getPolicyOverride(), display);
        }
    }

    protected static void printWorkItems(final WorkItem[] workItems, final Display display) {
        if (workItems != null && workItems.length > 0) {
            final TextOutputTable table = new TextOutputTable(display.getWidth());
            table.setOverallIndent(2);

            table.setColumns(new Column[] {
                new Column(Messages.getString("ChangesetPrinter.ID"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.Type"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.State"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.AssignedTo"), Sizing.TIGHT), //$NON-NLS-1$
                new Column(Messages.getString("ChangesetPrinter.Title"), Sizing.EXPAND) //$NON-NLS-1$
            });

            for (int i = 0; i < workItems.length; i++) {
                final WorkItem wi = workItems[i];

                /*
                 * Some work item types may be missing these fields, so test for
                 * them individually.
                 */

                String workItemType = ""; //$NON-NLS-1$
                if (fieldHasValue(wi, CoreFieldReferenceNames.WORK_ITEM_TYPE)) {
                    workItemType =
                        wi.getFields().getField(CoreFieldReferenceNames.WORK_ITEM_TYPE).getValue().toString();
                }

                String state = ""; //$NON-NLS-1$
                if (fieldHasValue(wi, CoreFieldReferenceNames.STATE)) {
                    state = wi.getFields().getField(CoreFieldReferenceNames.STATE).getValue().toString();
                }

                String assignedTo = ""; //$NON-NLS-1$
                if (fieldHasValue(wi, CoreFieldReferenceNames.ASSIGNED_TO)) {
                    assignedTo = wi.getFields().getField(CoreFieldReferenceNames.ASSIGNED_TO).getValue().toString();
                }

                String title = ""; //$NON-NLS-1$
                if (fieldHasValue(wi, CoreFieldReferenceNames.TITLE)) {
                    title = wi.getFields().getField(CoreFieldReferenceNames.TITLE).getValue().toString();
                }

                table.addRow(new String[] {
                    Integer.toString(wi.getFields().getID()),
                    workItemType,
                    state,
                    assignedTo,
                    title
                });
            }

            display.printLine(Messages.getString("ChangesetPrinter.WorkItemsColon")); //$NON-NLS-1$
            table.print(display.getPrintStream());

            display.printLine(""); //$NON-NLS-1$
        }
    }

    private static boolean fieldHasValue(final WorkItem workItem, final String fieldName) {
        return workItem != null
            && workItem.getFields() != null
            && workItem.getFields().getField(fieldName) != null
            && workItem.getFields().getField(fieldName).getValue() != null;
    }

    private static void printGeneral(final Changeset changeset, final DateFormat dateFormat, final Display display) {
        display.printLine(
            Messages.getString("ChangesetPrinter.ChangesetColon") + Integer.toString(changeset.getChangesetID())); //$NON-NLS-1$
        display.printLine(Messages.getString("ChangesetPrinter.UserColon") + changeset.getOwnerDisplayName()); //$NON-NLS-1$
        display.printLine(
            Messages.getString("ChangesetPrinter.DateColon") + dateFormat.format(changeset.getDate().getTime())); //$NON-NLS-1$

        display.printLine(""); //$NON-NLS-1$

        /*
         * Print comment. Always print the header, even if no comment.
         */
        display.printLine(Messages.getString("ChangesetPrinter.CommentColon")); //$NON-NLS-1$
        if (changeset.getComment() != null) {
            display.printLine("  " + changeset.getComment()); //$NON-NLS-1$
        }

        display.printLine(""); //$NON-NLS-1$

        /*
         * Print the items.
         */
        if (changeset.getChanges() != null && changeset.getChanges().length > 0) {
            display.printLine(Messages.getString("ChangesetPrinter.ItemsColon")); //$NON-NLS-1$

            final Change[] changes = changeset.getChanges();
            for (int i = 0; i < changes.length; i++) {
                final Change change = changes[i];
                Check.notNull(change, "change"); //$NON-NLS-1$

                display.printLine("  " //$NON-NLS-1$
                    + change.getChangeType().toUIString(false, change.getItem())
                    + " " //$NON-NLS-1$
                    + change.getItem().getServerItem());
            }

            display.printLine(""); //$NON-NLS-1$
        }
    }

    protected static void printCheckinNotes(final CheckinNote note, final Display display) {
        if (note != null && note.getValues() != null && note.getValues().length > 0) {
            display.printLine(Messages.getString("ChangesetPrinter.CheckInNotesColon")); //$NON-NLS-1$

            final CheckinNoteFieldValue[] values = note.getValues();

            for (int i = 0; i < values.length; i++) {
                final CheckinNoteFieldValue value = values[i];
                Check.notNull(value, "value"); //$NON-NLS-1$

                display.printLine("  " + value.getName() + ": " + value.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
            }

            display.printLine(""); //$NON-NLS-1$
        }
    }

    private static void printPolicyOverride(final PolicyOverrideInfo overrideInfo, final Display display) {
        if (overrideInfo != null
            && overrideInfo.getPolicyFailures() != null
            && overrideInfo.getPolicyFailures().length > 0) {
            display.printLine(Messages.getString("ChangesetPrinter.PolicyWarningsColon")); //$NON-NLS-1$
            display.printLine(Messages.getString("ChangesetPrinter.OverrideReasonColon") + overrideInfo.getComment()); //$NON-NLS-1$
            display.printLine(Messages.getString("ChangesetPrinter.MessagesColon")); //$NON-NLS-1$

            final PolicyFailureInfo[] failures = overrideInfo.getPolicyFailures();
            for (int i = 0; i < failures.length; i++) {
                display.printLine("    " + failures[i].getMessage()); //$NON-NLS-1$
            }

            display.printLine(""); //$NON-NLS-1$
        }
    }
}
