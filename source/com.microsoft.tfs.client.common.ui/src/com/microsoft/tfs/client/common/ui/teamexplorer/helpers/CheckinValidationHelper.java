// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.EvaluatePendingCheckinCommand;
import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl.ValidationResult;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.PendingCheckinSaveableFilter;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.helpers.EditorHelper;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.pendingcheckin.CheckinConflict;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationOptions;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationResult;
import com.microsoft.tfs.core.pendingcheckin.CheckinNoteFailure;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;

public class CheckinValidationHelper {
    public static ValidationResult validateForCheckin(
        final Shell shell,
        final TFSRepository repository,
        final PendingCheckin pendingCheckin) {
        Check.notNull(pendingCheckin, "pendingCheckin"); //$NON-NLS-1$

        // Ensure all dirty files which are part of this check-in are saved.
        final PendingCheckinSaveableFilter saveFilter = new PendingCheckinSaveableFilter(pendingCheckin);
        if (EditorHelper.saveAllDirtyEditors(saveFilter) == false) {
            return new ValidationResult(false, null, null, null);
        }

        /*
         * Use core's evaluation (via Workspace), run as a command.
         */
        final EvaluatePendingCheckinCommand command = new EvaluatePendingCheckinCommand(
            repository.getWorkspace(),
            CheckinEvaluationOptions.ALL.remove(CheckinEvaluationOptions.POLICIES),
            pendingCheckin,
            new PolicyContext());

        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(shell);

        /*
         * Non-OK status means the user cancelled some part of evaluation.
         */
        if (executor.execute(command) != Status.OK_STATUS) {
            return new ValidationResult(false, null, null, null);
        }

        /*
         * Get the result of the evaluation.
         */
        final CheckinEvaluationResult evaluationResult = command.getEvaluationResult();

        /*
         * See if checkin conflicts contain an error from the server (from the
         * checkPendingChanges SOAP call)
         */
        if (validateCheckinConflicts(shell, repository, evaluationResult) == false) {
            return new ValidationResult(false, null, null, evaluationResult.getConflicts());
        }

        /*
         * See if checkin notes evaluated correctly. If there were failures,
         * return the
         */
        if (validateEvaluationResultNotes(shell, evaluationResult) == false) {
            return new ValidationResult(false, null, null, evaluationResult.getConflicts());
        }

        /*
         * No failures.
         */
        return new ValidationResult(true, null, null, evaluationResult.getConflicts());
    }

    /**
     * Validates just the checkin conflicts that were raised by the server in
     * the checkPendingChanges SOAP call, notifying the user of errors via
     * dialog.
     *
     * @param evaluationResult
     *        the core evaluation result (must not be <code>null</code>)
     * @return true if there were no checkin conflict problems, false if the
     *         server raised an error
     */
    private static boolean validateCheckinConflicts(
        final Shell shell,
        final TFSRepository repository,
        final CheckinEvaluationResult evaluationResult) {
        final CheckinConflict[] conflicts = evaluationResult.getConflicts();

        if (conflicts.length == 0) {
            return true;
        }

        /*
         * Search for an ITEM_NOT_CHECKED_OUT_EXCEPTION code. This is the only
         * error code we wish to validate. Other checkin exceptions should
         * properly be handled by the conflict resolution mechanism.
         */
        for (int i = 0; i < conflicts.length; i++) {
            if (VersionControlConstants.ITEM_NOT_CHECKED_OUT_EXCEPTION.equals(conflicts[i].getCode())) {
                final RefreshPendingChangesCommand refreshCommand = new RefreshPendingChangesCommand(repository);
                final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(shell);

                executor.execute(refreshCommand);

                MessageDialog.openError(
                    shell,
                    Messages.getString("CheckinControl.CheckinConflictsDialogTitle"), //$NON-NLS-1$
                    Messages.getString("CheckinControl.PendingChangesOutdated")); //$NON-NLS-1$

                return false;
            }
        }

        return true;
    }

    /**
     * Validates just the checkin note part of a {@link CheckinEvaluationResult}
     * notifying the user of missing notes via dialog. .
     *
     * @param evaluationResult
     *        the core evaluation result (must not be <code>null</code>)
     * @return true if there were no note problems, false if some required notes
     *         were missing
     */
    private static boolean validateEvaluationResultNotes(
        final Shell shell,
        final CheckinEvaluationResult evaluationResult) {
        final CheckinNoteFailure[] failures = evaluationResult.getNoteFailures();
        final IStatus[] failureStatus = new IStatus[failures.length];

        if (failures.length == 0) {
            return true;
        }

        for (int i = 0; i < failures.length; i++) {
            failureStatus[i] = new Status(
                Status.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(
                    Messages.getString("CheckinControl.CheckinNoteIsRequiredFormat"), //$NON-NLS-1$
                    failures[i].getDefinition().getName()),
                null);
        }

        IStatus displayStatus;
        if (failures.length == 1) {
            displayStatus = failureStatus[0];
        } else {
            displayStatus = new MultiStatus(
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                failureStatus,
                Messages.getString("CheckinControl.SomeCheckinNotesAreMissingRequiredValues"), //$NON-NLS-1$
                null);
        }

        ErrorDialog.openError(
            shell,
            Messages.getString("CheckinControl.CheckinNotesAreRequiredDialogTitle"), //$NON-NLS-1$
            null,
            displayStatus);

        return false;
    }
}
