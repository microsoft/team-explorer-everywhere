// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
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

public class RenameMultipleCommand extends TFSCommand {
    private final TFSRepository repository;
    private final String[] sourceServerPaths;
    private final String[] targetServerPaths;
    private final LockLevel lockLevel;
    private final GetOptions getOptions;
    private final boolean detectTargetItemType;
    private final PendChangesOptions pendOptions;

    private final NonFatalCommandHelper nonFatalHelper;

    private boolean targetFolderMustExist = false;

    private int renameCount;

    public RenameMultipleCommand(
        final TFSRepository repository,
        final String[] sourceServerPaths,
        final String[] targetServerPaths) {
        this(
            repository,
            sourceServerPaths,
            targetServerPaths,
            LockLevel.UNCHANGED,
            GetOptions.NONE,
            true,
            PendChangesOptions.NONE);
    }

    public RenameMultipleCommand(
        final TFSRepository repository,
        final String[] sourceServerPaths,
        final String[] targetServerPaths,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final boolean detectTargetItemType,
        final PendChangesOptions pendOptions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(sourceServerPaths, "sourceServerPaths"); //$NON-NLS-1$
        Check.notNullOrEmpty(targetServerPaths, "targetServerPaths"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendOptions, "pendOptions"); //$NON-NLS-1$
        Check.isTrue(
            sourceServerPaths.length == targetServerPaths.length,
            "sourceServerPaths.length == targetServerPaths.length"); //$NON-NLS-1$

        this.repository = repository;
        this.sourceServerPaths = sourceServerPaths;
        this.targetServerPaths = targetServerPaths;
        this.lockLevel = lockLevel;
        this.getOptions = getOptions;
        this.detectTargetItemType = detectTargetItemType;
        this.pendOptions = pendOptions;

        nonFatalHelper = new NonFatalCommandHelper(repository);
    }

    @Override
    public String getName() {
        if (sourceServerPaths.length == 1) {
            final String messageFormat = Messages.getString("RenameMultipleCommand.RenamingToFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, sourceServerPaths[0], targetServerPaths[0]);
        } else {
            final String messageFormat = Messages.getString("RenameMultipleCommand.RenamingFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, sourceServerPaths.length);
        }
    }

    @Override
    public String getErrorDescription() {
        if (sourceServerPaths.length == 1) {
            final String messageFormat = Messages.getString("RenameMultipleCommand.RenameErrorFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, sourceServerPaths[0]);
        } else {
            final String messageFormat = Messages.getString("RenameMultipleCommand.RenameErrorFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, sourceServerPaths.length);
        }
    }

    @Override
    public String getLoggingDescription() {
        if (sourceServerPaths.length == 1) {
            final String messageFormat = Messages.getString("RenameMultipleCommand.RenamingToFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, sourceServerPaths[0], targetServerPaths[0]);
        } else {
            final String messageFormat = Messages.getString("RenameMultipleCommand.RenamingFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, sourceServerPaths.length);
        }
    }

    public void setTargetFolderMustExist(final boolean targetFolderMustExist) {
        this.targetFolderMustExist = targetFolderMustExist;
    }

    public boolean isTargetFolderMustExist() {
        return targetFolderMustExist;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), sourceServerPaths.length + 1);

        /* Cache seen target server parents */
        final Set targetServerParentSet = new HashSet();

        /* Analyze server paths */
        for (int i = 0; i < sourceServerPaths.length; i++) {
            final String sourceServerPath = sourceServerPaths[i];
            final String targetServerPath = targetServerPaths[i];

            /*
             * check to see if we're trying to move an item inside itself, which
             * is not possible
             */
            if (ServerPath.isDirectChild(sourceServerPath, targetServerPath)) {
                /* wording taken from Microsoft's TFC */
                final String messageFormat =
                    Messages.getString("RenameMultipleCommand.TargetCannotBeUnderSourceItemFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, targetServerPath, sourceServerPath);
                return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
            }

            final String targetFolderPath = ServerPath.getParent(targetServerPath);

            /* Already seen this path, don't bother querying the server. */
            if (targetServerParentSet.contains(targetFolderPath)) {
                continue;
            }

            /*
             * Ensure that the destination folder exists
             */
            String messageFormat = Messages.getString("RenameMultipleCommand.ExaminingParentOfDestinationFormat"); //$NON-NLS-1$
            String message = MessageFormat.format(messageFormat, targetFolderPath);
            final Item targetFolderItem = queryItem(progressMonitor, message, targetFolderPath);

            if (targetFolderMustExist && targetFolderItem == null) {
                messageFormat = Messages.getString("RenameMultipleCommand.FolderDoesNotExistFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, targetFolderPath);
                return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
            }

            if (targetFolderItem != null && targetFolderItem.getItemType() != ItemType.FOLDER) {
                messageFormat = Messages.getString("RenameMultipleCommand.ItemIsNotFolderFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, targetFolderPath);
                return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
            }

            /*
             * Make sure the target server path is mapped.
             */

            if (repository.getWorkspace().isServerPathMapped(targetFolderPath) == false) {
                messageFormat = Messages.getString("RenameMultipleCommand.PathMustBeMappedFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, targetFolderPath);
                return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
            }

            targetServerParentSet.add(targetFolderPath);
        }

        /* Do the rename */
        nonFatalHelper.hookupListener();

        try {
            final String messageFormat = Messages.getString("RenameMultipleCommand.RenamingItemsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, sourceServerPaths.length);
            progressMonitor.subTask(message);

            renameCount = repository.getWorkspace().pendRename(
                sourceServerPaths,
                targetServerPaths,
                lockLevel,
                getOptions,
                detectTargetItemType,
                pendOptions);

            progressMonitor.worked(1);
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (renameCount == 0) {
            return nonFatalHelper.getBestStatus(
                IStatus.ERROR,
                1,
                Messages.getString("RenameMultipleCommand.FilesCouldNotBeRenamedFormat")); //$NON-NLS-1$
        } else if (nonFatalHelper.getStatuses().length > 0) {
            return nonFatalHelper.getBestStatus(
                IStatus.WARNING,
                1,
                Messages.getString("RenameMultipleCommand.FilesCouldNotBeRenamedFormat")); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }

    private Item queryItem(final IProgressMonitor progressMonitor, final String title, final String serverPath)
        throws Exception {
        final SubProgressMonitor queryMonitor = new SubProgressMonitor(progressMonitor, 1);
        final QueryItemsCommand queryCommand = new QueryItemsCommand(repository, new ItemSpec[] {
            new ItemSpec(serverPath, RecursionType.NONE)
        }, LatestVersionSpec.INSTANCE, DeletedState.NON_DELETED, ItemType.ANY, GetItemsOptions.UNSORTED);
        queryCommand.setName(title);

        final IStatus queryStatus = queryCommand.run(queryMonitor);

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

        return subItems[0];
    }

    public int getRenameCount() {
        return renameCount;
    }
}