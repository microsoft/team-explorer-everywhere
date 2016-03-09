// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.GetChangesetCommand;
import com.microsoft.tfs.client.common.commands.wit.GetWorkItemsForChangesetCommand;
import com.microsoft.tfs.client.common.framework.command.CommandList;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.wit.WorkItemCheckinTable;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ChangesetDetailsDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.Check;

/**
 * Queries changeset information from the server and displays it in the details
 * view or dialog. If you already have a {@link Changeset} object handy, it's
 * probably easier just to use {@link ChangesetDetailsDialog} directly.
 *
 * {@link #run()} records whether the {@link Changeset} was modified in the
 * dialog. This can be queried with {@link #wasChangesetUpdated()}.
 *
 * @threadsafety unknown
 */
public class ViewChangesetDetailsTask extends BaseTask {
    private final TFSRepository repository;
    private final int changesetID;

    private boolean wasChangesetUpdated;

    public ViewChangesetDetailsTask(final Shell shell, final TFSRepository repository, final int changesetID) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.changesetID = changesetID;
    }

    @Override
    public IStatus run() {
        wasChangesetUpdated = false;

        final GetChangesetCommand changesetCommand = new GetChangesetCommand(repository, changesetID);
        final GetWorkItemsForChangesetCommand witCommand = new GetWorkItemsForChangesetCommand(repository, changesetID);
        witCommand.setExtraFieldNames(WorkItemCheckinTable.EXTRA_FIELDS);

        final CommandList compositeCommand = new CommandList(
            MessageFormat.format(
                Messages.getString("ViewChangesetDetailsTask.CommandTextFormat"), //$NON-NLS-1$
                Integer.toString(changesetID)),
            MessageFormat.format(
                Messages.getString("ViewChangesetDetailsTask.CommandErrorTextFormat"), //$NON-NLS-1$
                Integer.toString(changesetID)));

        compositeCommand.addCommand(changesetCommand);
        compositeCommand.addCommand(witCommand);

        final IStatus status = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(compositeCommand);

        if (!status.isOK()) {
            return status;
        }

        final Changeset fullChangeset = changesetCommand.getChangeset();
        fullChangeset.setWorkItems(witCommand.getWorkItems());

        final ChangesetDetailsDialog dialog = new ChangesetDetailsDialog(getShell(), fullChangeset, repository);
        dialog.open();

        wasChangesetUpdated = dialog.wasChangesetUpdated();

        return Status.OK_STATUS;
    }

    /**
     * @return <code>true</code> if the {@link Changeset} was updated in the
     *         dialog when it was viewed with {@link #run()} (the caller may
     *         wish to refresh the display of the changeset), <code>false</code>
     *         if the {@link Changeset} was not modified
     */
    public boolean wasChangesetUpdated() {
        return wasChangesetUpdated;
    }
}
