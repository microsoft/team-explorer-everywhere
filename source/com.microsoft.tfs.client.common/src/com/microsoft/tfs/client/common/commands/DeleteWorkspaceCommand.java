// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteWorkspaceCommand extends TFSConnectedCommand {
    private final Workspace workspace;

    public DeleteWorkspaceCommand(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;

        setConnection(workspace.getClient().getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("DeleteWorkspaceCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspace.getName());
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("DeleteWorkspaceCommand.ErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspace.getName());
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("DeleteWorkspaceCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspace.getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final String messageFormat = Messages.getString("DeleteWorkspaceCommand.ProgressMonitorTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, workspace.getName());
        progressMonitor.beginTask(message, IProgressMonitor.UNKNOWN);

        workspace.getClient().deleteWorkspace(workspace);

        return Status.OK_STATUS;
    }
}
