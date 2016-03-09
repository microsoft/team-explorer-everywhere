// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.checkinpolicies.ExtensionPointPolicyLoader;
import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.CheckinCommand;
import com.microsoft.tfs.client.common.commands.vc.DeleteShelvesetsCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryShelvesetsCommand;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.commands.IQueueGatedCheckinBuild;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl.ValidationResult;
import com.microsoft.tfs.client.common.ui.dialogs.vc.CheckinDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.GatedCheckinDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.util.ExtensionLoader;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.GatedCheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpecParseException;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;

/**
 * Handles the checkin procedure, either by prompting the user for the changes
 * to check-in from the given {@link PendingChange}s, or by checking in a
 * specified {@link PendingCheckin}.
 *
 * @threadsafety unknown
 */
public class CheckinTask extends BaseTask {
    public static final CodeMarker CODEMARKER_GATED_CHECKIN_QUEUED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.tasks.vc.CheckinTask#GatedCheckinQueued"); //$NON-NLS-1$

    private static final String QUEUE_BUILD_EXTENSION_POINT_ID =
        "com.microsoft.tfs.client.common.ui.queueGatedCheckinBuild"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(ExtensionPointPolicyLoader.class);
    private final TFSRepository repository;

    private PendingChange[] pendingChanges;
    private String comment;
    private PendingCheckin pendingCheckin;
    private PolicyOverrideInfo policyOverrideInfo;
    private boolean pendingChangesCleared;

    private int changesetID;
    private IQueuedBuild queuedGatedBuild;

    /**
     * Creates a {@link CheckinTask}.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param pendingChanges
     *        the pending changes (must not be <code>null</code>)
     * @param comment
     *        the comment text (may be <code>null</code>)
     */
    public CheckinTask(
        final Shell shell,
        final TFSRepository repository,
        final PendingChange[] pendingChanges,
        final String comment) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

