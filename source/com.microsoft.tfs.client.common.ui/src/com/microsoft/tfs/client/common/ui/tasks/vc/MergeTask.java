// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.MergeCommand;
import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.ui.wizard.merge.MergeWizard;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.util.Check;

public class MergeTask extends BaseTask {
    private final TFSRepository repository;
    private final String sourcePath;

    public static final CodeMarker CODEMARKER_MERGE_TASK_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.tasks.vc.MergeTask#mergeTaskComplete"); //$NON-NLS-1$

    public MergeTask(final Shell shell, final TFSRepository repository, final String sourcePath) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(sourcePath, "sourcePath"); //$NON-NLS-1$

        this.repository = repository;
        this.sourcePath = sourcePath;
    }

    @Override
    public IStatus run() {

        final MergeWizard mergeWizard = new MergeWizard(repository, sourcePath);
        final WizardDialog mergeDialog = new WizardDialog(getShell(), mergeWizard);

        mergeWizard.setNeedsProgressMonitor(true);

        if (mergeDialog.open() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
        }

        MergeFlags mergeFlags = mergeWizard.getMergeFlags();

        /* Mix-in the "no auto resolve" preference, if set. */
        if (!TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
            UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS)) {
            mergeFlags = mergeFlags.combine(MergeFlags.NO_AUTO_RESOLVE);
        }

        final MergeCommand mergeCommand = new MergeCommand(
            repository,
            mergeWizard.getSourcePath(),
            mergeWizard.getTargetPath(),
            mergeWizard.getFromVersion(),
            mergeWizard.getToVersion(),
            LockLevel.UNCHANGED,
            RecursionType.FULL,
            mergeFlags);

        final IStatus mergeStatus = getCommandExecutor().execute(new ResourceChangingCommand(mergeCommand));

        try {
            if (mergeStatus.getSeverity() == IStatus.ERROR) {
                return mergeStatus;
            }

            if (mergeCommand.hasConflicts()) {
                final ConflictDescription[] conflicts = mergeCommand.getConflictDescriptions();

                final ConflictResolutionTask conflictTask =
                    new ConflictResolutionTask(getShell(), repository, conflicts);
                conflictTask.run();
            } else if (!mergeStatus.isOK()) {
                if (mergeStatus.getSeverity() == IStatus.INFO) {
                    MessageDialog.openInformation(
                        getShell(),
                        Messages.getString("MergeTask.NoChangesToMergeDialogTitle"), //$NON-NLS-1$
                        mergeStatus.getMessage());

                    return Status.OK_STATUS;
                }
                return mergeStatus;
            }

            return mergeStatus;
        } finally {
            final ICommand refreshCommand = new RefreshPendingChangesCommand(repository);
            getCommandExecutor().execute(refreshCommand);

            CodeMarkerDispatch.dispatch(CODEMARKER_MERGE_TASK_COMPLETE);
        }
    }
}
