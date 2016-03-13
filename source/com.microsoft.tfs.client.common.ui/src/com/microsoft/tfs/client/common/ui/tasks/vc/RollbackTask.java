// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.commands.vc.RollbackCommand;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.RollbackItemDialog;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemFactory;
import com.microsoft.tfs.core.clients.versioncontrol.RollbackOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class RollbackTask extends BaseTask {
    private final TFSRepository repository;
    private String itemPath;
    private boolean showDialog;
    private VersionSpec versionFrom;
    private VersionSpec versionTo;
    private RecursionType recursionType;
    private RollbackOptions rollbackOptions;

    public RollbackTask(final Shell shell, final TFSRepository repository, final String itemPath) {
        this(shell, repository, itemPath, null, null);
        showDialog = true;
    }

    public RollbackTask(
        final Shell shell,
        final TFSRepository repository,
        final String itemPath,
        final VersionSpec versionFrom) {
        this(shell, repository, itemPath, versionFrom, versionFrom);
        showDialog = false;
    }

    public RollbackTask(
        final Shell shell,
        final TFSRepository repository,
        final String itemPath,
        final VersionSpec versionFrom,
        final VersionSpec versionTo) {
        super(shell);
        showDialog = true;

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.itemPath = itemPath;
        this.versionFrom = versionFrom;
        this.versionTo = versionTo;

        rollbackOptions = RollbackOptions.NONE;
        rollbackOptions = rollbackOptions.combine(RollbackOptions.KEEP_MERGE_HISTORY);

        final IPreferenceStore preferences = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();
        if (!preferences.getBoolean(UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS)) {
            rollbackOptions = rollbackOptions.combine(RollbackOptions.NO_AUTO_RESOLVE);
        }
    }

    @Override
    public IStatus run() {
        if (showDialog) {
            final RollbackItemDialog dialog =
                new RollbackItemDialog(getShell(), itemPath, repository, versionFrom, versionTo);

            if (dialog.open() != IDialogConstants.OK_ID) {
                return Status.OK_STATUS;
            }

            versionFrom = dialog.getFromVersion();
            versionTo = dialog.getToVersion();
            rollbackOptions = dialog.getRollbackOptions();
            itemPath = dialog.getItem();
        }

        if (!StringUtil.isNullOrEmpty(itemPath)) {
            final TFSItem item = TFSItemFactory.getItemAtPath(repository, itemPath);
            this.recursionType = item instanceof TFSFolder ? RecursionType.FULL : RecursionType.NONE;
        } else {
            this.recursionType = RecursionType.NONE;
        }

        final RollbackCommand rollbackCommand =
            new RollbackCommand(repository, itemPath, versionFrom, versionTo, recursionType, rollbackOptions);

        final ICommandExecutor commandExecutor = getCommandExecutor();

        final IStatus rollbackStatus = commandExecutor.execute(new ResourceChangingCommand(rollbackCommand));

        if (rollbackStatus.getSeverity() == IStatus.CANCEL) {
            return rollbackStatus;
        }

        try {
            if (rollbackCommand.hasConflicts()) {
                final ConflictDescription[] conflicts = rollbackCommand.getConflictDescriptions();

                final ConflictResolutionTask conflictTask =
                    new ConflictResolutionTask(getShell(), repository, conflicts);
                final IStatus conflictStatus = conflictTask.run();

                if (conflictStatus.isOK()) {
                    return Status.OK_STATUS;
                }
            }

            return rollbackStatus;
        } finally {
            final ICommand refreshCommand = new RefreshPendingChangesCommand(repository);
            commandExecutor.execute(refreshCommand);
        }

    }
}
