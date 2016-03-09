// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.printers;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.clients.versioncontrol.MergeSummary;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictCategory;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescriptionFactory;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.util.Check;

/**
 * Prints conflict details.
 */
public final class ConflictPrinter {
    /**
     * Prints the {@link Conflict}'s information and returns the display path
     * chosen for the conflict.
     */
    public static String printConflict(final Conflict conflict, final Display display, final boolean useErrorStream) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$

        String displayPath = conflict.getTargetLocalItem();

        if (displayPath != null) {
            displayPath = LocalPath.makeRelative(displayPath, LocalPath.getCurrentWorkingDirectory());
        } else if (conflict.getSourceLocalItem() != null) {
            displayPath = LocalPath.makeRelative(conflict.getSourceLocalItem(), LocalPath.getCurrentWorkingDirectory());
        } else {
            displayPath = conflict.getYourServerItem();
        }

        if (displayPath.length() == 0) {
            displayPath = LocalPath.getCurrentWorkingDirectory();
        }

        final ConflictDescription conflictDescription =
            ConflictDescriptionFactory.getConflictDescription(ConflictCategory.getConflictCategory(conflict), conflict);
        final String line = displayPath + ": " + conflictDescription.getDescription(); //$NON-NLS-1$
        if (useErrorStream) {
            display.printErrorLine(line);
        } else {
            display.printErrorLine(line);
        }

        if (conflict.getContentMergeSummary() != null) {
            final String summaryLine = getContentMergeSummaryLine(displayPath, conflict.getContentMergeSummary());

            if (useErrorStream) {
                display.printErrorLine(summaryLine);
            } else {
                display.printErrorLine(summaryLine);
            }
        }

        return displayPath;
    }

    public static String getContentMergeSummaryLine(final String path, final MergeSummary mergeSummary) {
        return MessageFormat.format(
            Messages.getString("ConflictPrinter.MergeSummaryFormat"), //$NON-NLS-1$
            path,
            mergeSummary.getLocalChangedLines(),
            mergeSummary.getLatestChangedLines(),
            mergeSummary.getCommonChangedLines(),
            mergeSummary.getTotalConflictingLines());
    }
}