        this.repository = repository;
        this.pendingChanges = pendingChanges;
        this.comment = comment;
    }

    /**
     * Creates a {@link CheckinTask}.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param pendingCheckin
     *        the pending check-in (must not be <code>null</code>)
     * @param policyOverrideInfo
     *        the policy override information (may be <code>null</code>)
     */
    public CheckinTask(
        final Shell shell,
        final TFSRepository repository,
        final PendingCheckin pendingCheckin,
        final PolicyOverrideInfo policyOverrideInfo) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(pendingCheckin, "pendingCheckin"); //$NON-NLS-1$

        this.repository = repository;
        this.pendingCheckin = pendingCheckin;
        this.policyOverrideInfo = policyOverrideInfo;
    }

    @Override
    public IStatus run() {
        // Show the dialog if the caller did not already supply pending changes
        // to check-in.
        if (pendingCheckin == null) {
            final CheckinDialog checkinDialog = new CheckinDialog(getShell(), repository, pendingChanges, comment);

            if (checkinDialog.open() != IDialogConstants.OK_ID) {
                return Status.OK_STATUS;
            }

            /*
             * Get the checkin information from the dialog.
             */
            pendingCheckin = checkinDialog.getPendingCheckin();

            /*
             * The dialog did the validation before it allowed OK to close the
             * dialog.
             */
            final ValidationResult validationResult = checkinDialog.getValidationResult();
            Check.notNull(validationResult, "validationResult"); //$NON-NLS-1$
            Check.isTrue(validationResult.getSucceeded(), "validationResult.getSucceeded()"); //$NON-NLS-1$

            /*
             * Build the override information from the validation result.
             */
            if (validationResult.getPolicyOverrideReason() != null
                && validationResult.getPolicyOverrideReason().length() > 0) {
                policyOverrideInfo = new PolicyOverrideInfo(
                    validationResult.getPolicyOverrideReason(),
                    validationResult.getPolicyFailures());
            }
        }

        final CheckinCommand checkinCommand = new CheckinCommand(repository, pendingCheckin, policyOverrideInfo);
        checkinCommand.setAutoResolveConflicts(
            TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
                UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS));
        checkinCommand.setQueryConflicts(true);

        /*
         * Make sure to use a command executor that does not raise error dialogs
         * - we need to handle conflicts (IStatus.WARNING) specially.
         *
         * Wrap these standard callbacks with a custom command finished callback
         * to gracefully handle some standard checkin exceptions.
         */
        final ICommandExecutor commandExecutor = getCommandExecutor();
        commandExecutor.setCommandFinishedCallback(new CheckinCommandFinishedCallback());

        boolean retryWithOverride;
        IStatus checkinStatus;

        do {
            retryWithOverride = false;
            checkinStatus = commandExecutor.execute(new ResourceChangingCommand(checkinCommand));

            if (checkinStatus.isOK()) {
                changesetID = checkinCommand.getChangeset();

                if (changesetID == 0) {
                    MessageDialog.openWarning(
                        getShell(),
                        Messages.getString("PendingChangesView.NoChangesDialogTitle"), //$NON-NLS-1$
                        Messages.getString("PendingChangesView.ChangeChangesDialogText")); //$NON-NLS-1$
                }

                pendingChangesCleared = true;
                return checkinStatus;
            }

            if (checkinStatus.getException() instanceof GatedCheckinException) {
                // The server rejected the check-in attempt because some of the
                // pending changes affect build definition(s) which have gated
                // check-ins.
                final GatedCheckinException e = (GatedCheckinException) checkinStatus.getException();

                // Strip the owner from the shelveset name
                String shelvesetName;
                try {
                    final WorkspaceSpec spec = WorkspaceSpec.parse(e.getShelvesetName(), null);
                    shelvesetName = spec.getName();
                } catch (final WorkspaceSpecParseException ex) {
                    shelvesetName = e.getShelvesetName();
                    log.warn("The shelveset name '" + shelvesetName + "' could not be parsed", e); //$NON-NLS-1$ //$NON-NLS-2$
                }

                // Determine if any changes had a lock.
                boolean allowKeepCheckedOut = true;

                for (final PendingChange pc : pendingCheckin.getPendingChanges().getCheckedPendingChanges()) {
                    if (pc.isLock()) {
                        allowKeepCheckedOut = false;
                        break;
                    }
                }

                // Display the gated check-in dialog to the user.
                final GatedCheckinDialog dialog = new GatedCheckinDialog(
                    getShell(),
                    shelvesetName,
                    e.getAffectedBuildDefinitionNames(),
                    e.getAffectedBuildDefinitionURIs(),
                    e.getOverridePermission(),
                    allowKeepCheckedOut);

                if (dialog.open() != IDialogConstants.OK_ID) {
                    deleteGatedCheckinShelveset(e.getShelvesetName());
                    return Status.OK_STATUS;
                }

                if (dialog.getBypassBuild()) {
                    // The user chose to override the gated check-in. Attempt to
                    // resubmit the check-in with the override property set.
                    checkinCommand.setOverrideGatedCheckinOption();
                    retryWithOverride = true;
                    deleteGatedCheckinShelveset(e.getShelvesetName());
                    continue; // retry the checkin
                } else {
                    // The user chose to build the changes for the gated
                    // check-in. Queue a build for the shelved changes against
                    // the selected build definition.
                    final IQueueGatedCheckinBuild queueGatedCheckinBuild = getQueueGatedCheckinBuildContributor();

                    if (queueGatedCheckinBuild != null) {
                        final String buildDefinitionUri = dialog.getSelectedBuildDefinitionURI();
                        final TFSTeamProjectCollection c = repository.getVersionControlClient().getConnection();
                        final IBuildDefinition buildDefinition =
                            c.getBuildServer().getBuildDefinition(buildDefinitionUri);

                        if (queueGatedCheckinBuild.queueBuild(
                            getShell(),
                            buildDefinition,
                            buildDefinitionUri,
                            e.getShelvesetName(),
                            e.getCheckinTicket())) {
                            queuedGatedBuild = queueGatedCheckinBuild.getQueuedBuild();

                            if (!dialog.getPreserveLocalChanges()) {
                                // The user chose to undo the local pending
                                // changes after queuing the build.
                                final PendingChange[] checkedPendingChanges =
                                    pendingCheckin.getPendingChanges().getCheckedPendingChanges();

                                // Create item specs from the pending changes.
                                final ItemSpec[] itemSpecs = new ItemSpec[checkedPendingChanges.length];
                                for (int i = 0; i < checkedPendingChanges.length; i++) {
                                    final PendingChange pendingChange = checkedPendingChanges[i];
                                    itemSpecs[i] = new ItemSpec(pendingChange.getServerItem(), RecursionType.NONE);
                                }

                                // Undo the local pending changes. The undo task
                                // does not prompt the user for confirmation
                                // when supplying item specs, which is the
                                // desired behavior here.
                                final UndoPendingChangesTask undoTask =
                                    new UndoPendingChangesTask(getShell(), repository, itemSpecs);

                                undoTask.run();
                                pendingChangesCleared = true;
                            }

                            CodeMarkerDispatch.dispatch(CODEMARKER_GATED_CHECKIN_QUEUED);
                        }
                    }
                    return Status.OK_STATUS;
                }
            } else if (checkinStatus.getException() != null) {
                break;
            }

            if (checkinCommand.allConflictsResolved()) {
                MessageDialog.openInformation(
                    getShell(),
                    Messages.getString("CheckinTask.AllResolvedDialogTitle"), //$NON-NLS-1$
                    Messages.getString("CheckinTask.AllResolvedDialogMessage")); //$NON-NLS-1$
                return Status.OK_STATUS;
            }

            else if (!checkinCommand.hasResolvableConflicts()) {
                ErrorDialog.openError(
                    getShell(),
                    Messages.getString("CheckinTask.ErrorDialogTitle"), //$NON-NLS-1$
                    null,
                    checkinStatus);
                return checkinStatus;
            }

            final ConflictDescription[] conflicts = checkinCommand.getCheckinConflicts();

            final ConflictResolutionTask conflictTask = new ConflictResolutionTask(getShell(), repository, conflicts);
            final IStatus conflictStatus = conflictTask.run();

            if (conflictStatus.isOK()) {
                return Status.OK_STATUS;
            }
        } while (retryWithOverride);

        return checkinStatus;
    }

    /**
     * Returns true if the pending changes were cleared as part of this task.
     *
     * @return True if the pending changes were cleared.
     */
    public boolean getPendingChangesCleared() {
        return pendingChangesCleared;
    }

    /**
     * Returns the integer ID of the successfully checked-in changeset.
     *
     *
     * @return
     */
    public int getChangesetID() {
        return changesetID;
    }

    /**
     * Returns the queued build for a gated check-in. Will be null if a gated
     * check-in build did not occur.
     *
     *
     * @return
     */
    public IBuildDetail getGatedBuildDetail() {
        return queuedGatedBuild == null ? null : queuedGatedBuild.getBuild();
    }

    /**
     * Delete the gated check-in shelveset which was created by the server. Let
     * the caller decide how to handle exceptions.
     *
     * @param gatedCheckinShelvesetName
     *        The gated check-in shelveset to delete.
     * @returns A command status.
     */
    private IStatus deleteGatedCheckinShelveset(final String gatedCheckinShelvesetName) {
        final String fallbackUser = repository.getVersionControlClient().getConnection().getAuthorizedAccountName();
        final WorkspaceSpec spec = WorkspaceSpec.parse(gatedCheckinShelvesetName, fallbackUser);

        final QueryShelvesetsCommand queryCommand =
            new QueryShelvesetsCommand(repository.getVersionControlClient(), spec.getName(), spec.getOwner());

        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getShell());
        IStatus status = executor.execute(queryCommand);

        if (status.isOK()) {
            final Shelveset[] shelvesets = queryCommand.getShelvesets();
            if (shelvesets.length > 0) {
                final DeleteShelvesetsCommand deleteCommand = new DeleteShelvesetsCommand(repository, shelvesets[0]);
                status = executor.execute(deleteCommand);
            }
        }

        return status;
    }

    /**
     * Searches for the extension contributor for the
     * {@link #QUEUE_BUILD_EXTENSION_POINT_ID} point.
     *
     * @return Returns the extension point contributor or null if none if found.
     */
    private IQueueGatedCheckinBuild getQueueGatedCheckinBuildContributor() {
        try {
            return (IQueueGatedCheckinBuild) ExtensionLoader.loadSingleExtensionClass(QUEUE_BUILD_EXTENSION_POINT_ID);
        } catch (final Exception e) {
            log.error("The extension point to build a gated check-in was not found.", e); //$NON-NLS-1$
        }

        return null;
    }

    private static class CheckinCommandFinishedCallback implements ICommandFinishedCallback {
        private final ICommandFinishedCallback defaultCallbacks;

        public CheckinCommandFinishedCallback() {
            defaultCallbacks = UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback();
        }

        @Override
        public void onCommandFinished(final ICommand command, final IStatus status) {
            /*
             * GatedCheckinExceptions shouldn't be fed to the callbacks (which
             * may log them, display them to the user, etc.) since they're a
             * normal part of checking in to a gated definition and are handled
             * by other event listeners.
             */
            if (!status.isMultiStatus() && status.getException() instanceof GatedCheckinException) {
                return;
            }

            defaultCallbacks.onCommandFinished(command, status);
        }
    }
}
