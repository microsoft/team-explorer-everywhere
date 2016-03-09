// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.checkinpolicies.ExtensionPointPolicyLoader;
import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.PromoteCandidateChangesCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryShelvesetsCommand;
import com.microsoft.tfs.client.common.commands.vc.ScanLocalWorkspaceCommand;
import com.microsoft.tfs.client.common.connectionconflict.ConnectionConflictHandler;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.repository.RepositoryConflictException;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.compare.BaselineItemByPendingChangeGenerator;
import com.microsoft.tfs.client.common.ui.compare.ServerItemByItemVersionGenerator;
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemType;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.RepositoryChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.UndoPendingChangesAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.UndoUnchangedPendingChangesAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ShelvesetDetailsDialog;
import com.microsoft.tfs.client.common.ui.dialogs.workspaces.WorkspacesDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.framework.compare.CustomCompareConfiguration;
import com.microsoft.tfs.client.common.ui.helpers.ToggleMessageHelper;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.tasks.vc.ConflictResolutionTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.UnshelveTask;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesViewModel;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.clients.versioncontrol.GatedCheckinUtils;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.StandardPendingCheckin;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;

public class PendingChangesHelpers {
    public static final CodeMarker BEFORE_CONFIRM_DIALOG =
        new CodeMarker("com.microsoft.tfs.client.common.ui.views.PendingChangesView#beforeConfirmDialog"); //$NON-NLS-1$

    public static void unshelve(final Shell shell, final TFSRepository repository) {
        final UnshelveTask task = new UnshelveTask(shell, repository);
        task.run();
    }

    public static void resolveConflicts(final Shell shell, final TFSRepository repository) {
        final ConflictResolutionTask resolveTask = new ConflictResolutionTask(shell, repository, null);
        resolveTask.run();
    }

    public static void undoAll(final Shell shell, final TFSRepository repository) {
        final RepositoryChangeItemProvider provider = new RepositoryChangeItemProvider(repository);
        UndoPendingChangesAction.undoPendingChanges(shell, repository, provider.getChangeItems());
    }

    public static void undoUnchangedPendingChanges(final Shell shell, final TFSRepository repository) {
        final RepositoryChangeItemProvider provider = new RepositoryChangeItemProvider(repository);
        UndoUnchangedPendingChangesAction.undoUnchangedChanges(shell, repository, provider.getChangeItems());
    }

    public static void evaluatePolicies(final Shell shell, final PendingChangesViewModel model) {
        final PendingCheckin pendingCheckin = getPendingCheckin(shell, model, false);
        if (pendingCheckin == null) {
            model.clearPolicyWarnings();
            return;
        }

        model.evaluateCheckinPolicies(pendingCheckin);
    }

    public static void detectLocalChanges(final Shell shell, final TFSRepository repository) {
        Check.isTrue(repository.getWorkspace().getLocation() == WorkspaceLocation.LOCAL, "local workspaces only"); //$NON-NLS-1$

        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(shell);
        final IStatus status = executor.execute(new ScanLocalWorkspaceCommand(repository));

        if (!status.isOK()) {
            // TODO: NYI
        }
    }

    public static void showShelvesetDetails(
        final Shell shell,
        final TFSRepository repository,
        final String shelvesetName) {
        final String fallbackUser = repository.getVersionControlClient().getConnection().getAuthorizedAccountName();
        final WorkspaceSpec spec = WorkspaceSpec.parse(shelvesetName, fallbackUser);

        final QueryShelvesetsCommand queryCommand =
            new QueryShelvesetsCommand(repository.getVersionControlClient(), spec.getName(), spec.getOwner());

        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(shell);
        final IStatus status = executor.execute(queryCommand);

        if (status.isOK()) {
            final Shelveset[] shelvesets = queryCommand.getShelvesets();
            if (shelvesets.length > 0) {
                final ShelvesetDetailsDialog detailsDialog =
                    new ShelvesetDetailsDialog(shell, shelvesets[0], repository, false);
                detailsDialog.open();
            }
        }
    }

