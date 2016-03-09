// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.framework.command.UndoableCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class SetWorkingFolderCommand extends TFSConnectedCommand implements UndoableCommand {
    private final TFSRepository repository;
    private final String serverPath;
    private final String localPath;
    private final WorkingFolderType type;
    private final RecursionType recursionType;
    private final boolean overwriteExisting;

    public SetWorkingFolderCommand(final TFSRepository repository, final String serverPath, final String localPath) {
        this(repository, serverPath, localPath, WorkingFolderType.MAP, RecursionType.FULL, false);
    }

    public SetWorkingFolderCommand(
        final TFSRepository repository,
        final String serverPath,
        final String localPath,
        final WorkingFolderType type,
        final RecursionType recursionType,
        final boolean overwriteExisting) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$

        this.repository = repository;
        this.serverPath = serverPath;
        this.localPath = localPath;
        this.type = type;
        this.recursionType = recursionType;
        this.overwriteExisting = overwriteExisting;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        if (type == WorkingFolderType.CLOAK) {
            return Messages.getString("SetWorkingFolderCommand.CloakingText"); //$NON-NLS-1$
        } else {
            return (Messages.getString("SetWorkingFolderCommand.CommandText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        if (type == WorkingFolderType.CLOAK) {
            return Messages.getString("SetWorkingFolderCommand.CloakingError"); //$NON-NLS-1$
        } else {
            return (Messages.getString("SetWorkingFolderCommand.ErrorText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (type == WorkingFolderType.CLOAK) {
            return Messages.getString("SetWorkingFolderCommand.CloakingText", LocaleUtil.ROOT); //$NON-NLS-1$
        } else {
            return (Messages.getString("SetWorkingFolderCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final WorkingFolder wf = new WorkingFolder(serverPath, LocalPath.canonicalize(localPath), type, recursionType);
        repository.getWorkspace().createWorkingFolder(wf, overwriteExisting);

        return Status.OK_STATUS;
    }

    @Override
    public IStatus rollback(final IProgressMonitor progressMonitor) throws Exception {
        return new DeleteWorkingFolderCommand(repository, new WorkingFolder(serverPath, localPath, type)).run(
            progressMonitor);
    }
}
