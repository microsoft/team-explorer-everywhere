// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteWorkingFolderCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final WorkingFolder workingFolder;

    public DeleteWorkingFolderCommand(final TFSRepository repository, final WorkingFolder workingFolder) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(workingFolder, "workingFolder"); //$NON-NLS-1$

        this.repository = repository;
        this.workingFolder = workingFolder;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("DeleteWorkingFolderCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("DeleteWorkingFolderCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("DeleteWorkingFolderCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        repository.getWorkspace().deleteWorkingFolder(workingFolder);

        return Status.OK_STATUS;
    }
}