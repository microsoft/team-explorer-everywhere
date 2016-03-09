// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.util.ProgressMonitorTaskMonitorAdapter;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

public class RenameCommand extends TFSCommand {
    public static final CodeMarker RENAME_COMMAND_FINISHED =
        new CodeMarker("com.microsoft.tfs.client.common.commands.vc.RenameCommand#renameFinished"); //$NON-NLS-1$

    private final TFSRepository repository;
    private final String sourceServerPath;
    private final String targetServerPath;
    private final LockLevel lockLevel;
    private final GetOptions getOptions;
    private final boolean detectTargetItemType;
    private final PendChangesOptions pendOptions;

    private final NonFatalCommandHelper nonFatalHelper;

    private boolean targetFolderMustExist = false;

    private int renameCount;

    public RenameCommand(final TFSRepository repository, final String sourceServerPath, final String targetServerPath) {
        this(
            repository,
            sourceServerPath,
            targetServerPath,
            LockLevel.UNCHANGED,
            GetOptions.NONE,
            true,
            PendChangesOptions.NONE);
    }

    public RenameCommand(
        final TFSRepository repository,
        final String sourceServerPath,
        final String targetServerPath,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final boolean detectTargetItemType,
        final PendChangesOptions pendOptions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(sourceServerPath, "sourceServerPath"); //$NON-NLS-1$
        Check.notNull(targetServerPath, "targetServerPath"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendOptions, "pendOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.sourceServerPath = sourceServerPath;
        this.targetServerPath = targetServerPath;
        this.lockLevel = lockLevel;
        this.getOptions = getOptions;
        this.detectTargetItemType = detectTargetItemType;
        this.pendOptions = pendOptions;

        nonFatalHelper = new NonFatalCommandHelper(repository);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("RenameCommand.RenamingToFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, sourceServerPath, targetServerPath);
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("RenameCommand.ErrorRenamingFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, sourceServerPath);
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("RenameCommand.RenamingToFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, sourceServerPath, targetServerPath);
    }

    public void setTargetFolderMustExist(final boolean targetFolderMustExist) {
        this.targetFolderMustExist = targetFolderMustExist;
    }

    public boolean isTargetFolderMustExist() {
        return targetFolderMustExist;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        // One unit for the destination folder check, one for the core rename.
        // Use null main task name so core doesn't get it prepended to the
        // useful "Getting [path]..." messages.
        progressMonitor.beginTask(null, 2);
        progressMonitor.setTaskName(getName());

        if (sourceServerPath.equals(targetServerPath)) {
            return new Status(
                IStatus.ERROR,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("RenameCommand.ServerPathsAreSame"), //$NON-NLS-1$
                null);
        } else if (ServerPath.equals(sourceServerPath, targetServerPath)) {
        }

        /*
         * check to see if we're trying to move an item inside itself, which is
         * not possible
         */
        if (ServerPath.isDirectChild(sourceServerPath, targetServerPath)) {
            /* wording taken from Microsoft's TFC */
            final String messageFormat = Messages.getString("RenameCommand.TargetItemCannotBeUnderSourceFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, targetServerPath, sourceServerPath);
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
        }

        final String targetFolderPath = ServerPath.getParent(targetServerPath);

        /*
         * Ensure that the destination folder exists
         */
        String messageFormat = Messages.getString("RenameCommand.ExaminingParentOfDestinationFormat"); //$NON-NLS-1$
        String message = MessageFormat.format(messageFormat, targetFolderPath);

        final IProgressMonitor queryMonitor = new SubProgressMonitor(progressMonitor, 1);
        final ItemType targetFolderItemType = queryItemType(queryMonitor, message, targetFolderPath);

        if (targetFolderMustExist && targetFolderItemType == null) {
            messageFormat = Messages.getString("RenameCommand.ItemDoesNotExistFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, targetFolderPath);
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
        }

        if (targetFolderItemType != null && targetFolderItemType != ItemType.FOLDER) {
            messageFormat = Messages.getString("RenameCommand.ItemIsNotFolderFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, targetFolderPath);
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
        }

        /*
         * Make sure the target server path is mapped.
         */

        if (repository.getWorkspace().isServerPathMapped(targetFolderPath) == false) {
            messageFormat = Messages.getString("RenameCommand.ParentOfDestinationMustBeMappedFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, targetFolderPath);
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
        }

        /* Do the rename */
        nonFatalHelper.hookupListener();

        try {
            try {
                final IProgressMonitor coreMonitor = new SubProgressMonitor(progressMonitor, 1);
                coreMonitor.setTaskName(getName());
                TaskMonitorService.pushTaskMonitor(new ProgressMonitorTaskMonitorAdapter(coreMonitor));

                renameCount = repository.getWorkspace().pendRename(
                    sourceServerPath,
                    targetServerPath,
                    lockLevel,
                    getOptions,
                    detectTargetItemType,
                    pendOptions);
            } finally {
                // Calls done() and works 1
                TaskMonitorService.popTaskMonitor(true);

                nonFatalHelper.unhookListener();
            }
        } finally {
            nonFatalHelper.unhookListener();
        }

        CodeMarkerDispatch.dispatch(RENAME_COMMAND_FINISHED);

        if (renameCount == 0) {
            return nonFatalHelper.getBestStatus(
                IStatus.ERROR,
                1,
                Messages.getString("RenameCommand.FilesCouldNotBeRenamedFormat")); //$NON-NLS-1$
        } else if (nonFatalHelper.getStatuses().length > 0) {
            return nonFatalHelper.getBestStatus(
                IStatus.WARNING,
                1,
                Messages.getString("RenameCommand.FilesCouldNotBeRenamedFormat")); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }

    private ItemType queryItemType(final IProgressMonitor progressMonitor, final String title, final String serverPath)
        throws Exception {
        if (repository.getWorkspace().getLocation().equals(WorkspaceLocation.LOCAL)) {
            return queryLocalItemType(progressMonitor, title, serverPath);
        } else {
            return queryServerItemType(progressMonitor, title, serverPath);
        }
    }

    private ItemType queryLocalItemType(
        final IProgressMonitor progressMonitor,
        final String title,
        final String serverPath) throws Exception {
        final QueryItemsExtendedCommand queryCommand = new QueryItemsExtendedCommand(
            repository,
            new ItemSpec(serverPath, RecursionType.NONE),
            DeletedState.NON_DELETED,
            ItemType.ANY,
            GetItemsOptions.LOCAL_ONLY);
        queryCommand.setName(title);

        final IStatus queryStatus = queryCommand.run(progressMonitor);
        progressMonitor.worked(1);

        if (!queryStatus.isOK()) {
            throw new CoreException(queryStatus);
        }

        if (queryCommand.getItems() == null
            || queryCommand.getItems().length != 1
            || queryCommand.getItems()[0].length != 1) {
            return null;
        }

        return queryCommand.getItems()[0][0].getItemType();
    }

    private ItemType queryServerItemType(
        final IProgressMonitor progressMonitor,
        final String title,
        final String serverPath) throws Exception {
        // Must give a fresh sub monitor to the query command
        final QueryItemsCommand queryCommand = new QueryItemsCommand(repository, new ItemSpec[] {
            new ItemSpec(serverPath, RecursionType.NONE)
        }, LatestVersionSpec.INSTANCE, DeletedState.NON_DELETED, ItemType.ANY, GetItemsOptions.UNSORTED);
        queryCommand.setName(title);

        final IStatus queryStatus = queryCommand.run(progressMonitor);
        progressMonitor.worked(1);

        if (!queryStatus.isOK()) {
            throw new CoreException(queryStatus);
        }

        if (queryCommand.getItemSets() == null || queryCommand.getItemSets().length != 1) {
            return null;
        }

        final Item[] subItems = queryCommand.getItemSets()[0].getItems();

        if (subItems == null || subItems.length != 1) {
            return null;
        }

        return subItems[0].getItemType();
    }

    public int getRenameCount() {
        return renameCount;
    }
}
