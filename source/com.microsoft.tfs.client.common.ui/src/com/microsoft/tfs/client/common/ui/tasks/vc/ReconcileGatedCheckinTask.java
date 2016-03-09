// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.FindReconcilablePendingChangesCommand;
import com.microsoft.tfs.client.common.commands.vc.GetChangesetCommand;
import com.microsoft.tfs.client.common.commands.vc.GetCommand;
import com.microsoft.tfs.client.common.commands.vc.GetPendingChangesCommand;
import com.microsoft.tfs.client.common.commands.vc.UndoCommand;
import com.microsoft.tfs.client.common.commands.wit.GetWorkItemsForChangesetCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.RepositoryChangeItemProvider;
import com.microsoft.tfs.client.common.ui.dialogs.vc.UndoPendingChangesFromChangesetDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesViewModel;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildInformation;
import com.microsoft.tfs.core.clients.build.InformationNodeConverters;
import com.microsoft.tfs.core.clients.build.flags.InformationTypes;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.util.Check;

/**
 * Examines a changeset produced by a completed, successful gated checkin and
 * undoes local pending changes which are are duplicates of items in the
 * changeset. Presents a dialog so the user can choose which items to reconcile.
 *
 * {@link #run()} always returns an error status if a sub command return non-OK,
 * cancel status if the user canceled the dialog or the operation via progress
 * monitor, warning status if there were conflicts during the get, OK status
 * otherwise (the reconcile is considered complete and doesn't need to be
 * restarted from some UI part).
 *
 * The {@link IBuildDetail} passed during construction may be refreshed
 * (modified) by the task to retrieve build information needed to complete the
 * task.
 *
 * @threadsafety unknown
 */
public class ReconcileGatedCheckinTask extends BaseTask {
    public static final CodeMarker CODEMARKER_RECONCILETASK_COMPLETED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.tasks.vc.ReconcileGatedCheckinTask#completed"); //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(ReconcileGatedCheckinTask.class);
    private final TFSRepository repository;

    private IBuildDetail buildDetail;
    private int changesetID = -1;