    public static boolean canCompareWithWorkspaceVersion(
        final TFSRepository repository,
        final PendingChange pendingChange) {
        if (pendingChange.getItemType() != ItemType.FILE) {
            return false;
        }

        if (containsSymlinkChange(pendingChange)) {
            return false;
        }

        String mappedLocalPath;
        try {
            mappedLocalPath = repository.getWorkspace().getMappedLocalPath(pendingChange.getServerItem());
        } catch (final ServerPathFormatException e) {
            return false;
        }
        if (mappedLocalPath == null || !new File(mappedLocalPath).exists()) {
            return false;
        }

        if (pendingChange.getChangeType().contains(ChangeType.DELETE)
            || pendingChange.getChangeType().contains(ChangeType.ADD)) {
            return false;
        }

        if (pendingChange.getChangeType().contains(ChangeType.BRANCH)
            && !pendingChange.getChangeType().contains(ChangeType.EDIT)) {
            return false;
        }

        return true;
    }

    public static boolean canCompareWithLatestVersion(final PendingChange pendingChange) {
        if (pendingChange.getItemType() != ItemType.FILE) {
            return false;
        }

        if (containsSymlinkChange(pendingChange)) {
            return false;
        }

        if (pendingChange.getChangeType().contains(ChangeType.DELETE)
            || pendingChange.getChangeType().contains(ChangeType.ADD)) {
            return false;
        }

        /*
         * Note: Microsoft's implementation disables this action when the change
         * type contains Rename or Branch. I think it makes more sense to enable
         * it under these conditions, as long as there is also an Edit.
         */

        if (pendingChange.getChangeType().contains(ChangeType.RENAME)
            && !pendingChange.getChangeType().contains(ChangeType.EDIT)) {
            return false;
        }

        if (pendingChange.getChangeType().contains(ChangeType.BRANCH)
            && !pendingChange.getChangeType().contains(ChangeType.EDIT)) {
            return false;
        }

        /* Ensure that the local file exists. */
        if (!new File(pendingChange.getLocalItem()).exists()) {
            return false;
        }

        return true;
    }

    public static boolean containsSymlinkChange(final PendingChange pendingChange) {
        if (pendingChange == null || pendingChange.getLocalItem() == null) {
            return false;
        }

        final boolean symlink = PropertyConstants.IS_SYMLINK.equals(
            PropertyUtils.selectMatching(pendingChange.getPropertyValues(), PropertyConstants.SYMBOLIC_KEY))
            || FileSystemUtils.getInstance().getAttributes(pendingChange.getLocalItem()).isSymbolicLink();
        return symlink;
    }

