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
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.Check;

/**
 * Prints shelveset details.
 */
public final class ShelvesetPrinter {
    public static void printBriefShelvesets(final Shelveset[] shelvesets, final Display display) {
        Check.notNull(shelvesets, "shelvesets"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$

        final TextOutputTable table = new TextOutputTable(display.getWidth());
        table.setHeadingsVisible(true);
        table.setTruncateLongLines(true);
        table.setColumns(new Column[] {
            new Column(Messages.getString("ShelvesetPrinter.Shelveset"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("ShelvesetPrinter.Owner"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("ShelvesetPrinter.Comment"), Sizing.EXPAND) //$NON-NLS-1$
        });

        // Display the shelvesets.
        for (int i = 0; i < shelvesets.length; i++) {
            final Shelveset shelveset = shelvesets[i];
            Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$

            table.addRow(new String[] {
                shelveset.getName(),
                shelveset.getOwnerDisplayName(),
                shelveset.getComment()
            });
        }

        table.print(display.getPrintStream());
    }

    public static void printXMLShelvesets(final Shelveset[] shelvesets, final String elementName, final Display display)
        throws CLCException {
        Check.notNull(shelvesets, "shelvesets"); //$NON-NLS-1$
        Check.notNullOrEmpty(elementName, "elementName"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$

        try {
            final SimpleXMLWriter xmlWriter = new SimpleXMLWriter(display);

            xmlWriter.startDocument();
            xmlWriter.startElement("", "", elementName, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$

            for (int i = 0; i < shelvesets.length; i++) {
                final Shelveset shelveset = shelvesets[i];

                final AttributesImpl shelvesetAttributes = new AttributesImpl();

                shelvesetAttributes.addAttribute("", "", CommonXMLNames.NAME, "CDATA", shelveset.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                shelvesetAttributes.addAttribute("", "", CommonXMLNames.OWNER, "CDATA", shelveset.getOwnerName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                shelvesetAttributes.addAttribute(
                    "", //$NON-NLS-1$
                    "", //$NON-NLS-1$
                    CommonXMLNames.DATE,
                    "CDATA", //$NON-NLS-1$
                    SimpleXMLWriter.ISO_DATE_FORMAT.format(shelveset.getCreationDate().getTime()));

                xmlWriter.startElement("", "", CommonXMLNames.SHELVESET, shelvesetAttributes); //$NON-NLS-1$ //$NON-NLS-2$

                if (shelveset.getComment() != null && shelveset.getComment().length() > 0) {
                    xmlWriter.startElement("", "", CommonXMLNames.COMMENT, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$
                    xmlWriter.characters(shelveset.getComment().toCharArray(), 0, shelveset.getComment().length());
                    xmlWriter.endElement("", "", CommonXMLNames.COMMENT); //$NON-NLS-1$ //$NON-NLS-2$
                }

                if (shelveset.getCheckinNote() != null) {
                    final CheckinNoteFieldValue[] values = shelveset.getCheckinNote().getValues();

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

                xmlWriter.endElement("", "", CommonXMLNames.SHELVESET); //$NON-NLS-1$ //$NON-NLS-2$
            }

            xmlWriter.endElement("", "", elementName); //$NON-NLS-1$ //$NON-NLS-2$
            xmlWriter.endDocument();
        } catch (final SAXException e) {
            throw new CLCException(e);
        } catch (final TransformerConfigurationException e) {
            throw new CLCException(e);
        }
    }

    public static void printDetailedShelvesets(
        final Shelveset[] shelvesets,
        final DateFormat dateFormat,
        final Display display,
        final WorkItemClient workItemClient) {
        Check.notNull(shelvesets, "shelvesets"); //$NON-NLS-1$
        Check.notNull(dateFormat, "dateFormat"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        for (int i = 0; i < shelvesets.length; i++) {
            BasicPrinter.printSeparator(display, '=');

            printGeneral(shelvesets[i], dateFormat, display);

            /*
             * Use the ChangesetPrinter to do notes, since they're the same.
             */
            ChangesetPrinter.printCheckinNotes(shelvesets[i].getCheckinNote(), display);

            try {
                final WorkItemCheckinInfo[] workItemInfo = shelvesets[i].getWorkItemInfo(workItemClient);
                final WorkItem[] workItems = new WorkItem[workItemInfo.length];

                for (int j = 0; j < workItemInfo.length; j++) {
                    workItems[j] = workItemInfo[j].getWorkItem();
                }

                ChangesetPrinter.printWorkItems(workItems, display);
            } catch (final TECoreException e) {
                display.printErrorLine(e.getMessage());
            }

            /*
             * Unlike Changesets, we only have a comment string (not failures),
             * so use our method.
             */
            printPolicyOverrideComment(shelvesets[i].getPolicyOverrideComment(), display);
        }
    }

    private static void printGeneral(final Shelveset shelveset, final DateFormat dateFormat, final Display display) {
        display.printLine(Messages.getString("ShelvesetPrinter.ShelvesetColon") + shelveset.getName()); //$NON-NLS-1$
        display.printLine(Messages.getString("ShelvesetPrinter.OwnerColon") + shelveset.getOwnerDisplayName()); //$NON-NLS-1$
        display.printLine(Messages.getString("ShelvesetPrinter.DateColon") //$NON-NLS-1$
            + dateFormat.format(shelveset.getCreationDate().getTime()));

        /*
         * Print comment. Always print the header, even if no comment.
         */
        display.printLine(Messages.getString("ShelvesetPrinter.CommentColon")); //$NON-NLS-1$
        if (shelveset.getComment() != null) {
            display.printLine("  " + shelveset.getComment()); //$NON-NLS-1$
        }

        display.printLine(""); //$NON-NLS-1$
    }

    private static void printPolicyOverrideComment(final String overrideComment, final Display display) {
        if (overrideComment != null && overrideComment.length() > 0) {
            display.printLine(Messages.getString("ShelvesetPrinter.PolicyWarningsColon")); //$NON-NLS-1$
            display.printLine(Messages.getString("ShelvesetPrinter.OverrideReasonColon")); //$NON-NLS-1$
            display.printLine("  " + overrideComment); //$NON-NLS-1$

            display.printLine(""); //$NON-NLS-1$
        }
    }
}
