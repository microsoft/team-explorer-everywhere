// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.printers;

import java.text.DateFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.InformationNodeConverters;
import com.microsoft.tfs.util.Check;

/**
 * Prints build details in a table..
 */
public final class BuildPrinter {
    private static String getFinishTimeString(final IBuildDetail detail, final DateFormat dateFormat) {
        if (detail.isBuildFinished()) {
            return dateFormat.format(detail.getFinishTime().getTime());
        }

        return ""; //$NON-NLS-1$
    }

    private static String getChangesetString(final IBuildDetail detail) {
        final int changesetID = InformationNodeConverters.getChangesetID(detail.getInformation());
        if (changesetID > 0) {
            return Integer.toString(changesetID);
        }

        return ""; //$NON-NLS-1$
    }

    public static int printBuildDetails(
        final IBuildDetail[] details,
        final IBuildServer server,
        final DateFormat dateFormat,
        final Display display) {
        final TextOutputTable table = new TextOutputTable(display.getWidth());
        table.setColumns(new Column[] {
            new Column(Messages.getString("BuildPrinter.ColumnHeaderBuildName"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("BuildPrinter.ColumnHeaderStatus"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("BuildPrinter.ColumnHeaderDateCompleted"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("BuildPrinter.ColumnHeaderChangeset"), Sizing.TIGHT) //$NON-NLS-1$
        });

        for (int i = 0; i < details.length; i++) {
            final IBuildDetail detail = details[i];

            if (detail == null) {
                continue;
            }

            table.addRow(new String[] {
                detail.getBuildNumber(),
                server.getDisplayText(detail.getStatus()),
                getFinishTimeString(detail, dateFormat),
                getChangesetString(detail)
            });
        }

        if (table.getRowCount() > 0) {
            table.print(display.getPrintStream());
        }

        return table.getRowCount();
    }

    public static int printQueuedBuilds(
        final IQueuedBuild[] queuedBuilds,
        final IBuildServer server,
        final DateFormat dateFormat,
        final Display display) {
        Check.notNull(queuedBuilds, "queuedBuilds"); //$NON-NLS-1$
        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(dateFormat, "dateFormat"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$

        final TextOutputTable table = new TextOutputTable(display.getWidth());
        table.setColumns(new Column[] {
            new Column(Messages.getString("BuildPrinter.ColumnHeaderBuildName"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("BuildPrinter.ColumnHeaderStatus"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("BuildPrinter.ColumnHeaderDateCompleted"), Sizing.TIGHT), //$NON-NLS-1$
        });

        for (int i = 0; i < queuedBuilds.length; i++) {
            final IQueuedBuild queuedBuild = queuedBuilds[i];

            if (queuedBuild == null || queuedBuild.getID() == 0 || queuedBuild.getBuild() == null) {
                continue;
            }

            final IBuildDetail detail = queuedBuild.getBuild();

            table.addRow(new String[] {
                detail.getBuildNumber(),
                server.getDisplayText(detail.getStatus()),
                getFinishTimeString(detail, dateFormat),
            });
        }

        if (table.getRowCount() > 0) {
            display.printLine(Messages.getString("BuildPrinter.QueuedBuildsMessage")); //$NON-NLS-1$
            display.printLine(""); //$NON-NLS-1$

            table.print(display.getPrintStream());
        }

        return table.getRowCount();
    }

}
