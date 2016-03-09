// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.UndoCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.RepositoryChangeItemProvider;
import com.microsoft.tfs.client.common.ui.dialogs.vc.UndoPendingChangesDialog;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public class UndoPendingChangesTask extends BaseTask {
    public static final CodeMarker UNDO_PC_TASK_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.tasks.vc.UndoPendingChangesTask#undoPCTaskComplete"); //$NON-NLS-1$

    private final TFSRepository repository;
    private final PendingChange[] pendingChanges;
    private final ItemSpec[] itemSpecs;

    public UndoPendingChangesTask(
        final Shell shell,
        final TFSRepository repository,
        final PendingChange[] pendingChanges) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

        this.repository = repository;
        this.pendingChanges = pendingChanges;
        itemSpecs = null;
    }

    public UndoPendingChangesTask(final Shell shell, final TFSRepository repository, final ItemSpec[] itemSpecs) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        this.repository = repository;
        pendingChanges = null;
        this.itemSpecs = itemSpecs;
    }

    @Override
    public IStatus run() {
        final ItemSpec[] undoItemSpec;

        if (pendingChanges != null && itemSpecs == null) {
            final ChangeItem[] pendingChangeItems =
                RepositoryChangeItemProvider.getChangeItemsFromPendingChanges(repository, pendingChanges);
            final UndoPendingChangesDialog undoDialog = new UndoPendingChangesDialog(getShell(), pendingChangeItems);

            if (undoDialog.open() != IDialogConstants.OK_ID) {
                return Status.OK_STATUS;
            }

            final PendingChange[] undoChanges = undoDialog.getCheckedPendingChanges();
            undoItemSpec = new ItemSpec[undoChanges.length];

            for (int i = 0; i < undoChanges.length; i++) {
                undoItemSpec[i] = new ItemSpec(undoChanges[i].getServerItem(), RecursionType.NONE);
            }
        } else if (pendingChanges == null && itemSpecs != null) {
            undoItemSpec = itemSpecs;
        } else {
            throw new NullPointerException("pendingChanges == null && itemSpecs == null"); //$NON-NLS-1$
        }

        final UndoCommand undoCommand = new UndoCommand(repository, undoItemSpec);
        final IStatus undoStatus = getCommandExecutor().execute(new ResourceChangingCommand(undoCommand));

        if (!undoStatus.isOK() && undoCommand.hasConflicts()) {
            final ConflictDescription[] conflicts = undoCommand.getConflictDescriptions();

            final ConflictResolutionTask conflictTask = new ConflictResolutionTask(getShell(), repository, conflicts);
            final IStatus conflictStatus = conflictTask.run();

            if (conflictStatus.isOK()) {
                return Status.OK_STATUS;
            }
        }

        CodeMarkerDispatch.dispatch(UNDO_PC_TASK_COMPLETE);

        return undoStatus;
    }
}
