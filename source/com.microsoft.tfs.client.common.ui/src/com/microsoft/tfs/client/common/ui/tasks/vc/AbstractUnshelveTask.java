// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.commands.vc.UnshelveCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public abstract class AbstractUnshelveTask extends BaseTask {
    public static final CodeMarker CODEMARKER_UNSHELVE_FINISHED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.tasks.vc.AbstractUnshelveTask#unshelveCompleted"); //$NON-NLS-1$

    private final static Log log = LogFactory.getLog(AbstractUnshelveTask.class);

    private final TFSRepository repository;

    public AbstractUnshelveTask(final Shell shell, final TFSRepository repository) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
    }

    /**
     * Unshelves the given shelveset
     *
     * @param shelveset
     *        The {@link Shelveset} to unshelve
     * @param itemSpecs
     *        An array of {@link ItemSpec}s from the shelveset that should be
     *        unshelved, or null to unshelve the entire shelveset
     * @param deleteShelveset
     *        true to delete the shelveset from the server after unshelving,
     *        false to leave the shelveset on the server
     * @param restoreData
     *        true to restore comment, work item associations and check-in notes
     *        from the shelveset, false to leave the local pending change(s)
     *        unmodified
     * @return The status of the unshelve execution
     */
    protected final IStatus unshelve(
        Shelveset shelveset,
        final ItemSpec[] itemSpecs,
        final boolean deleteShelveset,
        final boolean restoreData,
        final boolean autoResolveConflicts) {
        final UnshelveCommand unshelveCommand =
            new UnshelveCommand(repository, shelveset, itemSpecs, deleteShelveset, autoResolveConflicts);

        final ICommandExecutor commandExecutor = getCommandExecutor();
        commandExecutor.setCommandFinishedCallback(UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        final IStatus unshelveStatus = getCommandExecutor().execute(new ResourceChangingCommand(unshelveCommand));

        if (!unshelveStatus.isOK()) {
            String title;
            final String shelveName = shelveset.getName();

            if (unshelveStatus.getSeverity() == IStatus.ERROR) {
                final String titleFormat = Messages.getString("AbstractUnshelveTask.ErrorUnshelvingFormat"); //$NON-NLS-1$
                title = MessageFormat.format(titleFormat, shelveName);
            } else {
                final String titleFormat = Messages.getString("AbstractUnshelveTask.WarningUnshelvingFormat"); //$NON-NLS-1$
                title = MessageFormat.format(titleFormat, shelveName);
            }

            ErrorDialog.openError(getShell(), title, null, unshelveStatus);

            if (unshelveCommand.hasUnresolvedConflicts()) {
                final ConflictResolutionTask conflictTask = new ConflictResolutionTask(getShell(), repository, null);
                conflictTask.run();
            }

            return unshelveStatus;
        }

        shelveset = unshelveCommand.getShelveset();
        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$

        final RefreshPendingChangesCommand refreshPendingChangesCommand = new RefreshPendingChangesCommand(repository);
        getCommandExecutor().execute(refreshPendingChangesCommand);

        if (unshelveCommand.hasConflicts()) {
            final ConflictDescription[] conflicts = unshelveCommand.getConflictDescriptions();

            final ConflictResolutionTask conflictTask = new ConflictResolutionTask(getShell(), repository, conflicts);
            conflictTask.run();
        }

        if (restoreData) {
            PendingChangesHelpers.afterUnshelve(shelveset);
        }

        CodeMarkerDispatch.dispatch(CODEMARKER_UNSHELVE_FINISHED);

        return Status.OK_STATUS;
    }
}
