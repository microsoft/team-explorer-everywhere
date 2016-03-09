// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionStatus;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

public class ConflictHelpers {
    /**
     * Displays data about resolution error, if this resolution was in error.
     * Helper method for showConflictErrors()
     *
     * @param parent
     *        The parent shell to display on
     * @param resolutions
     *        The attempted conflict resolution
     * @param statuses
     *        The status from the resulotion
     */
    public static void showConflictError(
        final Shell parent,
        final ConflictResolution resolution,
        final ConflictResolutionStatus status) {
        showConflictErrors(parent, new ConflictResolution[] {
            resolution
        }, new ConflictResolutionStatus[] {
            status
        });
    }

    /**
     * Displays data about resolution errors (if any) in a modal error dialog.
     * If there are no errors determined in the inputs, does nothing.
     *
     * There must be a 1:1 mapping between resolutions and statuses.
     *
     * @param parent
     *        The parent shell to display on
     * @param resolutions
     *        All conflict resolutions which were attempted
     * @param statuses
     *        All status from the resolutions
     */
    public static void showConflictErrors(
        final Shell parent,
        final ConflictResolution[] resolutions,
        final ConflictResolutionStatus[] statuses) {
        /* Results which were SucceededWithConflicts. */
        int newConflictCount = 0;
        String conflictFile = null;
        String conflictMessage = null;

        /* Results which were resolution failures. */
        int errorCount = 0;
        String errorFile = null;
        String errorMessage = null;

        /* Walk the statuses, see if there were any errors */
        for (int i = 0; i < statuses.length; i++) {
            final String filename = (resolutions[i].getConflictDescription().getLocalPath() != null)
                ? LocalPath.getFileName(resolutions[i].getConflictDescription().getLocalPath())
                : Messages.getString("ConflictHelpers.UnknownFile"); //$NON-NLS-1$

            if (statuses[i] == ConflictResolutionStatus.SUCCEEDED_WITH_CONFLICTS) {
                newConflictCount += resolutions[i].getConflicts().length;
                conflictFile = filename;
                conflictMessage = resolutions[i].getErrorMessage();
            } else if (statuses[i] == ConflictResolutionStatus.FAILED) {
                errorCount++;
                errorFile = filename;
                errorMessage = resolutions[i].getErrorMessage();
            }
        }

        /* Notify of any failures */
        if (newConflictCount > 0 || errorCount > 0) {
            final StringBuffer message = new StringBuffer();

            // add messages about new conflicts
            if (newConflictCount > 0) {
                String error = ConflictResolution.DefaultErrorMessage;
                if (newConflictCount == 1 && conflictMessage != null) {
                    error = conflictMessage;
                }

                if (newConflictCount == 1) {
                    final String textFormat = Messages.getString("ConflictHelpers.SingleConflictFormat"); //$NON-NLS-1$
                    message.append(MessageFormat.format(textFormat, conflictFile, error));

                } else {
                    final String textFormat = Messages.getString("ConflictHelpers.MultiConflictFormat"); //$NON-NLS-1$
                    message.append(MessageFormat.format(textFormat, newConflictCount, error));

                }
            }

            if (newConflictCount > 0 && errorCount > 0) {
                message.append("\n\n"); //$NON-NLS-1$
            }

            if (errorCount > 0) {
                String error = ConflictResolution.DefaultErrorMessage;
                if (errorCount == 1 && errorMessage != null) {
                    error = errorMessage;
                }

                if (newConflictCount == 1) {
                    if (errorCount == 1) {
                        final String textFormat = Messages.getString("ConflictHelpers.SingleConflictFormat"); //$NON-NLS-1$
                        message.append(MessageFormat.format(textFormat, errorFile, error));
                    } else {
                        final String textFormat = Messages.getString("ConflictHelpers.SingleConflictMutliErrorFormat"); //$NON-NLS-1$
                        message.append(MessageFormat.format(textFormat, errorCount, error));
                    }
                } else {
                    if (errorCount == 1) {
                        final String textFormat = Messages.getString("ConflictHelpers.MultiConflictSingleErrorFormat"); //$NON-NLS-1$
                        message.append(MessageFormat.format(textFormat, errorFile, error));
                    } else {
                        final String textFormat = Messages.getString("ConflictHelpers.MultiConflictMultiErrorFormat"); //$NON-NLS-1$
                        message.append(MessageFormat.format(textFormat, errorCount, error));
                    }
                }
            }

            message.append("\n\n"); //$NON-NLS-1$
            if (newConflictCount + errorCount > 1) {
                message.append(Messages.getString("ConflictHelpers.SingleConflictResolve")); //$NON-NLS-1$
            } else {
                message.append(Messages.getString("ConflictHelpers.MultiConflictResolve")); //$NON-NLS-1$
            }

            final String title =
                (resolutions.length > 1) ? Messages.getString("ConflictHelpers.MultiConflictDialogTitle") //$NON-NLS-1$
                    : Messages.getString("ConflictHelpers.SingleConflictDialogTitle"); //$NON-NLS-1$

            MessageDialog.openError(parent, title, message.toString());
        }
    }
}
