// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.RepositoryAction;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.UndoPendingChangesDialog;
import com.microsoft.tfs.client.common.ui.helpers.UndoHelper;
import com.microsoft.tfs.client.common.ui.tasks.vc.UndoPendingChangesTask;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class UndoPendingChangesAction extends RepositoryAction {
    private final Shell shell;

    public UndoPendingChangesAction(final ISelectionProvider selectionProvider, final Shell shell) {
        this(selectionProvider, null, shell);
    }

    public UndoPendingChangesAction(
        final ISelectionProvider selectionProvider,
        final TFSRepository repository,
        final Shell shell) {
        super(selectionProvider, repository);

        this.shell = shell;

        setText(Messages.getString("UndoPendingChangesAction.ActionText")); //$NON-NLS-1$
        setToolTipText(Messages.getString("UndoPendingChangesAction.ActionTooltip")); //$NON-NLS-1$
        setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_UNDO));
    }

    @Override
    protected void doRun(final TFSRepository repository) {
        final ChangeItem[] changeitems = (ChangeItem[]) adaptSelectionToArray(ChangeItem.class);
        undoPendingChanges(shell, repository, changeitems);
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        return selection.size() > 0;
    }

    public static void undoPendingChanges(
        final Shell shell,
        final TFSRepository repository,
        final ChangeItem[] changeItems) {

        final UndoPendingChangesDialog dialog = new UndoPendingChangesDialog(shell, changeItems);

        if (IDialogConstants.CANCEL_ID == dialog.open()) {
            return;
        }

        PendingChange[] changesToUndo = dialog.getCheckedPendingChanges();
        changesToUndo = UndoHelper.filterChangesToUndo(changesToUndo, shell);
        if (changesToUndo == null || changesToUndo.length == 0) {
            return;
        }

        final ItemSpec[] itemSpecs = new ItemSpec[changesToUndo.length];
        for (int i = 0; i < changesToUndo.length; i++) {
            itemSpecs[i] = new ItemSpec(changesToUndo[i].getServerItem(), RecursionType.NONE);
        }

        final UndoPendingChangesTask undoTask = new UndoPendingChangesTask(shell, repository, itemSpecs);
        undoTask.run();
    }
}