    public static void compareWithLatestVersion(
        final Shell shell,
        final TFSRepository repository,
        final PendingChange pendingChange,
        final CompareUIType compareUIType) {
        final Compare compare = new Compare();

        compare.setModifiedLocalPath(pendingChange.getLocalItem(), ResourceType.FILE);

        compare.setOriginal(
            new ServerItemByItemVersionGenerator(
                repository,
                pendingChange.getServerItem(),
                new WorkspaceVersionSpec(repository.getWorkspace()),
                LatestVersionSpec.INSTANCE));

        compare.setAncestor(
            new ServerItemByItemVersionGenerator(
                repository,
                pendingChange.getServerItem(),
                new WorkspaceVersionSpec(repository.getWorkspace()),
                new ChangesetVersionSpec(pendingChange.getVersion())));

        compare.addComparator(TFSItemContentComparator.INSTANCE);

        compare.getCompareConfiguration().setProperty(
            CustomCompareConfiguration.LEFT_LABEL_SUFFIX_PROPERTY,
            Messages.getString("ComparePendingChangeWithLatestVersionAction.LocalSuffix")); //$NON-NLS-1$

        compare.setUIType(compareUIType);

        compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(shell));
        compare.open();
    }

    public static void compareWithWorkspaceVersion(
        final Shell shell,
        final TFSRepository repository,
        final PendingChange pendingChange,
        final CompareUIType compareUIType) {
        final Compare compare = new Compare();

        compare.setModifiedLocalPath(pendingChange.getLocalItem(), ResourceType.FILE);

        if (repository.getWorkspace().getLocation() == WorkspaceLocation.LOCAL) {
            /*
             * For a local workspace, use a generator which doesn't query the
             * server to resolve the workspace version into a changeset version,
             * because we probably have a local baseline file to get content
             * from. It's possible the baseline file is missing, and the server
             * will be contacted in that case.
             */
            compare.setOriginal(new BaselineItemByPendingChangeGenerator(repository.getWorkspace(), pendingChange));
        } else {
            compare.setOriginal(
                new ServerItemByItemVersionGenerator(
                    repository,
                    pendingChange.getServerItem(),
                    new WorkspaceVersionSpec(repository.getWorkspace()),
                    new WorkspaceVersionSpec(repository.getWorkspace())));
        }

        compare.addComparator(TFSItemContentComparator.INSTANCE);

        compare.getCompareConfiguration().setProperty(
            CustomCompareConfiguration.LEFT_LABEL_SUFFIX_PROPERTY,
            Messages.getString("ComparePendingChangeWithWorkspaceVersionAction.LocalSuffix")); //$NON-NLS-1$

        compare.setUIType(compareUIType);

        compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(shell));
        compare.open();
    }

    public static void manageWorkspaces(final Shell shell) {
        final RepositoryManager repositoryManager =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();

        final WorkspacesDialog dialog = new WorkspacesDialog(
            shell,
            repositoryManager.getDefaultRepository().getWorkspace().getClient().getConnection(),
            false,
            false,
            Messages.getString("WorkspaceToolbarPulldownAction.WorkspacesDialogTitle")); //$NON-NLS-1$

        /*
         * Don't allow the user to modify some data about the active workspace
         * (name, server, owner, etc.). The user can still always modify working
         * folder mappings.
         */
        final TFSRepository[] immutableRepositories =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getRepositories();

        if (immutableRepositories != null && immutableRepositories.length > 0) {
            /* Convert to Workspaces */
            final Workspace[] immutableWorkspaces = new Workspace[immutableRepositories.length];

            for (int i = 0; i < immutableRepositories.length; i++) {
                immutableWorkspaces[i] = immutableRepositories[i].getWorkspace();
            }

            dialog.getWorkspacesControl().setImmutableWorkspaces(immutableWorkspaces);
        }

        dialog.open();
    }

    public static void showPromoteCandidateChanges(
        final Shell shell,
        final TFSRepository repository,
        final ChangeItem[] candidates) {
        Check.notNull(candidates, "candidates"); //$NON-NLS-1$

        final PromoteCandidateChangesDialog dialog = new PromoteCandidateChangesDialog(shell, repository, candidates);

        if (dialog.open() == IDialogConstants.OK_ID) {
            final PromoteCandidateChangesCommand command = new PromoteCandidateChangesCommand(
                repository,
                ChangeItem.getPendingChanges(dialog.getCheckedCandidates()));

            UICommandExecutorFactory.newUICommandExecutor(shell).execute(command);
        }
    }

    public static boolean confirmShelvesetCanBeWritten(
        final Shell shell,
        final TFSRepository repository,
        final String shelvesetName) {
        final QueryShelvesetsCommand queryCommand = new QueryShelvesetsCommand(
            repository.getVersionControlClient(),
            shelvesetName,
            VersionControlConstants.AUTHENTICATED_USER);

        final IStatus queryStatus = UICommandExecutorFactory.newUICommandExecutor(shell).execute(queryCommand);

        if (!queryStatus.isOK()) {
            return false;
        }

        final Shelveset[] existingShelvesets = queryCommand.getShelvesets();

        if (existingShelvesets != null && existingShelvesets.length > 0) {
            final String title = Messages.getString("ShelveDialog.OverwriteDialogTitle"); //$NON-NLS-1$
            final String messageFormat = Messages.getString("ShelveDialog.OverwriteDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, shelvesetName);

            if (!MessageDialog.openQuestion(shell, title, message)) {
                return false;
            }
        }

        return true;
    }

    public static PendingCheckin getPendingCheckin(
        final Shell shell,
        final PendingChangesViewModel model,
        final boolean evaluatePolicies) {
        // Warn if there are on Included pending changes.
        final PendingChange[] changes = model.getIncludedUnfilteredPendingChanges();
        if (changes.length == 0) {
            MessageDialog.openWarning(
                shell,
                Messages.getString("PendingChangesView.NoChangesDialogTitle"), //$NON-NLS-1$
                Messages.getString("PendingChangesView.NoChangesDialogText")); //$NON-NLS-1$
            return null;
        }

        final TFSRepository repository = model.getRepository();
        final Workspace workspace = model.getWorkspace();
        final PendingChange[] allChanges = model.getAllPendingChanges();

        final PolicyEvaluator policyEvaluator = evaluatePolicies
            ? new PolicyEvaluator(repository.getVersionControlClient(), new ExtensionPointPolicyLoader()) : null;

        return new StandardPendingCheckin(
            workspace,
            allChanges,
            changes,
            model.getComment(),
            model.getCheckinNote(),
            model.getAssociatedWorkItems(),
            policyEvaluator);
    }

    public static boolean confirmCheckin(final Shell shell, final int changesCount) {
        String message;
        if (changesCount > 1) {
            final String messageFormat = Messages.getString("PendingChangesView.MultiChangesConfirmTextFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, changesCount);
        } else {
            message = Messages.getString("PendingChangesView.SingleChangeConfirmTextFormat"); //$NON-NLS-1$
        }

        CodeMarkerDispatch.dispatch(BEFORE_CONFIRM_DIALOG);
        if (!ToggleMessageHelper.openYesNoQuestion(
            shell,
            Messages.getString("PendingChangesView.ConfirmCheckinDialogTitle"), //$NON-NLS-1$
            message,
            Messages.getString("PendingChangesView.CheckinWithoutPromptCheckboxText"), //$NON-NLS-1$
            false,
            UIPreferenceConstants.PROMPT_BEFORE_CHECKIN)) {
            return false;
        }

        return true;
    }

    /**
     * Call after a gated build changeset has been reconciled (unmodified local
     * changes undone and new items fetched). Clears the comment, associated
     * work items, and notes if and only if the committed changeset and
     * associated items exactly matches the control's current state.
     *
     * @param changeset
     *        the committed changeset (must not be <code>null</code>)
     * @param associatedWorkItems
     *        the associated work items for the changeset (must not be
     *        <code>null</code>)
     */
    public static void afterReconcileGatedCheckin(
        final Changeset changeset,
        final WorkItem[] associatedWorkItems,
        final CheckinNote noteBeforeUndo) {
        Check.notNull(changeset, "changeset"); //$NON-NLS-1$
        Check.notNull(associatedWorkItems, "associatedWorkItems"); //$NON-NLS-1$
        Check.notNull(noteBeforeUndo, "noteBeforeUndo"); //$NON-NLS-1$

        final PendingChangesViewModel model = TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel();

        final boolean commentsMatch =
            GatedCheckinUtils.gatedCheckinCommentsMatch(changeset.getComment(), model.getComment());

        final boolean checkinNotesMatch =
            GatedCheckinUtils.gatedCheckinNotesMatch(changeset.getCheckinNote(), noteBeforeUndo);

        final int[] committedWorkItemIds = getIDsForWorkItems(associatedWorkItems);

        final WorkItemCheckinInfo[] workItemInfos = model.getAssociatedWorkItems();
        final int[] pendingWorkItemIds = getIDsForWorkItemCheckinInfos(workItemInfos);

        final boolean workItemsMatch =
            GatedCheckinUtils.gatedCheckinWorkItemsMatch(committedWorkItemIds, pendingWorkItemIds);

        if (commentsMatch && checkinNotesMatch && workItemsMatch) {
            // Resets the comments, notes, and work items
            model.clearComment();
            model.dissociateAllWorkItems();
            model.clearCheckinNotes();
        }
    }

    /**
     * Call after unshelving changes. Updates teh comment, associated work
     * items, and notes from the shelveset.
     *
     * @param shelveset
     *        the shelveset to update with (must not be <code>null</code>)
     */
    public static void afterUnshelve(final Shelveset shelveset) {
        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$

        final PendingChangesViewModel model = TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel();

        final WorkItemClient workItemClient = model.getWorkspace().getClient().getConnection().getWorkItemClient();

        model.setComment(shelveset.getComment());
        model.associateWorkItems(shelveset.getWorkItemInfo(workItemClient));
        model.setCheckinNoteFieldValues(shelveset.getCheckinNote().getValues());
    }

    public static ChangeItem[] pendingChangesToChangeItems(
        final TFSRepository repository,
        final PendingChange[] pendingChanges) {
        final List<ChangeItem> changes = new ArrayList<ChangeItem>();

        for (final PendingChange pendingChange : pendingChanges) {
            changes.add(new ChangeItem(pendingChange, ChangeItemType.PENDING, repository));
        }

        return changes.toArray(new ChangeItem[changes.size()]);
    }

    public static int[] getIDsForWorkItemCheckinInfos(final WorkItemCheckinInfo[] workItemInfos) {
        final int[] ids = new int[workItemInfos.length];
        for (int i = 0; i < workItemInfos.length; i++) {
            ids[i] = workItemInfos[i].getWorkItem().getID();
        }
        return ids;
    }

    public static int[] getIDsForWorkItems(final WorkItem[] workItems) {
        final int[] ids = new int[workItems.length];
        for (int i = 0; i < workItems.length; i++) {
            ids[i] = workItems[i].getID();
        }
        return ids;
    }

    public static void switchWorkspace(final Shell shell, final Workspace workspace) {
        final RepositoryManager repositoryManager =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();

        final ConnectionConflictHandler connectionConflictHandler =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getConnectionConflictHandler();

        /*
         * Ensure the workspace exists.
         */
        try {
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer().getConnection().getVersionControlClient().queryWorkspace(
                workspace.getName(),
                workspace.getOwnerName());
        } catch (final TECoreException e) {
            final String message =
                MessageFormat.format(
                    Messages.getString("WorkspaceToolbarPulldownAction.WorkspaceDoesNotExistFormat"), //$NON-NLS-1$
                    new WorkspaceSpec(workspace.getOwnerName(), workspace.getOwnerDisplayName()).toString());

            ErrorDialog.openError(
                shell,
                Messages.getString("WorkspaceToolbarPulldownAction.WorkspaceNotFound"), //$NON-NLS-1$
                null,
                new Status(Status.WARNING, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, null));

            return;
        }

        /*
         * Switch to the selected workspace.
         */
        try {
            repositoryManager.getOrCreateRepository(workspace);
        } catch (final RepositoryConflictException conflictException) {
            /*
             * Another connection to a server already exists: allow the product
             * plugin's connection conflict handler to retry this.
             */
            if (connectionConflictHandler.resolveRepositoryConflict()) {
                /* Retry */
                try {
                    repositoryManager.getOrCreateRepository(workspace);
                } catch (final RepositoryConflictException f) {
                    connectionConflictHandler.notifyRepositoryConflict();
                }
            }
        }

        UIConnectionPersistence.getInstance().setLastUsedWorkspace(workspace);
    }

    public static boolean isPendingAdd(final TFSRepository repository, final String localPath) {
        final PendingChange pendingChange = repository.getPendingChangeCache().getPendingChangeByLocalPath(localPath);
        return pendingChange != null && pendingChange.getChangeType().contains(ChangeType.ADD);
    }
}
