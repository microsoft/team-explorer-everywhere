// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse;

import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.commands.vc.AddCommand;
import com.microsoft.tfs.client.common.commands.vc.DeleteCommand;
import com.microsoft.tfs.client.common.commands.vc.DeleteWorkingFolderCommand;
import com.microsoft.tfs.client.common.commands.vc.EditCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsExtendedCommand;
import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.commands.vc.RenameCommand;
import com.microsoft.tfs.client.common.commands.vc.RenameMultipleCommand;
import com.microsoft.tfs.client.common.commands.vc.UndoCommand;
import com.microsoft.tfs.client.common.commands.vc.UpdateLocalVersionCommand;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ExtensionPointAsyncObjectWaiter;
import com.microsoft.tfs.client.common.framework.command.IAsyncObjectWaiter;
import com.microsoft.tfs.client.common.framework.command.JobCommandAdapter;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter.Builder;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter.CompositeResourceFilterType;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilters;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationValidator;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.repository.ResourceRepositoryMap;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceDataManager;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.core.clients.versioncontrol.ClientLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueueOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/*
 * Important points to remember:
 *
 * All the updates will be done with GetOptions.NO_DISK_ACCESS, which means that
 * the server will make the changes, and any getops returning from these
 * operations will be ignored. This is done so that we can call
 * tree.standardMove/Delete* functions. These know better than we how to deal
 * with the actual deletion and movement of resources within Eclipse. (They
 * will, for example, make backups as appropriate, etc.)
 *
 * Despite the documentation, DO NOT use tree.failed() to report failures. While
 * this seems to be the proper way to do it, these Statuses get wrapped with
 * various exceptions to the point that the actual failure you passed is totally
 * ignored. Users merely get a message box that says "A failure occurred" with
 * no context. Instead, you have to throw a RuntimeException.
 */
public final class TFSMoveDeleteHook implements IMoveDeleteHook {
    private final Log log = LogFactory.getLog(TFSMoveDeleteHook.class);

    private static final ChangeType ALL_CHANGE_TYPES = ChangeType.combine(new ChangeType[] {
        ChangeType.ADD,
        ChangeType.BRANCH,
        ChangeType.DELETE,
        ChangeType.EDIT,
        ChangeType.ENCODING,
        ChangeType.LOCK,
        ChangeType.MERGE,
        ChangeType.RENAME,
        ChangeType.ROLLBACK,
        ChangeType.SOURCE_RENAME,
        ChangeType.UNDELETE
    });

    /*
     * Deletes can only proceed when a file or folder beneath the deletion has a
     * pending change of delete. (This is all change types sans delete.)
     */
    private static final ChangeType DELETE_DENIED_CHANGE_TYPES = ChangeType.combine(new ChangeType[] {
        ChangeType.BRANCH,
        ChangeType.LOCK,
        ChangeType.MERGE,
        ChangeType.RENAME,
        ChangeType.ROLLBACK,
        ChangeType.SOURCE_RENAME,
        ChangeType.UNDELETE
    });

    private static final ChangeType ADD_DENIED_CHANGE_TYPES = ALL_CHANGE_TYPES;
    private static final ChangeType MOVE_DENIED_CHANGE_TYPES = ChangeType.combine(new ChangeType[] {
        ChangeType.BRANCH,
        ChangeType.MERGE,
        ChangeType.SOURCE_RENAME
    });

