// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.UndoCommand;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangesTable;
import com.microsoft.tfs.client.common.ui.dialogs.vc.UndoPendingChangesDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public class UndoUnchangedPendingChangesAction extends Action {
    private final ChangesTable changesTable;
    private final Shell shell;
    private TFSRepository repository;

    public UndoUnchangedPendingChangesAction(final ChangesTable changesTable, final Shell shell) {
        this(changesTable, null, shell);
    }

    public UndoUnchangedPendingChangesAction(
        final ChangesTable changesTable,
        final TFSRepository repository,
        final Shell shell) {
        this.changesTable = changesTable;
        this.repository = repository;
        this.shell = shell;

        changesTable.addElementListener(new ElementListener() {
            @Override
            public void elementsChanged(final ElementEvent event) {
                setEnabled(event.getElements().length > 0);
            }
        });

        setText(Messages.getString("UndoUnchangedPendingChangesAction.ActionText")); //$NON-NLS-1$
        setToolTipText(Messages.getString("UndoUnchangedPendingChangesAction.ActionTooltip")); //$NON-NLS-1$
        setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_UNDO));
        setEnabled(false);
    }

    public void setRepository(final TFSRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run() {
        final ChangeItem[] changeItems = changesTable.getChangeItems();
        undoUnchangedChanges(shell, repository, changeItems);
    }

    public static void undoUnchangedChanges(
        final Shell shell,
        final TFSRepository repository,
        final ChangeItem[] changes) {
        ChangeItem[] changeItems = changes;

        final FindUnchangedPendingChangesCommand examinePendingChangesCommand =
            new FindUnchangedPendingChangesCommand(changeItems);

        final ICommandExecutor commandExecutor = UICommandExecutorFactory.newUICommandExecutor(shell);

        final IStatus status = commandExecutor.execute(new ResourceChangingCommand(examinePendingChangesCommand));
        if (!status.isOK()) {
            return;
        }

        changeItems = examinePendingChangesCommand.getUnchangedPendingChanges();

        if (changeItems.length == 0) {
            MessageBoxHelpers.messageBox(
                shell,
                Messages.getString("UndoUnchangedPendingChangesAction.DialogTitle"), //$NON-NLS-1$
                Messages.getString("UndoUnchangedPendingChangesAction.AllChangesModifiedSinceCheckout")); //$NON-NLS-1$
            return;
        }

        final UndoPendingChangesDialog dialog = new UndoPendingChangesDialog(shell, changeItems);

        dialog.setLabelText(Messages.getString("UndoUnchangedPendingChangesAction.NoneChangesModifiedSinceCheckout")); //$NON-NLS-1$

        if (IDialogConstants.CANCEL_ID == dialog.open()) {
            return;
        }

        final PendingChange[] changesToUndo = dialog.getCheckedPendingChanges();

        final ItemSpec[] itemSpecs = new ItemSpec[changesToUndo.length];
        for (int i = 0; i < changesToUndo.length; i++) {
            itemSpecs[i] = new ItemSpec(changesToUndo[i].getServerItem(), RecursionType.NONE);
        }

        final ICommand undoCommand = new ResourceChangingCommand(new UndoCommand(repository, itemSpecs));
        commandExecutor.execute(undoCommand);
    }

    protected boolean computeEnablement(final IStructuredSelection selection) {
        return selection.size() > 0;
    }

    private static class FindUnchangedPendingChangesCommand extends TFSCommand {
        private final ChangeItem[] changeItems;
        private ChangeItem[] changedPendingChanges;
        private ChangeItem[] unchangedPendingChanges;

        public FindUnchangedPendingChangesCommand(final ChangeItem[] changeItems) {
            Check.notNull(changeItems, "changeItems"); //$NON-NLS-1$
            this.changeItems = changeItems;
        }

        @Override
        public boolean isCancellable() {
            return true;
        }

        @Override
        public String getName() {
            return (Messages.getString("UndoUnchangedPendingChangesAction.CommandText")); //$NON-NLS-1$
        }

        @Override
        public String getErrorDescription() {
            return (Messages.getString("UndoUnchangedPendingChangesAction.CommandErrorText")); //$NON-NLS-1$
        }

        @Override
        public String getLoggingDescription() {
            return (Messages.getString("UndoUnchangedPendingChangesAction.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
        }

        @Override
        protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
            progressMonitor.beginTask(
                Messages.getString("UndoUnchangedPendingChangesAction.ExaminingChangesProgressText"), //$NON-NLS-1$
                changeItems.length);
            final List<ChangeItem> changedList = new ArrayList<ChangeItem>();
            final List<ChangeItem> unchangedList = new ArrayList<ChangeItem>();

            for (int i = 0; i < changeItems.length; i++) {
                if (progressMonitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                try {
                    if (changeItems[i].isUnchanged()) {
                        unchangedList.add(changeItems[i]);
                    } else {
                        changedList.add(changeItems[i]);
                    }
                } catch (final CanceledException e) {
                    /*
                     * Thrown by core while it's calculating content changes if
                     * the user canceled.
                     */
                    return Status.CANCEL_STATUS;
                }

                progressMonitor.worked(1);
            }

            changedPendingChanges = changedList.toArray(new ChangeItem[changedList.size()]);
            unchangedPendingChanges = unchangedList.toArray(new ChangeItem[unchangedList.size()]);
            return null;
        }

        public ChangeItem[] getUnchangedPendingChanges() {
            return unchangedPendingChanges;
        }

    }
}
