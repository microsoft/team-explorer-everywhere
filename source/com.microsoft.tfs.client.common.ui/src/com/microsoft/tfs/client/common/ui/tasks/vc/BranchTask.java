// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.BranchBranchCommand;
import com.microsoft.tfs.client.common.commands.vc.BranchCommand;
import com.microsoft.tfs.client.common.commands.vc.CalculateDefaultBranchPathCommand;
import com.microsoft.tfs.client.common.commands.vc.ConvertFolderToBranchCommand;
import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.BranchBranchDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.BranchDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemFactory;
import com.microsoft.tfs.util.Check;

public class BranchTask extends BaseTask {
    private final TFSRepository repository;
    private final TFSItem branchFromItem;

    public static final CodeMarker BRANCH_TASK_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.tasks.vc.BranchTask#branchTaskComplete"); //$NON-NLS-1$

    public BranchTask(final Shell shell, final TFSRepository repository, final TFSItem branchFromItem) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(branchFromItem, "branchFromPath"); //$NON-NLS-1$

        this.repository = repository;
        this.branchFromItem = branchFromItem;
    }

    protected TFSRepository getRepository() {
        return repository;
    }

    @Override
    public final IStatus run() {
        final CalculateDefaultBranchPathCommand calculatePathCommand =
            new CalculateDefaultBranchPathCommand(repository, branchFromItem.getFullPath());

        final IStatus calculatePathStatus =
            UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(calculatePathCommand);

        if (!calculatePathStatus.isOK()) {
            return Status.OK_STATUS;
        }

        if (branchFromItem.getExtendedItem().isBranch()) {
            final BranchBranchDialog branchDialog = new BranchBranchDialog(
                getShell(),
                repository,
                branchFromItem.getFullPath(),
                calculatePathCommand.getDefaultBranchPath());

            if (branchDialog.open() != IDialogConstants.OK_ID) {
                return Status.OK_STATUS;
            }

            final BranchBranchCommand branchCommand = new BranchBranchCommand(
                repository,
                branchDialog.getBranchFromPath(),
                branchDialog.getBranchToPath(),
                branchDialog.getVersionSpec(),
                branchDialog.getDescription());

            // No refresh needed because branching from a branch never creates
            // pending changes
            return UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(
                new ResourceChangingCommand(branchCommand));
        }

        final BranchDialog branchDialog = new BranchDialog(
            getShell(),
            repository,
            branchFromItem.getFullPath(),
            calculatePathCommand.getDefaultBranchPath(),
            branchFromItem instanceof TFSFolder);

        if (branchDialog.open() != IDialogConstants.OK_ID) {
            return Status.OK_STATUS;
        }

        final BranchCommand branchCommand = new BranchCommand(
            repository,
            branchDialog.getBranchFromPath(),
            branchDialog.getBranchToPath(),
            branchDialog.getVersionSpec(),
            branchDialog.getGetOptions());

        final IStatus status = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(
            new ResourceChangingCommand(branchCommand));

        /*
         * Comments from VS code (with small changes for this task):
         *
         * To have better support for branch (objects), the following algorithm
         * is applied to automatically convert the source folder to a branch
         * prior to branching it.
         *
         * 1. Only for a folder, has server support, has UI confirmation
         * (branchDialog.convertToBranch() is only true for folders)
         *
         * 2. If the source folder is already a branch, this code shouldn't be
         * executed (would not get this far in this task)
         *
         * 3. If none of the ancestor folders is a branch, convert the source
         * folder to a branch before branching it (actually done after the pend,
         * here and in VS code)
         *
         * 4. If there is an ancestor folder that is a branch, branch the source
         * folder like we do currently (already done pend branch by here, same
         * as VS)
         */
        if (status.isOK() && branchDialog.convertToBranch() && isBranchOrHasBranchAncestor(branchFromItem) == false) {
            final String username =
                repository.getWorkspace().getClient().getConnection().getAuthorizedTFSUser().toString();

            final ConvertFolderToBranchCommand convertCommand =
                new ConvertFolderToBranchCommand(
                    repository,
                    branchDialog.getBranchFromPath(),
                    username,
                    "", //$NON-NLS-1$
                    false);

            UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(
                new ResourceChangingCommand(convertCommand));
        }

        refreshPendingChanges();

        CodeMarkerDispatch.dispatch(BRANCH_TASK_COMPLETE);

        return status;
    }

    /**
     * This method is inefficient (does server round-trips) because our folder
     * control model does not provide parentage discovery.
     *
     * @param item
     *        the {@link TFSItem} to inspect for branch status including
     *        ancestors (must not be <code>null</code>)
     * @return <code>true</code> if the given item is a branch or has any branch
     *         ancestors
     */
    private boolean isBranchOrHasBranchAncestor(TFSItem item) {
        Check.notNull(item, "item"); //$NON-NLS-1$

        while (item != null) {
            if (item.getExtendedItem() != null && item.getExtendedItem().isBranch()) {
                return true;
            }

            final String parentPath = item.getParentFullPath();
            if (parentPath == null) {
                item = null;
            } else {
                item = TFSItemFactory.getItemAtPath(getRepository(), item.getParentFullPath());
            }
        }

        return false;
    }

    private void refreshPendingChanges() {
        /*
         * Branching may create get operations, and those may get turned into
         * pending chages in our cache. Unfortunately source local item isn't in
         * the get operation information, so it can't make it into the pending
         * changes. Best to refresh the changes list so we don't miss
         * information (which may cause UI problems later).
         */

        final ICommand refreshCommand = new RefreshPendingChangesCommand(repository);
        getCommandExecutor().execute(refreshCommand);
    }
}