    /**
     * Items rejected by this filter are considered ignored: deletes are
     * deferred to Eclipse (no delete pended), moves across an ignore boundary
     * (ignored source to unignored target or other way around) are pended as
     * renames, but moves inside one zone (ignored to ignored) are not deferred
     * to Eclipse (no rename pended).
     *
     * This list does not include linked resources.
     *
     * {@link PluginResourceFilters#TFS_IGNORE_FILTER} should not be used here,
     * otherwise we would fail to pend changes that the local workspace scanner
     * would later detect (perhaps breaking a rename into an add/delete).
     */
    private final static ResourceFilter IGNORED_RESOURCES_FILTER =
        new Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT).addFilter(
            PluginResourceFilters.TEAM_IGNORED_RESOURCES_FILTER).addFilter(
                ResourceFilters.TEAM_PRIVATE_RESOURCES_FILTER).addFilter(PluginResourceFilters.TPIGNORE_FILTER).build();

    private final TFSRepositoryProvider repositoryProvider;

    public TFSMoveDeleteHook(final TFSRepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public boolean deleteFile(
        final IResourceTree tree,
        final IFile file,
        final int updateFlags,
        final IProgressMonitor progressMonitor) {
        log.trace("deleteFile: " + file.getFullPath()); //$NON-NLS-1$

        progressMonitor.beginTask(
            MessageFormat.format(Messages.getString("TFSMoveDeleteHook.DeletingPathFormat"), file.getFullPath()), //$NON-NLS-1$
            30);

        try {
            /* Do the common pre-deletion resource inspection */
            final ResourceInspectionResult inspectionResult = inspectResource(
                tree,
                file,
                true,
                true,
                DELETE_DENIED_CHANGE_TYPES,
                updateFlags,
                false,
                new SubProgressMonitor(progressMonitor, 5));

            /*
             * Defer to the platform, returning false meaning we do not do any
             * work.
             */
            if (inspectionResult.getStatus().equals(ResourceInspectionStatus.DEFER)) {
                return false;
            }

            if (repositoryProvider.getRepositoryStatus() == ProjectRepositoryStatus.CONNECTING) {
                throw new RuntimeException(
                    Messages.getString("TFSMoveDeleteHook.ConnectionInProgressWaitBeforeDeleting")); //$NON-NLS-1$
            }

            log.info(MessageFormat.format("Deletion detected for file {0}", file)); //$NON-NLS-1$

            final TFSRepository repository = inspectionResult.getRepository();
            final PendingChange[] pendingChanges = inspectionResult.getPendingChanges();

            final IStatus deleteStatus =
                deleteFile(file, repository, pendingChanges, new SubProgressMonitor(progressMonitor, 10));

            if (!deleteStatus.isOK()) {
                throw new RuntimeException(deleteStatus.getMessage());
            }

            tree.standardDeleteFile(file, updateFlags, new SubProgressMonitor(progressMonitor, 5));
            return true;
        } finally {
            progressMonitor.done();
        }
    }

    private IStatus deleteFile(
        final IFile file,
        final TFSRepository repository,
        final PendingChange[] pendingChanges,
        final IProgressMonitor progressMonitor) {
        progressMonitor.beginTask(
            MessageFormat.format(Messages.getString("TFSMoveDeleteHook.DeletingPathFormat"), file.getFullPath()), //$NON-NLS-1$
            20);

        try {
            if (pendingChanges.length > 1) {
                throw new RuntimeException(
                    Messages.getString("TFSMoveDeleteHook.ReceivedMultiplePendingChangesForSingleFile")); //$NON-NLS-1$
            }
            /* Undo pending changes for the file */
            else if (pendingChanges.length == 1) {
                final UndoCommand undoCommand = new UndoCommand(repository, new ItemSpec[] {
                    new ItemSpec(file.getLocation().toOSString(), RecursionType.NONE)
                });
                final IStatus undoStatus = runCommand(undoCommand, new SubProgressMonitor(progressMonitor, 10));

                if (!undoStatus.isOK()) {
                    return undoStatus;
                }
            }

            /*
             * Pend a delete iff we didn't just undo the pending add for the
             * file.
             */
            if (pendingChanges.length == 0 || !pendingChanges[0].getChangeType().contains(ChangeType.ADD)) {
                final DeleteCommand deleteCommand = new DeleteCommand(repository, new String[] {
                    file.getLocation().toOSString()
                }, RecursionType.NONE, LockLevel.UNCHANGED, GetOptions.NO_DISK_UPDATE, PendChangesOptions.NONE);
                final IStatus deleteStatus = runCommand(deleteCommand, new SubProgressMonitor(progressMonitor, 10));

                if (!deleteStatus.isOK() && deleteStatus.getSeverity() == IStatus.ERROR) {
                    return deleteStatus;
                }
            }

            return Status.OK_STATUS;
        } finally {
            progressMonitor.done();
        }
    }

    @Override
    public boolean deleteFolder(
        final IResourceTree tree,
        final IFolder folder,
        final int updateFlags,
        final IProgressMonitor progressMonitor) {
        log.info("deleteFolder: " + folder.getFullPath()); //$NON-NLS-1$

        if (repositoryProvider.getRepositoryStatus() == ProjectRepositoryStatus.CONNECTING) {
            throw new RuntimeException(Messages.getString("TFSMoveDeleteHook.ConnectionInProgressWaitBeforeDeleting")); //$NON-NLS-1$
        }

        progressMonitor.beginTask(
            MessageFormat.format(Messages.getString("TFSMoveDeleteHook.DeletingPathFormat"), folder.getFullPath()), //$NON-NLS-1$
            20);

        try {
            /* Do the common pre-deletion resource inspection */
            final ResourceInspectionResult inspectionResult = inspectResource(
                tree,
                folder,
                true,
                true,
                DELETE_DENIED_CHANGE_TYPES,
                updateFlags,
                true,
                new SubProgressMonitor(progressMonitor, 5));

            /*
             * Defer to the platform, returning false meaning we do not do any
             * work.
             */
            if (inspectionResult.getStatus().equals(ResourceInspectionStatus.DEFER)) {
                return false;
            }

            /*
             * Deletion should be done later by a separate user's request.
             */
            if (inspectionResult.getStatus().equals(ResourceInspectionStatus.WARN)) {
                TFSEclipseClientPlugin.getDefault().getConsole().printWarning(inspectionResult.getMessage());
                log.warn("Deletion should be done later by a separate user's request."); //$NON-NLS-1$
                log.warn(inspectionResult.getMessage());
                return true;
            }

            log.info(MessageFormat.format("Deletion detected for folder {0}", folder)); //$NON-NLS-1$

            final TFSRepository repository = inspectionResult.getRepository();
            final PendingChange[] pendingChanges = inspectionResult.getPendingChanges();
            final boolean isInServer = inspectionResult.isInServer();

            final IStatus deleteStatus = deleteFolder(
                folder,
                repository,
                isInServer,
                pendingChanges,
                new SubProgressMonitor(progressMonitor, 10));

            if (!deleteStatus.isOK()) {
                PendingChange[] allChanges = repository.getPendingChangeCache().getPendingChanges();
                log.info("All pending changes: [" + String.valueOf(allChanges.length) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                for (final PendingChange change : allChanges) {
                    log.info("  ChangeType: " + change.getChangeType().toString()); //$NON-NLS-1$
                    log.info("    ServerItem: " + change.getServerItem()); //$NON-NLS-1$
                    log.info("    LocalItem: " + (change.getLocalItem() == null ? "null" : change.getLocalItem())); //$NON-NLS-1$ //$NON-NLS-2$
                    log.info("    SourceLocalItem: " //$NON-NLS-1$
                        + (change.getSourceLocalItem() == null ? "null" : change.getSourceLocalItem())); //$NON-NLS-1$
                    log.info("    SourceServerItem: " //$NON-NLS-1$
                        + (change.getSourceServerItem() == null ? "null" : change.getSourceServerItem())); //$NON-NLS-1$
                    log.info("    Date: " //$NON-NLS-1$
                        + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z").format( //$NON-NLS-1$
                            change.getWebServiceObject().getDate().getTime()));
                }

                log.info("Refreshing pending changes cache"); //$NON-NLS-1$
                repository.getPendingChangeCache().refresh();

                allChanges = repository.getPendingChangeCache().getPendingChanges();
                log.info("All pending changes: [" + String.valueOf(allChanges.length) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                for (final PendingChange change : allChanges) {
                    log.info("  ChangeType: " + change.getChangeType().toString()); //$NON-NLS-1$
                    log.info("    ServerItem: " + change.getServerItem()); //$NON-NLS-1$
                    log.info("    LocalItem: " + (change.getLocalItem() == null ? "null" : change.getLocalItem())); //$NON-NLS-1$ //$NON-NLS-2$
                    log.info("    SourceLocalItem: " //$NON-NLS-1$
                        + (change.getSourceLocalItem() == null ? "null" : change.getSourceLocalItem())); //$NON-NLS-1$
                    log.info("    SourceServerItem: " //$NON-NLS-1$
                        + (change.getSourceServerItem() == null ? "null" : change.getSourceServerItem())); //$NON-NLS-1$
                    log.info("    Date: " //$NON-NLS-1$
                        + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z").format( //$NON-NLS-1$
                            change.getWebServiceObject().getDate().getTime()));
                }

                throw new RuntimeException(deleteStatus.getMessage());
            }

            tree.standardDeleteFolder(folder, updateFlags, new SubProgressMonitor(progressMonitor, 5));
            return true;
        } finally {
            progressMonitor.done();
        }
    }

    private IStatus deleteFolder(
        final IFolder folder,
        final TFSRepository repository,
        final boolean isInServer,
        final PendingChange[] pendingChanges,
        final IProgressMonitor progressMonitor) {
        progressMonitor.beginTask(
            MessageFormat.format(Messages.getString("TFSMoveDeleteHook.DeletingPathFormat"), folder.getFullPath()), //$NON-NLS-1$
            20);

        try {
            boolean folderAddUndone = false;
            if (pendingChanges.length > 0) {
                for (int i = 0; i < pendingChanges.length; i++) {
                    if (pendingChanges[i].getLocalItem().equals(folder.getLocation().toOSString())
                        && pendingChanges[i].getChangeType().contains(ChangeType.ADD)) {
                        folderAddUndone = true;
                    }
                }

                final UndoCommand undoCommand = new UndoCommand(repository, new ItemSpec[] {
                    new ItemSpec(folder.getLocation().toOSString(), RecursionType.FULL)
                });
                final IStatus undoStatus = runCommand(undoCommand, new SubProgressMonitor(progressMonitor, 10));

                if (!undoStatus.isOK()) {
                    return undoStatus;
                }
            }

            /*
             * Pend a delete iff we didn't just undo a pending add for this
             * folder OR if the folder does not exist on the server (ie, it's
             * synthesized due to pending child adds.)
             */
            if (!folderAddUndone && isInServer) {
                final DeleteCommand deleteCommand = new DeleteCommand(repository, new String[] {
                    folder.getLocation().toOSString()
                }, RecursionType.FULL, LockLevel.UNCHANGED, GetOptions.NO_DISK_UPDATE, PendChangesOptions.NONE);
                final IStatus deleteStatus = runCommand(deleteCommand, new SubProgressMonitor(progressMonitor, 10));

                if (!deleteStatus.isOK() && deleteStatus.getSeverity() == IStatus.ERROR) {
                    return deleteStatus;
                }
            }

            return Status.OK_STATUS;
        } finally {
            progressMonitor.done();
        }
    }

    @Override
    public boolean deleteProject(
        final IResourceTree tree,
        final IProject project,
        final int updateFlags,
        final IProgressMonitor progressMonitor) {
        log.trace(MessageFormat.format("deleteProject: {0}", project.getFullPath())); //$NON-NLS-1$

        if (repositoryProvider.getRepositoryStatus() == ProjectRepositoryStatus.CONNECTING) {
            throw new RuntimeException(Messages.getString("TFSMoveDeleteHook.ConnectionInProgressWaitBeforeDeleting")); //$NON-NLS-1$
        }

        progressMonitor.beginTask(
            MessageFormat.format(Messages.getString("TFSMoveDeleteHook.DeletingPathFormat"), project.getName()), //$NON-NLS-1$
            20);

        try {
            /* See if we're online */
            final ResourceRepositoryMap repositoryMap = PluginResourceHelpers.mapResources(new IResource[] {
                project
            });
            final TFSRepository repository = repositoryMap.getRepository(project);

            /* Don't allow project deletion while offline. */
            /* TODO: investigate */
            if (repository == null) {
                throw new RuntimeException(
                    MessageFormat.format(
                        Messages.getString("TFSMoveDeleteHook.CannotRemoveProjectWhileDisconnectedFormat"), //$NON-NLS-1$
                        project.getName()));
            }

            /*
             * Uses the same logic as oldplugin: if the user selected
             * "delete files on disk" we notify the server that we're deleting
             * the files. Otherwise, just allow it to proceed.
             */
            if ((updateFlags & IResource.NEVER_DELETE_PROJECT_CONTENT) == IResource.NEVER_DELETE_PROJECT_CONTENT) {
                /* Remove from project repository manager. */
                TFSEclipseClientPlugin.getDefault().getProjectManager().removeProject(project);

                return false;
            }

            final QueryItemsExtendedCommand queryCommand = new QueryItemsExtendedCommand(
                repository,
                project.getLocation().toOSString(),
                ItemType.ANY,
                DeletedState.NON_DELETED,
                RecursionType.FULL,
                GetItemsOptions.NONE);
            final IStatus queryStatus = runCommand(queryCommand, new SubProgressMonitor(progressMonitor, 5));

            if (!queryStatus.isOK()) {
                throw new RuntimeException(queryStatus.getMessage());
            }

            final ExtendedItem[][] items = queryCommand.getItems();

            if (items == null || items.length == 0 || items[0] == null || items[0].length == 0) {
                return true;
            }

            final ClientLocalVersionUpdate[] updates = new ClientLocalVersionUpdate[items[0].length];

            for (int i = 0; i < items[0].length; i++) {
                updates[i] = new ClientLocalVersionUpdate(
                    items[0][i].getSourceServerItem(),
                    items[0][i].getItemID(),
                    null,
                    items[0][i].getLocalVersion(),
                    items[0][i].getPropertyValues());
            }

            final UpdateLocalVersionCommand updateCommand =
                new UpdateLocalVersionCommand(repository, updates, UpdateLocalVersionQueueOptions.UPDATE_BOTH);
            final IStatus updateStatus = runCommand(updateCommand, new SubProgressMonitor(progressMonitor, 10));

            if (!updateStatus.isOK()) {
                throw new RuntimeException(updateStatus.getMessage());
            }

            /*
             * Unmap if and only if this exact path is mapped. (ie, through the
             * typical import process, for example.)
             */
            final WorkingFolder workingFolder =
                repository.getWorkspace().getExactMappingForLocalPath(project.getLocation().toOSString());

            if (workingFolder != null) {
                final DeleteWorkingFolderCommand deleteWfCommand =
                    new DeleteWorkingFolderCommand(repository, workingFolder);
                final IStatus deleteStatus = runCommand(deleteWfCommand, new SubProgressMonitor(progressMonitor, 5));

                if (!deleteStatus.isOK()) {
                    throw new RuntimeException(deleteStatus.getMessage());
                }
            }

            /* Remove from project repository manager. */
            TFSEclipseClientPlugin.getDefault().getProjectManager().removeProject(project);

            tree.standardDeleteProject(project, updateFlags, new SubProgressMonitor(progressMonitor, 5));

            return true;
        } finally {
            progressMonitor.done();
        }
    }

    @Override
    public boolean moveFile(
        final IResourceTree tree,
        final IFile source,
        final IFile target,
        final int updateFlags,
        final IProgressMonitor progressMonitor) {
        log.trace(MessageFormat.format("moveFile: {0} to {1}", source.getFullPath(), target.getFullPath())); //$NON-NLS-1$

        if (repositoryProvider.getRepositoryStatus() == ProjectRepositoryStatus.CONNECTING) {
            throw new RuntimeException(Messages.getString("TFSMoveDeleteHook.ConnectionInProgressWaitBeforeMoving")); //$NON-NLS-1$
        }

        progressMonitor.beginTask(
            MessageFormat.format(
                Messages.getString("TFSMoveDeleteHook.MovingSourceToTargetFormat"), //$NON-NLS-1$
                source.getFullPath(),
                target.getFullPath()),
            20);

        try {
            final MoveResourceInspectionResult moveResult =
                inspectMove(tree, source, target, updateFlags, new SubProgressMonitor(progressMonitor, 5));

            if (moveResult.getStatus() == ResourceInspectionStatus.DEFER) {
                return false;
            }

            /*
             * The source is mapped in TFS but the target is not a TFS-managed
             * project. We need to pend a delete on the source and ignore the
             * target entirely (let the target's SCM, if configured, deal with
             * it.)
             */
            if (moveResult.getOperation() == MoveResourceOperation.DELETE_SOURCE) {
                log.info(
                    MessageFormat.format(
                        "Rename detected from file {0} to {1} (target not managed by TFS, source will be deleted)", //$NON-NLS-1$
                        source,
                        target));

                final TFSRepository repository = moveResult.getRepository();
                final PendingChange[] pendingChanges = moveResult.getSourcePendingChanges();

                final IStatus deleteStatus =
                    deleteFile(source, repository, pendingChanges, new SubProgressMonitor(progressMonitor, 10));

                if (!deleteStatus.isOK()) {
                    throw new RuntimeException(deleteStatus.getMessage());
                }

                tree.standardMoveFile(source, target, updateFlags, new SubProgressMonitor(progressMonitor, 5));

                /*
                 * Moving a file from a TFS-managed project to a non-managed
                 * project can be preceded by a refactor edit, which leaves the
                 * source file writable, which causes the DeleteCommand above to
                 * cause a source-writable warning in core (which can be safely
                 * ignored here because we'll just use tree.standardMoveFile
                 * anyway). If the warning happened, the delete event to refresh
                 * pending changes didn't fire, so just force a refresh here to
                 * cover all cases.
                 */
                refreshPendingChangesCache(repository);

                return true;
            }

            /*
             * Note: we do not get called unless the source of the move is in a
             * TFS-managed projects. Thus we do not deal with the case where the
             * source is not managed but the target is.
             * TFSResourceChangeListener is called for these situations.
             */

            /*
             * Both projects are TFS-managed (or the same project.) We can
             * safely pend a rename here.
             */
            else if (moveResult.getOperation() == MoveResourceOperation.MOVE) {
                log.info(MessageFormat.format("Rename detected from file {0} to {1}", source, target)); //$NON-NLS-1$

                final TFSRepository repository = moveResult.getRepository();
                final String sourceServerPath = moveResult.getSourceServerPath();
                final String targetServerPath = moveResult.getTargetServerPath();

                /*
                 * Detect when the source file (which soon be the target file)
                 * is writable but does not currently have a pending edit
                 * change, and check it out before the rename. This facilitates
                 * the case that the source is ignored by .tpignore (or some
                 * other ignore mechanism), and the user is renaming it, and the
                 * file has triggered a refactor process (like Java files do)
                 * which edited the source (which was made writable without
                 * pending an edit [because it was ignored]). After the rename,
                 * we want the file to have both rename and edit changes.
                 */
                if (WorkspaceLocation.SERVER.equals(repository.getWorkspace().getLocation())
                    && source.isReadOnly() == false
                    && containsChangeType(moveResult.getSourcePendingChanges(), ChangeType.EDIT) == false) {
                    /*
                     * Pass NO_DISK_UPDATE to get options (unnecessary), but
                     * pass FORCE_CHECK_OUT_LOCAL_VERSION in case the user has
                     * get latest on checkout enabled. This will prevent an
                     * inevitable writable conflict here at the expense of a
                     * version conflict when the user performs a get or
                     * check-in. This seems preferable, as the UI is better
                     * prepared to deal with conflicts at that point.
                     */

                    final EditCommand editCommand = new EditCommand(
                        repository,
                        new ItemSpec[] {
                            new ItemSpec(sourceServerPath, RecursionType.NONE)
                    },
                        LockLevel.UNCHANGED,
                        null,
                        GetOptions.NO_DISK_UPDATE,
                        PendChangesOptions.FORCE_CHECK_OUT_LOCAL_VERSION,
                        false);

                    final IStatus editStatus = runCommand(editCommand, new SubProgressMonitor(progressMonitor, 10));

                    if (editStatus.getSeverity() == IStatus.ERROR) {
                        throw new RuntimeException(editStatus.getMessage());
                    }
                }

                final RenameCommand renameCommand = new RenameCommand(
                    repository,
                    sourceServerPath,
                    targetServerPath,
                    LockLevel.UNCHANGED,
                    GetOptions.NO_DISK_UPDATE,
                    true,
                    PendChangesOptions.NONE);
                final IStatus renameStatus = runCommand(renameCommand, new SubProgressMonitor(progressMonitor, 10));

                if (renameStatus.getSeverity() == IStatus.ERROR) {
                    throw new RuntimeException(renameStatus.getMessage());
                }

                tree.standardMoveFile(source, target, updateFlags, new SubProgressMonitor(progressMonitor, 5));

                /*
                 * The server can undo rename changes, we need to refresh the
                 * pending changes cache.
                 */
                refreshPendingChangesCache(repository);

                return true;
            }

            /* Sanity check. */
            else {
                throw new RuntimeException(MessageFormat.format(
                    Messages.getString("TFSMoveDeleteHook.InconsistentStateMovingSourceToTargetFormat"), //$NON-NLS-1$
                    source.getFullPath(),
                    target.getFullPath()));
            }
        } finally {
            progressMonitor.done();
        }
    }

    private boolean containsChangeType(final PendingChange[] changes, final ChangeType changeType) {
        if (changes == null) {
            return false;
        }

        for (final PendingChange change : changes) {
            if (change.getChangeType().contains(changeType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean moveFolder(
        final IResourceTree tree,
        final IFolder source,
        final IFolder target,
        final int updateFlags,
        final IProgressMonitor progressMonitor) {
        log.trace(MessageFormat.format("moveFolder: {0} to {1}", source.getFullPath(), target.getFullPath())); //$NON-NLS-1$

        if (repositoryProvider.getRepositoryStatus() == ProjectRepositoryStatus.CONNECTING) {
            throw new RuntimeException(Messages.getString("TFSMoveDeleteHook.ConnectionInProgressWaitBeforeMoving")); //$NON-NLS-1$
        }

        progressMonitor.beginTask(
            MessageFormat.format(
                Messages.getString("TFSMoveDeleteHook.MovingSourceToTargetFormat"), //$NON-NLS-1$
                source.getFullPath(),
                target.getFullPath()),
            20);

        try {
            final MoveResourceInspectionResult moveResult =
                inspectMove(tree, source, target, updateFlags, new SubProgressMonitor(progressMonitor, 5));

            if (moveResult.getStatus() == ResourceInspectionStatus.DEFER) {
                return false;
            }

            /*
             * The source is mapped in TFS but the target is not a TFS-managed
             * project. We need to pend a delete on the source and ignore the
             * target entirely (let the target's SCM, if configured, deal with
             * it.)
             */
            if (moveResult.getOperation() == MoveResourceOperation.DELETE_SOURCE) {
                log.info(
                    MessageFormat.format(
                        "Rename detected from folder {0} to {1} (target not managed by TFS, source will be deleted)", //$NON-NLS-1$
                        source,
                        target));

                final TFSRepository repository = moveResult.getRepository();
                final PendingChange[] pendingChanges = moveResult.getSourcePendingChanges();
                final boolean inServer = moveResult.isSourceInServer();

                final IStatus deleteStatus = deleteFolder(
                    source,
                    repository,
                    inServer,
                    pendingChanges,
                    new SubProgressMonitor(progressMonitor, 10));

                if (!deleteStatus.isOK()) {
                    throw new RuntimeException(deleteStatus.getMessage());
                }

                tree.standardMoveFolder(source, target, updateFlags, new SubProgressMonitor(progressMonitor, 5));
                return true;
            }

            /*
             * The target is mapped in TFS but the source is not a TFS-managed
             * project. We need to pend an add on the target and ignore the
             * source.
             */
            else if (moveResult.getOperation() == MoveResourceOperation.ADD_TARGET) {
                log.info(
                    MessageFormat.format(
                        "Rename detected from folder {0} to {1} (source not managed by TFS, target will be added)", //$NON-NLS-1$
                        source,
                        target));

                final TFSRepository repository = moveResult.getRepository();

                final AddCommand addCommand = new AddCommand(repository, new String[] {
                    target.getLocation().toOSString()
                }, true, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);
                final IStatus addStatus = runCommand(addCommand, new SubProgressMonitor(progressMonitor, 10));

                if (!addStatus.isOK()) {
                    throw new RuntimeException(addStatus.getMessage());
                }

                tree.standardMoveFolder(source, target, updateFlags, new SubProgressMonitor(progressMonitor, 5));
                return true;
            }

            /*
             * Both projects are TFS-managed (or the same project) BUT the
             * source of the rename is a folder that is implicitly added to
             * source control. (Ie, it has a pending add of a file, but there is
             * no pending add for the folder itself.) Special case this and
             * rename only the pending children.
             */
            else if (moveResult.getOperation() == MoveResourceOperation.MOVE
                && moveResult.isSourceInServer() == false) {
                log.info(
                    MessageFormat.format(
                        "Rename detected from folder {0} to {1} (source implicitly added, renaming child pending changes)", //$NON-NLS-1$
                        source,
                        target));

                final TFSRepository repository = moveResult.getRepository();
                final PendingChange[] pendingChanges = moveResult.getSourcePendingChanges();

                final String sourceFolderPath = moveResult.getSourceServerPath();
                final String targetFolderPath = moveResult.getTargetServerPath();

                final String[] sourcePaths = new String[pendingChanges.length];
                final String[] targetPaths = new String[pendingChanges.length];

                for (int i = 0; i < pendingChanges.length; i++) {
                    /* Sanity check */
                    if (!pendingChanges[i].getChangeType().contains(ChangeType.ADD)) {
                        throw new RuntimeException(
                            MessageFormat.format(
                                Messages.getString("TFSMoveDeleteHook.UnexpectedChangeForFileFormat"), //$NON-NLS-1$
                                pendingChanges[i].getChangeType(),
                                pendingChanges[i].getLocalItem()));
                    }

                    sourcePaths[i] = pendingChanges[i].getServerItem();
                    targetPaths[i] =
                        ServerPath.combine(targetFolderPath, ServerPath.makeRelative(sourcePaths[i], sourceFolderPath));
                }

                final RenameMultipleCommand renameCommand = new RenameMultipleCommand(
                    repository,
                    sourcePaths,
                    targetPaths,
                    LockLevel.UNCHANGED,
                    GetOptions.NO_DISK_UPDATE,
                    true,
                    PendChangesOptions.NONE);
                final IStatus renameStatus = runCommand(renameCommand, new SubProgressMonitor(progressMonitor, 10));

                if (!renameStatus.isOK()) {
                    throw new RuntimeException(renameStatus.getMessage());
                }

                tree.standardMoveFolder(source, target, updateFlags, new SubProgressMonitor(progressMonitor, 5));
                return true;
            }

            /*
             * Both projects are TFS-managed (or the same project.) We can
             * safely pend a rename here.
             */
            else if (moveResult.getOperation() == MoveResourceOperation.MOVE) {
                log.info(MessageFormat.format("Rename detected from folder {0} to {1}", source, target)); //$NON-NLS-1$

                final TFSRepository repository = moveResult.getRepository();
                final String sourceServerPath = moveResult.getSourceServerPath();
                final String targetServerPath = moveResult.getTargetServerPath();

                final RenameCommand renameCommand = new RenameCommand(
                    repository,
                    sourceServerPath,
                    targetServerPath,
                    LockLevel.UNCHANGED,
                    GetOptions.NO_DISK_UPDATE,
                    true,
                    PendChangesOptions.NONE);
                final IStatus renameStatus = runCommand(renameCommand, new SubProgressMonitor(progressMonitor, 10));

                if (!renameStatus.isOK()) {
                    throw new RuntimeException(renameStatus.getMessage());
                }

                tree.standardMoveFolder(source, target, updateFlags, new SubProgressMonitor(progressMonitor, 5));

                /*
                 * The server can undo rename changes, we need to refresh the
                 * pending changes cache.
                 */
                refreshPendingChangesCache(repository);

                return true;
            }

            /* Sanity check. */
            else {
                throw new RuntimeException(MessageFormat.format(
                    Messages.getString("TFSMoveDeleteHook.InconsistentStateMovingSourceToTargetFormat"), //$NON-NLS-1$
                    source.getFullPath(),
                    target.getFullPath()));
            }
        } finally {
            progressMonitor.done();
        }
    }

    /*
     * Refresh the pending change cache in the background
     */
    private void refreshPendingChangesCache(final TFSRepository repository) {
        final RefreshPendingChangesCommand refreshCommand = new RefreshPendingChangesCommand(repository);
        final Job refreshJob = new JobCommandAdapter(refreshCommand);

        refreshJob.schedule();
    }

    /*
     * We don't do particularly well at moving projects. We need to revisit this
     * in the future.
     */
    @Override
    public boolean moveProject(
        final IResourceTree tree,
        final IProject source,
        final IProjectDescription target,
        final int updateFlags,
        final IProgressMonitor progressMonitor) {
        log.trace(MessageFormat.format("moveProject: {0}", source.getFullPath())); //$NON-NLS-1$

        progressMonitor.beginTask(
            MessageFormat.format(Messages.getString("TFSMoveDeleteHook.MovingProjectFormat"), source.getFullPath()), //$NON-NLS-1$
            10);

        log.info(MessageFormat.format("Rename detected from project {0} to {1}", source, target)); //$NON-NLS-1$

        try {
            /*
             * Renaming the project only. Does not move folders, only sets the
             * new name in the .project file.
             */
            if (target.getName() != source.getName()) {
                target.setLocation(source.getLocation());
                tree.standardMoveProject(source, target, updateFlags, new SubProgressMonitor(progressMonitor, 10));
                return true;
            } else {
                /*
                 * User is attempting to relocate the project. This gets hairy
                 * and would involve changing WF mappings in order to pend a
                 * rename.
                 */
                tree.failed(
                    new Status(
                        IStatus.ERROR,
                        TFSEclipseClientPlugin.PLUGIN_ID,
                        0,
                        MessageFormat.format(
                            Messages.getString("TFSMoveDeleteHook.MovingProjectNotSupportedInProductFormat"), //$NON-NLS-1$
                            ProductInformation.getCurrent().toString()),
                        null));
                return true;
            }
        } finally {
            progressMonitor.done();
        }
    }

    /**
     * Perform an investigation of the resources involved in a move to determine
     * how to proceed. Returns a ResourceInspectionResult with details about how
     * to proceed (or not to.) Reports errors to the tree.
     *
     * @param tree
     *        The resource tree
     * @param source
     *        The source of the move
     * @param deniedSourceChangeTypes
     *        Change types pended for the source which cause a failure
     * @param target
     *        The target of the move
     * @param deniedTargetChangeTypes
     *        Change types pended for the target which cause a failure
     * @param updateFlags
     *        The update flags in this move operation
     * @param monitor
     *        The process monitor to report to
     * @return A {@link MoveResourceInspectionResult} containing the data
     *         learned. DEFER means to defer to the platform, STOP means to stop
     *         processing due to error, CONTINUE means to continue the
     *         operation.
     */
    private final MoveResourceInspectionResult inspectMove(
        final IResourceTree tree,
        final IResource source,
        final IResource target,
        final int updateFlags,
        final IProgressMonitor progressMonitor) {
        log.trace(MessageFormat.format("inspectMove: {0} to {1}", source.getFullPath(), target.getFullPath())); //$NON-NLS-1$

        progressMonitor.beginTask(
            MessageFormat.format(
                Messages.getString("TFSMoveDeleteHook.InspectingMoveFromSourceToTargetFormat"), //$NON-NLS-1$
                source.getFullPath(),
                target.getFullPath()),
            15);

        try {
            /*
             * The source (or target) may be part of TFS, but the target (or
             * source) may not be. In this case, we need to actually add or
             * delete from TFS, as appropriate.
             */
            final boolean sourceManaged = TeamUtils.isConfiguredWith(source, TFSRepositoryProvider.PROVIDER_ID);
            final boolean targetManaged = TeamUtils.isConfiguredWith(target, TFSRepositoryProvider.PROVIDER_ID);

            /*
             * Sanity check: we should never be called unless we manage the
             * source or target
             */
            if (!sourceManaged && !targetManaged) {
                log.warn(
                    MessageFormat.format(
                        "inspecting resource move from {0} to {1}: neither managed by Team Foundation Server", //$NON-NLS-1$
                        source.getFullPath(),
                        target.getFullPath()));
                return new MoveResourceInspectionResult(ResourceInspectionStatus.DEFER);
            }

            /*
             * Source is managed, target is not. This means that we need to pend
             * a delete on the source to remove it from source control.
             * Investigate the source as such.
             */
            else if (sourceManaged && !targetManaged) {
                final ResourceInspectionResult sourceDetails = inspectResource(
                    tree,
                    source,
                    true,
                    true,
                    DELETE_DENIED_CHANGE_TYPES,
                    updateFlags,
                    false,
                    new SubProgressMonitor(progressMonitor, 15));

                if (!sourceDetails.getStatus().equals(ResourceInspectionStatus.CONTINUE)) {
                    return new MoveResourceInspectionResult(sourceDetails.getStatus());
                }

                final TFSRepository repository = sourceDetails.getRepository();

                return new MoveResourceInspectionResult(
                    repository,
                    sourceDetails.isInServer(),
                    sourceDetails.getPendingChanges(),
                    false,
                    null,
                    MoveResourceOperation.DELETE_SOURCE);
            }

            /*
             * Target is managed, source is not. This means that we need to pend
             * an add on the target to bring it into source control.
             */
            else if (!sourceManaged && targetManaged) {
                final ResourceInspectionResult targetDetails = inspectResource(
                    tree,
                    target,
                    false,
                    true,
                    ADD_DENIED_CHANGE_TYPES,
                    updateFlags,
                    false,
                    new SubProgressMonitor(progressMonitor, 15));

                if (!targetDetails.getStatus().equals(ResourceInspectionStatus.CONTINUE)) {
                    return new MoveResourceInspectionResult(targetDetails.getStatus());
                }

                final TFSRepository repository = targetDetails.getRepository();

                return new MoveResourceInspectionResult(
                    repository,
                    false,
                    null,
                    targetDetails.isInServer(),
                    targetDetails.getPendingChanges(),
                    MoveResourceOperation.ADD_TARGET);
            }

            /*
             * Source and target are both managed. This means that we pend a
             * rename from source to target.
             */
            else {
                /*
                 * Pass deferIgnoredResources=false so ignored resources survive
                 * for testing later in this method (detecting a move across an
                 * ignore boundary).
                 */
                final ResourceInspectionResult sourceDetails = inspectResource(
                    tree,
                    source,
                    true,
                    false,
                    MOVE_DENIED_CHANGE_TYPES,
                    updateFlags,
                    false,
                    new SubProgressMonitor(progressMonitor, 5));
                final ResourceInspectionStatus sourceStatus = sourceDetails.getStatus();

                final ResourceInspectionResult targetDetails = inspectResource(
                    tree,
                    target,
                    false,
                    false,
                    MOVE_DENIED_CHANGE_TYPES,
                    updateFlags,
                    false,
                    new SubProgressMonitor(progressMonitor, 5));
                final ResourceInspectionStatus targetStatus = targetDetails.getStatus();

                /*
                 * Test for deferrals from linked resources (not ignores), but
                 * we can only take action if the source is the deferred item.
                 * If the source is not deferred but the target is, we don't
                 * know how to proceed and should error.
                 */
                if (sourceStatus == ResourceInspectionStatus.DEFER) {
                    log.debug(MessageFormat.format(
                        "Ignoring resource {0} (not in TFS)", //$NON-NLS-1$
                        source.getLocation().toOSString()));
                    return new MoveResourceInspectionResult(ResourceInspectionStatus.DEFER);
                } else if (targetStatus == ResourceInspectionStatus.DEFER) {
                    throw new RuntimeException(
                        MessageFormat.format(
                            Messages.getString("TFSMoveDeleteHook.TargetOfRenameIgnoredByTFSButSourceIsNotFormat"), //$NON-NLS-1$
                            target.getFullPath(),
                            source.getFullPath()));
                }

                /*
                 * Defer moves happening entirely within an ignored area. Moves
                 * across the ignore boundary are pended (otherwise the user
                 * will be seeing conflicts soon).
                 */
                if (sourceDetails.isIgnored() && targetDetails.isIgnored()) {
                    log.debug(MessageFormat.format(
                        "Ignoring resource {0} because both source and target ignored", //$NON-NLS-1$
                        source.getLocation().toOSString()));
                    return new MoveResourceInspectionResult(ResourceInspectionStatus.DEFER);
                }

                /* Sanity check: should never happen */
                if (!sourceDetails.getRepository().equals(targetDetails.getRepository())) {
                    throw new RuntimeException(
                        Messages.getString("TFSMoveDeleteHook.SourceAndTargetAreInDifferentWorkspaces")); //$NON-NLS-1$
                }

                final TFSRepository repository = sourceDetails.getRepository();

                final String sourceServerPath =
                    repository.getWorkspace().getMappedServerPath(source.getLocation().toOSString());
                final String targetServerPath =
                    repository.getWorkspace().getMappedServerPath(target.getLocation().toOSString());

                /* Handle cloaks */
                if (sourceServerPath == null && targetServerPath == null) {
                    log.warn(MessageFormat.format(
                        "Renaming {0} to {1}, both in cloaked folders, ignoring", //$NON-NLS-1$
                        source.getFullPath(),
                        target.getFullPath()));
                    return new MoveResourceInspectionResult(ResourceInspectionStatus.DEFER);
                } else if (sourceServerPath == null) {
                    throw new RuntimeException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("TFSMoveDeleteHook.CannotRenameSourceToTargetBecauseSourceIsCloakedFormat"), //$NON-NLS-1$
                        //@formatter:on
                        source.getFullPath(),
                        target.getFullPath()));
                } else if (targetServerPath == null) {
                    throw new RuntimeException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("TFSMoveDeleteHook.CannotRenameSourceToTargetBecauseTargetIsCloakedFormat"), //$NON-NLS-1$
                        //@formatter:on
                        source.getFullPath(),
                        target.getFullPath()));
                }

                progressMonitor.worked(5);

                return new MoveResourceInspectionResult(
                    repository,
                    sourceDetails.isInServer(),
                    sourceDetails.getPendingChanges(),
                    targetDetails.isInServer(),
                    targetDetails.getPendingChanges(),
                    MoveResourceOperation.MOVE,
                    sourceServerPath,
                    targetServerPath);
            }
        } finally {
            progressMonitor.done();
        }
    }

    /**
     * Performs a first pass sanity check for moving or deleting files or
     * folders. Ensures they're valid resources, managed by us, do not have
     * pending changes that are disallowed, etc.
     *
     * @param tree
     *        The resource tree
     * @param resource
     *        The resource to inspect
     * @param resourceMustExist
     * @param deferIgnoredResources
     *        if <code>true</code> resources which match ignore filters can
     *        result in a DEFER result, if <code>false</code> they are processed
     *        as if they were not ignored and may result in DEFER, CONTINUE, or
     *        STOP. Calls for delete validation usually pass <code>true</code>
     *        so the items can be deferred, whereas move validation will decide
     *        to allow defers independenty for the source and target.
     * @param deniedChangeTypes
     *        If the resource has a change type in ChangeTypes, stops processing
     * @param updateFlags
     *        Update flags
     * @param deletingFolder
     *        if true, additional check for rename refactoring is needed
     * @param monitor
     *        A progress monitor
     * @return A {@link ResourceInspectionResult} containing the data learned.
     *         DEFER means to defer to the platform, STOP means to stop
     *         processing due to error, CONTINUE means to continue the
     *         operation.
     */
    private final ResourceInspectionResult inspectResource(
        final IResourceTree tree,
        final IResource resource,
        final boolean resourceMustExist,
        final boolean deferIgnoredResources,
        final ChangeType deniedChangeTypes,
        final int updateFlags,
        final boolean deletingFolder,
        final IProgressMonitor progressMonitor) {
        progressMonitor.beginTask(MessageFormat.format(
            Messages.getString("TFSMoveDeleteHook.ExaminingResourceFormat"), //$NON-NLS-1$
            resource.getFullPath()), 10);

        try {
            /* File is a linked resource, exit quickly */
            if (resource.getLocation() == null || resource.isLinked(IResource.CHECK_ANCESTORS)) {
                log.info(MessageFormat.format("Cannot move/delete linked resource {0}", resource)); //$NON-NLS-1$
                return new ResourceInspectionResult(ResourceInspectionStatus.DEFER);
            }
            /* Resource has already been removed from the workspace */
            else if (resourceMustExist && !resource.exists()) {
                log.warn(
                    MessageFormat.format("Resource {0} no longer exists in the workspace.", resource.getFullPath())); //$NON-NLS-1$
            }

            final boolean isIgnored = IGNORED_RESOURCES_FILTER.filter(resource).isReject();
            if (deferIgnoredResources && isIgnored) {
                log.debug(MessageFormat.format("Ignoring resource {0}", resource)); //$NON-NLS-1$
                return new ResourceInspectionResult(ResourceInspectionStatus.DEFER);
            }

            final String resourcePath = resource.getLocation().toOSString();

            if (resourceMustExist) {
                final File resourceFile = new File(resourcePath);

                /* Ensure that the file exists on disk */
                if (!resourceFile.exists()) {
                    log.warn(MessageFormat.format("Resource {0} no longer exists on disk", resourcePath)); //$NON-NLS-1$
                }

                /*
                 * Ensure that the type in the tree is the same type as on the
                 * file system
                 */
                if (resource instanceof IFile && resourceFile.isDirectory()) {
                    throw new RuntimeException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("TFSMoveDeleteHook.ResourceExistsAsFileInWorkspaceButFolderOnDiskFormat"), //$NON-NLS-1$
                        //@formatter:on
                        resource.getFullPath()));
                } else if (!(resource instanceof IFile) && resourceFile.isFile()) {
                    throw new RuntimeException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("TFSMoveDeleteHook.ResourceExistsAsFolderInWorkspaceButFileOnDiskFormat"), //$NON-NLS-1$
                        //@formatter:on
                        resource.getFullPath()));
                }
            }

            /* See if the tree is in sync */
            final int depth = (resource instanceof IFile) ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
            if ((updateFlags & IResource.FORCE) != IResource.FORCE && !tree.isSynchronized(resource, depth)) {
                throw new RuntimeException(
                    MessageFormat.format(
                        Messages.getString("TFSMoveDeleteHook.ResourceNotInSyncWithLocalFileSystemFormat"), //$NON-NLS-1$
                        resource.getFullPath()));
            }

            /* See if we're online */
            final ResourceRepositoryMap repositoryMap = PluginResourceHelpers.mapResources(new IResource[] {
                resource
            });
            final TFSRepository repository = repositoryMap.getRepository(resource);

            /* Offline */
            if (repository == null) {
                log.info(
                    MessageFormat.format(
                        "Repository is offline, delete for {0} must be reconciled when returning online to Team Foundation Server.", //$NON-NLS-1$
                        resource.getFullPath()));

                return new ResourceInspectionResult(ResourceInspectionStatus.DEFER);
            } else if (!repository.equals(repositoryProvider.getRepository())) {
                log.warn(
                    MessageFormat.format(
                        "Repository for resource {0} mapped to {1} but repository provider returned {2}", //$NON-NLS-1$
                        resource.getFullPath(),
                        repository,
                        repositoryProvider.getRepository()));
            }

            /*
             * Test if this item has an invalid TFS name/path (which would cause
             * errors in the queries below).
             */
            try {
                repository.getWorkspace().getMappedServerPath(resourcePath);
            } catch (final InputValidationException e) {
                log.warn(MessageFormat.format("Ignoring resource {0} because it is not a valid TFS path", resource), e); //$NON-NLS-1$
                return new ResourceInspectionResult(ResourceInspectionStatus.DEFER);
            }

            /*
             * See if the resource exists in the ResourceDataManager (ie, if it
             * exists in TFS). If the project exists in TFS but the file is not,
             * then ResourceDataManager.hasCompletedRefresh will be true but
             * ResourceDataManager.getResourceData will be null.
             */
            final boolean needsQuery;

            if (resourceMustExist) {
                final ResourceDataManager resourceDataManager =
                    TFSEclipseClientPlugin.getDefault().getResourceDataManager();

                if (!resourceDataManager.hasCompletedRefresh(resource.getProject())
                    || resourceDataManager.getResourceData(resource) == null) {
                    needsQuery = true;
                } else {
                    needsQuery = false;
                }
            } else {
                needsQuery = false;
            }

            /*
             * This is a particularly complicated and ugly work-around for
             * server workspaces and Eclipse's copy and paste behavior.
             *
             * JDT (and probably others) implements "paste" that overwrites an
             * existing file by:
             *
             * 1. Pending an edit on the target
             *
             * 2. Deleting the target
             *
             * 3. Copying the source to the target
             *
             * In a server workspace, the user may have the
             * "check out files in background" option enabled, which uses
             * another thread to do the checkout. The problem is that we may
             * arrive at this point in this method before the background thread
             * (started at step #1) has completed the checkout. Querying the
             * pending changes might miss the "edit" change that we need to know
             * about to do the MoveDeleteHook work.
             *
             * So we we must wait if the file is being checked out in the
             * background.
             */
            if (repository.getWorkspace().getLocation() == WorkspaceLocation.SERVER && resource instanceof IFile) {
                final TFSFileModificationValidator validator = repositoryProvider.getTFSFileModificationValidator();

                try {
                    new ExtensionPointAsyncObjectWaiter().waitUntilTrue(new IAsyncObjectWaiter.Predicate() {
                        @Override
                        public boolean isTrue() {
                            return !validator.isCheckingOutFileInBackground((IFile) resource);
                        }
                    });
                } catch (final InterruptedException e) {
                    throw new VersionControlException(
                        MessageFormat.format(
                            Messages.getString("TFSMoveDeleteHook.InterruptedWaitingForBackgroundCheckoutFormat"), //$NON-NLS-1$
                            resource.getName()));
                }
            }

            final List<PendingChange> pendingChanges = getNestedPendingChanges(repository, resourcePath);
            log.info("Nested pending changes count: [" + String.valueOf(pendingChanges.size()) + "]"); //$NON-NLS-1$ //$NON-NLS-2$

            if (deletingFolder) {
                /*
                 * Test if there is a pending rename with the source path
                 * including the item's path. That happens when Eclipse cleans
                 * up empty folders left after a package renaming. We don't need
                 * to do anything and skip the operation in the Eclipse
                 * platform.
                 */
                final String serverPath = repository.getWorkspace().getMappedServerPath(resourcePath);
                log.info("Local folder to be deleted: " + resourcePath); //$NON-NLS-1$
                log.info("Server folder to be deleted: " + serverPath); //$NON-NLS-1$

                boolean reject = false;
                for (final PendingChange change : pendingChanges) {
                    if (change.getChangeType().contains(ChangeType.RENAME)
                        && !isSamePendingChange(serverPath, resourcePath, change)) {
                        log.info("Child pending change:"); //$NON-NLS-1$
                        log.info("  ChangeType: " + change.getChangeType().toString()); //$NON-NLS-1$
                        log.info("    ServerItem: " + change.getServerItem()); //$NON-NLS-1$
                        log.info("    LocalItem: " + (change.getLocalItem() == null ? "null" : change.getLocalItem())); //$NON-NLS-1$ //$NON-NLS-2$
                        log.info("    SourceLocalItem: " //$NON-NLS-1$
                            + (change.getSourceLocalItem() == null ? "null" : change.getSourceLocalItem())); //$NON-NLS-1$
                        log.info("    SourceServerItem: " //$NON-NLS-1$
                            + (change.getSourceServerItem() == null ? "null" : change.getSourceServerItem())); //$NON-NLS-1$
                        log.info("    Date: " //$NON-NLS-1$
                            + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z").format( //$NON-NLS-1$
                                change.getWebServiceObject().getDate().getTime()));

                        reject = true;
                    }
                }

                if (reject) {
                    progressMonitor.worked(1);

                    return new ResourceInspectionResult(
                        ResourceInspectionStatus.WARN,
                        repository,
                        true,
                        pendingChanges.toArray(new PendingChange[pendingChanges.size()]),
                        isIgnored,
                        Messages.getString("TFSMoveDeleteHook.CannotDeleteFolder")); //$NON-NLS-1$
                }
            }

            /*
             * If there are pending changes, see if they are of a disallowed
             * type.
             */
            boolean reject = false;
            for (final PendingChange change : pendingChanges) {
                if (change.getChangeType().containsAny(deniedChangeTypes)) {
                    log.info("  ChangeType: " + change.getChangeType().toString()); //$NON-NLS-1$
                    log.info("    ServerItem: " + change.getServerItem()); //$NON-NLS-1$
                    log.info("    LocalItem: " + (change.getLocalItem() == null ? "null" : change.getLocalItem())); //$NON-NLS-1$ //$NON-NLS-2$
                    log.info("    SourceLocalItem: " //$NON-NLS-1$
                        + (change.getSourceLocalItem() == null ? "null" : change.getSourceLocalItem())); //$NON-NLS-1$
                    log.info("    SourceServerItem: " //$NON-NLS-1$
                        + (change.getSourceServerItem() == null ? "null" : change.getSourceServerItem())); //$NON-NLS-1$
                    log.info("    Date: " //$NON-NLS-1$
                        + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z").format( //$NON-NLS-1$
                            change.getWebServiceObject().getDate().getTime()));

                    reject = true;
                }
            }

            if (reject) {
                log.warn(MessageFormat.format(
                    //@formatter:off
                    Messages.getString("TFSMoveDeleteHook.ResourceHasPendingChangePleaseUndoBeforeContinuingFormat"), //$NON-NLS-1$
                    //@formatter:on
                    resource.getLocation().toOSString()));

                progressMonitor.worked(1);

                throw new RuntimeException(MessageFormat.format(
                    //@formatter:off
                    Messages.getString("TFSMoveDeleteHook.ResourceHasPendingChangePleaseUndoBeforeContinuingFormat"), //$NON-NLS-1$
                    //@formatter:on
                    resource.getLocation().toOSString()));
            }

            /*
             * If there are no pending changes and we didn't have any
             * ResourceData for this resource, we need to query the server to
             * make sure the item exists.
             */
            boolean inServer = true;

            if (needsQuery && WorkspaceLocation.LOCAL.equals(repository.getWorkspace().getLocation())) {
                /*
                 * Local workspaces should query extended items to avoid hitting
                 * the server.
                 */
                final QueryItemsExtendedCommand queryCommand =
                    new QueryItemsExtendedCommand(repository, new ItemSpec[] {
                        new ItemSpec(resourcePath, RecursionType.NONE)
                }, DeletedState.NON_DELETED, ItemType.ANY, GetItemsOptions.LOCAL_ONLY);

                final IStatus queryStatus = runCommand(queryCommand, new SubProgressMonitor(progressMonitor, 9));

                if (!queryStatus.isOK()) {
                    throw new RuntimeException(queryStatus.getMessage());
                }

                final ExtendedItem[][] items = queryCommand.getItems();

                if (items == null || items.length == 0 || items[0] == null || items[0].length == 0) {
                    /*
                     * The item does not exist on the server (and is not an
                     * implicit add.)
                     */
                    return new ResourceInspectionResult(ResourceInspectionStatus.DEFER);
                }
            } else if (needsQuery) {
                final QueryItemsCommand queryCommand = new QueryItemsCommand(
                    repository,
                    new ItemSpec[] {
                        new ItemSpec(resourcePath, RecursionType.NONE)
                },
                    new WorkspaceVersionSpec(repository.getWorkspace()),
                    DeletedState.NON_DELETED,
                    ItemType.ANY,
                    GetItemsOptions.NONE);

                final IStatus queryStatus = runCommand(queryCommand, new SubProgressMonitor(progressMonitor, 9));

                if (!queryStatus.isOK()) {
                    throw new RuntimeException(queryStatus.getMessage());
                }

                final ItemSet[] itemSets = queryCommand.getItemSets();

                /* Item does not exist in TFS */
                if (itemSets == null
                    || itemSets.length == 0
                    || itemSets[0].getItems() == null
                    || itemSets[0].getItems().length == 0) {
                    /*
                     * Special case: pending adds of files may implicitly add a
                     * folder. If this file does not exist on the server and is
                     * not an implicit add, ignore it.
                     */
                    if (pendingChanges.size() == 0) {
                        return new ResourceInspectionResult(ResourceInspectionStatus.DEFER);
                    }

                    /*
                     * An implicit add that does not exist on the server.
                     */
                    inServer = false;
                }
            }

            /* Allow the deleteFile/deleteFolder methods to continue. */
            progressMonitor.worked(1);
            return new ResourceInspectionResult(
                repository,
                inServer,
                pendingChanges.toArray(new PendingChange[pendingChanges.size()]),
                isIgnored);
        } finally {
            progressMonitor.done();
        }
    }

    private List<PendingChange> getNestedPendingChanges(final TFSRepository repository, final String resourcePath) {
        final PendingChange[] allPendingChanges = repository.getPendingChangeCache().getPendingChanges();
        final String serverPath = repository.getWorkspace().getMappedServerPath(resourcePath);
        final List<PendingChange> nestedPendingChanges = new ArrayList<PendingChange>();

        for (int i = 0; i < allPendingChanges.length; i++) {
            final PendingChange change = allPendingChanges[i];
            if (change.getChangeType().containsAny(ChangeType.RENAME)) {
                if (change.getSourceLocalItem() == null || change.getSourceServerItem() == null) {
                    change.updateMissingProperties(repository.getVersionControlClient());
                }
            }

            if (isNestedPendingChange(serverPath, resourcePath, change)) {
                nestedPendingChanges.add(change);
            }
        }

        return nestedPendingChanges;
    }

    private boolean isNestedPendingChange(final String serverPath, final String localPath, final PendingChange change) {
        if (change.getLocalItem() != null && LocalPath.isChild(localPath, change.getLocalItem())) {
            return true;
        }

        if (change.getServerItem() != null && ServerPath.isChild(serverPath, change.getServerItem())) {
            return true;
        }

        if (change.getSourceLocalItem() != null && LocalPath.isChild(localPath, change.getSourceLocalItem())) {
            return true;
        }

        if (change.getSourceServerItem() != null && ServerPath.isChild(serverPath, change.getSourceServerItem())) {
            return true;
        }

        return false;
    }

    private boolean isSamePendingChange(final String serverPath, final String localPath, final PendingChange change) {
        if (change.getLocalItem() != null && LocalPath.equals(localPath, change.getLocalItem())) {
            return true;
        }

        if (change.getServerItem() != null && ServerPath.equals(serverPath, change.getServerItem())) {
            return true;
        }

        if (change.getSourceLocalItem() != null && LocalPath.equals(localPath, change.getSourceLocalItem())) {
            return true;
        }

        if (change.getSourceServerItem() != null && ServerPath.equals(serverPath, change.getSourceServerItem())) {
            return true;
        }

        return false;
    }

    /**
     * Safety mechanism for executing commands. Provides try/catch logic to
     * always return an IStatus.
     *
     * @param command
     * @param progressMonitor
     * @return
     */
    private final IStatus runCommand(final Command command, final IProgressMonitor progressMonitor) {
        final CommandExecutor commandExecutor = new CommandExecutor(progressMonitor);

        return commandExecutor.execute(command);
    }

    private static class ResourceInspectionResult {
        private final ResourceInspectionStatus status;
        private final TFSRepository repository;
        private final boolean inServer;
        private final PendingChange[] pendingChanges;
        private final boolean ignored;
        private final String message;

        /**
         * This constructor is only valid for
         * {@link ResourceInspectionStatus#DEFER} and
         * {@link ResourceInspectionStatus#STOP}.
         *
         * @param status
         */
        public ResourceInspectionResult(final ResourceInspectionStatus status) {
            Check.notNull(status, "status"); //$NON-NLS-1$
            Check.isTrue(
                status.equals(ResourceInspectionStatus.DEFER) || status.equals(ResourceInspectionStatus.WARN),
                "status == DEFER || status == WARN"); //$NON-NLS-1$

            this.status = status;
            repository = null;
            inServer = false;
            pendingChanges = null;
            ignored = false;
            message = null;
        }

        public ResourceInspectionResult(
            final TFSRepository repository,
            final boolean inServer,
            final PendingChange[] pendingChanges,
            final boolean ignored) {
            this(ResourceInspectionStatus.CONTINUE, repository, inServer, pendingChanges, ignored, null);
        }

        public ResourceInspectionResult(
            final ResourceInspectionStatus status,
            final TFSRepository repository,
            final boolean inServer,
            final PendingChange[] pendingChanges,
            final boolean ignored,
            final String message) {
            Check.notNull(repository, "repository"); //$NON-NLS-1$

            this.status = status;
            this.repository = repository;
            this.inServer = inServer;
            this.pendingChanges = pendingChanges;
            this.ignored = ignored;
            this.message = message;
        }

        public final ResourceInspectionStatus getStatus() {
            return status;
        }

        public final TFSRepository getRepository() {
            return repository;
        }

        public final boolean isInServer() {
            return inServer;
        }

        public final PendingChange[] getPendingChanges() {
            return pendingChanges;
        }

        public boolean isIgnored() {
            return ignored;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final class MoveResourceInspectionResult {
        private final ResourceInspectionStatus status;
        private final TFSRepository repository;
        private final MoveResourceOperation operation;
        private final boolean sourceInServer;
        private final String sourceServerPath;
        private final PendingChange[] sourcePendingChanges;
        private final String targetServerPath;

        public MoveResourceInspectionResult(final ResourceInspectionStatus status) {
            Check.notNull(status, "status"); //$NON-NLS-1$

            this.status = status;
            repository = null;
            operation = MoveResourceOperation.NONE;
            sourceInServer = false;
            sourceServerPath = null;
            sourcePendingChanges = new PendingChange[0];
            targetServerPath = null;
        }

        public MoveResourceInspectionResult(
            final TFSRepository repository,
            final boolean sourceInServer,
            final PendingChange[] sourcePendingChanges,
            final boolean targetInServer,
            final PendingChange[] targetPendingChanges,
            final MoveResourceOperation operation) {
            Check.notNull(repository, "repository"); //$NON-NLS-1$
            Check.notNull(operation, "operation"); //$NON-NLS-1$
            Check.isTrue(
                operation.equals(MoveResourceOperation.DELETE_SOURCE)
                    || operation.equals(MoveResourceOperation.ADD_TARGET),
                "operation == DELETE_SOURCE || operation == ADD_TARGET"); //$NON-NLS-1$

            status = ResourceInspectionStatus.CONTINUE;
            this.repository = repository;
            this.operation = operation;
            this.sourceInServer = sourceInServer;
            sourceServerPath = null;
            this.sourcePendingChanges = sourcePendingChanges;
            targetServerPath = null;
        }

        public MoveResourceInspectionResult(
            final TFSRepository repository,
            final boolean sourceInServer,
            final PendingChange[] sourcePendingChanges,
            final boolean targetInServer,
            final PendingChange[] targetPendingChanges,
            final MoveResourceOperation operation,
            final String sourceServerPath,
            final String targetServerPath) {
            Check.notNull(repository, "repository"); //$NON-NLS-1$
            Check.notNull(operation, "operation"); //$NON-NLS-1$
            Check.isTrue(operation.equals(MoveResourceOperation.MOVE), "operation == MOVE"); //$NON-NLS-1$
            Check.notNull(sourceServerPath, "sourceServerPath"); //$NON-NLS-1$
            Check.notNull(targetServerPath, "targetServerPath"); //$NON-NLS-1$

            status = ResourceInspectionStatus.CONTINUE;
            this.repository = repository;
            this.operation = operation;
            this.sourceInServer = sourceInServer;
            this.sourceServerPath = sourceServerPath;
            this.sourcePendingChanges = sourcePendingChanges;
            this.targetServerPath = targetServerPath;
        }

        public final ResourceInspectionStatus getStatus() {
            return status;
        }

        public final TFSRepository getRepository() {
            return repository;
        }

        public MoveResourceOperation getOperation() {
            return operation;
        }

        public boolean isSourceInServer() {
            return sourceInServer;
        }

        public String getSourceServerPath() {
            return sourceServerPath;
        }

        public PendingChange[] getSourcePendingChanges() {
            return sourcePendingChanges;
        }

        public String getTargetServerPath() {
            return targetServerPath;
        }
    }

    /**
     * Deletion status results from the inspection of
     * {@link TFSMoveDeleteHook#inspectDelete(IResourceTree, IResource, int, IProgressMonitor)}
     *
     * @threadsafety thread safe
     */
    private final static class ResourceInspectionStatus extends TypesafeEnum {
        /**
         * Do not process deletion, instead defer to the Eclipse platform
         * default.
         */
        public final static ResourceInspectionStatus DEFER = new ResourceInspectionStatus(0);

        /*
         * Deletion is outside the scope of TFS, do not perform any work.
         *
         * deprecated: throw a RuntimeException instead.
         */
        /*
         * public final static ResourceInspectionStatus STOP = new
         * ResourceInspectionStatus(1);
         */

        /**
         * Deletion should be pended to Team Foundation Server.
         */
        public final static ResourceInspectionStatus CONTINUE = new ResourceInspectionStatus(2);

        /**
         * Deletion should be done later by a separate user's request.
         */
        public final static ResourceInspectionStatus WARN = new ResourceInspectionStatus(3);

        private ResourceInspectionStatus(final int value) {
            super(value);
        }
    }

    private final static class MoveResourceOperation extends TypesafeEnum {
        public final static MoveResourceOperation NONE = new MoveResourceOperation(0);
        public final static MoveResourceOperation MOVE = new MoveResourceOperation(1);
        public final static MoveResourceOperation DELETE_SOURCE = new MoveResourceOperation(2);
        public final static MoveResourceOperation ADD_TARGET = new MoveResourceOperation(3);

        private MoveResourceOperation(final int value) {
            super(value);
        }
    }
}