    public ReconcileGatedCheckinTask(final Shell shell, final TFSRepository repository, final int changesetID) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.changesetID = changesetID;
    }

    /**
     * Constructs a {@link ReconcileGatedCheckinTask} for a build detail (the
     * changeset is read from the build information nodes). The
     * {@link IBuildDetail} may be refreshed (modified).
     */
    public ReconcileGatedCheckinTask(
        final Shell shell,
        final TFSRepository repository,
        final IBuildDetail buildDetail) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(buildDetail, "buildDetail"); //$NON-NLS-1$

        this.repository = repository;
        this.buildDetail = buildDetail;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link IStatus#OK} if the changes were reconciled successfully, a
     *         non-OK status if there was an error querying for build
     *         information or the status could not be reconciled (the task
     *         displays the error to the user),
     */
    @Override
    public IStatus run() {
        /*
         * Get the changeset from the build detail if required.
         */
        if (buildDetail != null) {
            changesetID = getChangesetIDFromBuildInformation();
        }

        /*
         * Callers should test changesetID, but fail early if they neglected to.
         */
        if (changesetID < 0) {
            final Status status = new Status(
                IStatus.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("ReconcileGatedCheckinTask.BuildDoesNotContainChangeset"), //$NON-NLS-1$
                null);

            ErrorDialog.openError(
                getShell(),
                Messages.getString("ReconcileGatedCheckinTask.NoChangesToReconcileTitle"), //$NON-NLS-1$
                null,
                status);

            return status;
        }

        /*
         * Unusual but possible: the gated build's changes were all undone by
         * the server.
         */
        else if (changesetID == 0) {
            CodeMarkerDispatch.dispatch(CODEMARKER_RECONCILETASK_COMPLETED);
            return Status.OK_STATUS;
        }

        /*
         * Get the current pending changes.
         */

        final GetPendingChangesCommand getPendingChangesCommand = new GetPendingChangesCommand(repository);
        IStatus status = getCommandExecutor().execute(getPendingChangesCommand);

        if (status.isOK() == false) {
            return status;
        }

        final PendingChange[] allPendingChanges = getPendingChangesCommand.getPendingChanges();
        if (allPendingChanges == null || allPendingChanges.length == 0) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("ReconcileGatedCheckinTask.NoChangesToReconcileTitle"), //$NON-NLS-1$
                Messages.getString("ReconcileGatedCheckinTask.ThereAreNoLocalPendingChangesToReconcile")); //$NON-NLS-1$

            return Status.OK_STATUS;
        }

        /*
         * Get the commited changeset info.
         */

        final GetChangesetCommand getChangesetCommand = new GetChangesetCommand(repository, changesetID);
        status = getCommandExecutor().execute(getChangesetCommand);

        if (status.isOK() == false) {
            return status;
        }

        final Changeset changeset = getChangesetCommand.getChangeset();

        /*
         * Filter the changes by the changeset.
         */

        final FindReconcilablePendingChangesCommand findCommand =
            new FindReconcilablePendingChangesCommand(repository, allPendingChanges, changeset);

        status = getCommandExecutor().execute(findCommand);

        if (status.isOK() == false) {
            return status;
        }

        if (findCommand.matchedAtLeastOnePendingChange() == false) {
            /*
             * Not even one pending change matched by path.
             */
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("ReconcileGatedCheckinTask.NoChangesToReconcileTitle"), //$NON-NLS-1$
                Messages.getString("ReconcileGatedCheckinTask.NoPendingChangesFoundInChangeset")); //$NON-NLS-1$

            CodeMarkerDispatch.dispatch(CODEMARKER_RECONCILETASK_COMPLETED);
            return Status.OK_STATUS;
        }

        final PendingChange[] reconcilablePendingChanges = findCommand.getReconcilablePendingChanges();

        if (reconcilablePendingChanges.length == 0) {
            /*
             * At least one path may have appeared to match, but after querying
             * for rename info and testing for things like encodings, it turns
             * out there's nothing we can automatically reconcile.
             */
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("ReconcileGatedCheckinTask.NoChangesToReconcileTitle"), //$NON-NLS-1$
                Messages.getString("ReconcileGatedCheckinTask.NoChangesCanBeAutomaticallyReconciled")); //$NON-NLS-1$

            return Status.OK_STATUS;
        }

        /*
         * Open the dialog so the user can select items to reconcile.
         */

        final ChangeItem[] changeItems =
            RepositoryChangeItemProvider.getChangeItemsFromPendingChanges(repository, reconcilablePendingChanges);

        final UndoPendingChangesFromChangesetDialog dialog =
            new UndoPendingChangesFromChangesetDialog(getShell(), changeItems, changeset.getChangesetID());

        if (dialog.open() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
        }

        final PendingChange[] undoPendingChanges = dialog.getCheckedPendingChanges();

        /*
         * Get the local items which are not null.
         */
        final String[] localItems = PendingChange.toLocalItems(undoPendingChanges);

        /*
         * Create item specs for undo.
         */
        final ItemSpec[] itemSpecs = new ItemSpec[localItems.length];
        for (int i = 0; i < localItems.length; i++) {
            itemSpecs[i] = new ItemSpec(localItems[i], RecursionType.NONE);
        }

        /*
         * Get a copy of the check-in notes before the undo.
         */
        final PendingChangesViewModel model = TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel();
        final CheckinNote noteBeforeUndo = model.getCheckinNote();

        /*
         * Do not update the local disk when we undo these files. We'll do an
         * explicit get later to overwrite them.
         */
        final UndoCommand undoCommand = new UndoCommand(repository, itemSpecs, GetOptions.NO_DISK_UPDATE);
        status = getCommandExecutor().execute(undoCommand);

        if (status.isOK() == false) {
            // Scan the undone items to ensure they don't remain as candidates.
            scanItems(localItems);
            return status;
        }

        /*
         * Build the get requests for the specific changeset. This "optimizing"
         * method generates for parent items with some recursion to minimize the
         * request data sent to the server. This widening of scope (through
         * limited recursion) has the happy effect of covering both sides of a
         * rename of an item that didn't move directories.
         */
        final GetRequest[] getRequests = GetRequest.createOptimizedRequests(
            repository.getVersionControlClient(),
            localItems,
            new ChangesetVersionSpec(changeset.getChangesetID()));

        final GetCommand getCommand = new GetCommand(repository, getRequests, GetOptions.OVERWRITE);

        /*
         * Turn on a custom command finished callback (and restore the old one
         * after execution). See GetTask.
         */
        final ICommandFinishedCallback previousCallback = getCommandExecutor().getCommandFinishedCallback();

        getCommandExecutor().setCommandFinishedCallback(
            UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        status = getCommandExecutor().execute(new ResourceChangingCommand(getCommand));

        getCommandExecutor().setCommandFinishedCallback(previousCallback);

        // Now that we have local version info, scan all the items so they don't
        // hang around as candidates.
        scanItems(localItems);

        /*
         * Get status handling for conflicts is similar to GetTask.
         */
        if (status.isOK() == false && getCommand.hasConflicts()) {
            final ConflictDescription[] conflicts = getCommand.getConflictDescriptions();

            final ConflictResolutionTask conflictTask = new ConflictResolutionTask(getShell(), repository, conflicts);
            final IStatus conflictStatus = conflictTask.run();

            return conflictStatus;
        } else if (status.isOK() == false) {
            ErrorDialog.openError(
                getShell(),
                Messages.getString("ReconcileGatedCheckinTask.CouldNotGetFiles"), //$NON-NLS-1$
                null,
                status);

            /*
             * Should be a warning status.
             */
            return status;
        }

        /*
         * Reconcile the checkin comment, notes, etc.
         */
        final GetWorkItemsForChangesetCommand getWorkItemsCommand =
            new GetWorkItemsForChangesetCommand(repository, changeset.getChangesetID());

        status = getCommandExecutor().execute(new ResourceChangingCommand(getWorkItemsCommand));

        // VS treats a failure to fetch associated IDs as an empty list
        WorkItem[] associatedWorkItems = new WorkItem[0];
        if (status.isOK() && getWorkItemsCommand.getWorkItems() != null) {
            associatedWorkItems = getWorkItemsCommand.getWorkItems();
        }

        // Update Team Explorer
        PendingChangesHelpers.afterReconcileGatedCheckin(changeset, associatedWorkItems, noteBeforeUndo);

        CodeMarkerDispatch.dispatch(CODEMARKER_RECONCILETASK_COMPLETED);
        return Status.OK_STATUS;
    }

    /**
     * Marks the specified paths as changed in the current
     * {@link WorkspaceWatcher}.
     * <p>
     * Normally core methods that pend changes or process get operations will do
     * this automatically, but we use unconventional flag sets (like
     * "no disk update") and must do the work ourselves.
     * <p>
     * Does nothing in a server workspace.
     *
     * @param localItems
     *        the items to mark changed (may be <code>null</code>)
     */
    private void scanItems(final String[] paths) {
        if (paths == null || paths.length == 0 || repository.getWorkspace().getLocation() != WorkspaceLocation.LOCAL) {
            return;
        }

        repository.getPathWatcherManager().notifyWatchers(Arrays.asList(paths));
    }

    /**
     * Gets the changeset ID, possibly refreshing the {@link IBuildDetail}.
     *
     * @return the changeset ID in {@link #buildDetail} (must not be
     *         <code>null</code>), 0 if there were no changes to check in, -1 if
     *         no changeset was created by this build
     */
    private int getChangesetIDFromBuildInformation() {
        Check.notNull(buildDetail, "this.buildDetail"); //$NON-NLS-1$

        if (buildDetail.getInformation() == null
            || buildDetail.getInformation().getNodesByType(InformationTypes.CHECK_IN_OUTCOME).length == 0) {
            buildDetail.refresh(new String[] {
                InformationTypes.CHECK_IN_OUTCOME
            }, QueryOptions.ALL);
        }

        final IBuildInformation information = buildDetail.getInformation();
        if (information != null) {
            return InformationNodeConverters.getChangesetID(information);
        }

        return -1;
    }
}
