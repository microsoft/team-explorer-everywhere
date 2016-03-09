// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.printers;

import java.text.DateFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.MergeCandidate;
import com.microsoft.tfs.util.Check;

/**
 * Prints merge candidates.
 */
public final class MergeCandidatePrinter {

    public static int printBriefMergeCandidates(
        final MergeCandidate[] candidates,
        final DateFormat dateFormat,
        final Display display) {
        Check.notNull(candidates, "candidates"); //$NON-NLS-1$
        Check.notNull(dateFormat, "dateFormat"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$

        final TextOutputTable table = new TextOutputTable(display.getWidth());

        table.setColumns(new Column[] {
            new Column(Messages.getString("MergeCandidatePrinter.Changeset"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("MergeCandidatePrinter.Author"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("MergeCandidatePrinter.Date"), Sizing.TIGHT) //$NON-NLS-1$
        });

        for (int i = 0; i < candidates.length; i++) {
            final MergeCandidate candidate = candidates[i];
            Check.notNull(candidate, "candidate"); //$NON-NLS-1$

            final String changesetString =
                candidate.getChangeset().getChangesetID() + (candidate.isPartial() ? "*" : " "); //$NON-NLS-1$ //$NON-NLS-2$

            table.addRow(new String[] {
                changesetString,
                candidate.getChangeset().getOwnerDisplayName(),
                dateFormat.format(candidate.getChangeset().getDate().getTime())
            });
        }

        table.print(display.getPrintStream());

        return candidates.length;
    }
}
