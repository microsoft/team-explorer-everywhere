// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.util;

import java.text.DateFormat;
import java.util.Date;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.revision.Revision;
import com.microsoft.tfs.core.clients.workitem.revision.RevisionField;

public class HistoryDisplay {
    public static void displayHistory(final WorkItem workItem, final Display display) {
        displayHistory(workItem, display, -1, true);
    }

    public static void displayHistory(
        final WorkItem workItem,
        final Display display,
        final int revisionsToDisplay,
        final boolean printHeader) {
        final DateFormat historyDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

        final TextOutputTable table = new TextOutputTable(display.getWidth());
        table.setOverallIndent(2);
        table.setWrapColumnText(true);
        table.setHeadingsVisible(true);

        table.setColumns(new TextOutputTable.Column[] {
            new TextOutputTable.Column(Messages.getString("HistoryDisplay.Field"), Sizing.TIGHT), //$NON-NLS-1$
            new TextOutputTable.Column(Messages.getString("HistoryDisplay.OldValue"), Sizing.TIGHT), //$NON-NLS-1$
            new TextOutputTable.Column(Messages.getString("HistoryDisplay.NewValue"), Sizing.TIGHT) //$NON-NLS-1$
        });

        int limit = 0;
        if (revisionsToDisplay > 0 && revisionsToDisplay < workItem.getRevisions().size()) {
            limit = workItem.getRevisions().size() - revisionsToDisplay;
        }

        if (printHeader) {
            display.printLine(Messages.getString("HistoryDisplay.HistoryColon")); //$NON-NLS-1$
            display.printLine(""); //$NON-NLS-1$
        }

        for (int i = workItem.getRevisions().size() - 1; i >= limit; i--) {
            final Revision revision = workItem.getRevisions().get(i);
            final Date revisionDate = revision.getRevisionDate();

            display.printLine(historyDateFormat.format(revisionDate) + ": " + revision.getTagLine()); //$NON-NLS-1$

            final String history = (String) revision.getField(CoreFieldReferenceNames.HISTORY).getValue();
            if (history != null) {
                display.printLine(history);
            }

            if (i == 0) {
                table.clearColumns();
                table.setColumns(new TextOutputTable.Column[] {
                    new TextOutputTable.Column(Messages.getString("HistoryDisplay.Field"), Sizing.TIGHT), //$NON-NLS-1$
                    new TextOutputTable.Column(Messages.getString("HistoryDisplay.NewValue"), Sizing.TIGHT) //$NON-NLS-1$
                });
            }

            for (int j = 0; j < revision.getFields().length; j++) {
                final RevisionField field = revision.getFields()[j];

                if (field.shouldIgnoreForDeltaTable()) {
                    continue;
                }

                final Object oldValue = field.getOriginalValue();
                final Object newValue = field.getValue();

                if (objectsAreEqual(oldValue, newValue)) {
                    continue;
                }

                if (i != 0) {
                    table.addRow(new String[] {
                        field.getName(),
                        getStringForDisplay(oldValue, historyDateFormat),
                        getStringForDisplay(newValue, historyDateFormat)
                    });
                } else {
                    table.addRow(new String[] {
                        field.getName(),
                        getStringForDisplay(newValue, historyDateFormat)
                    });
                }
            }

            table.print(display.getPrintStream());
            table.clearRows();
            display.printLine(""); //$NON-NLS-1$
        }
    }

    private static boolean objectsAreEqual(final Object o1, final Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    private static String getStringForDisplay(final Object value, final DateFormat historyDateFormat) {
        if (value instanceof Date) {
            return historyDateFormat.format(((Date) value));
        }
        if (value != null) {
            return value.toString();
        }
        return ""; //$NON-NLS-1$
    }
}
