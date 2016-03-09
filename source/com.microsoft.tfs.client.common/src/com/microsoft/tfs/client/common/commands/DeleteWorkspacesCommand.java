// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.framework.command.MultiCommandHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteWorkspacesCommand extends TFSCommand {
    private final Workspace[] workspaces;
    private volatile List<Workspace> deletedWorkspaces = new ArrayList<Workspace>();

    public DeleteWorkspacesCommand(final Workspace[] workspaces) {
        this.workspaces = workspaces;
    }

    @Override
    public String getName() {
        if (workspaces.length == 1) {
            final String messageFormat = Messages.getString("DeleteWorkspacesCommand.SingleWorkspaceCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, workspaces[0].getName());
        } else {
            return Messages.getString("DeleteWorkspacesCommand.MultiWorkspaceCommandText"); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        if (workspaces.length == 1) {
            final String messageFormat = Messages.getString("DeleteWorkspacesCommand.SingleWorkspaceErrorTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, workspaces[0].getName());
        } else {
            return Messages.getString("DeleteWorkspacesCommand.MultiWorkspaceErrorText"); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (workspaces.length == 1) {
            final String messageFormat =
                Messages.getString("DeleteWorkspacesCommand.SingleWorkspaceCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, workspaces[0].getName());
        } else {
            return Messages.getString("DeleteWorkspacesCommand.MultiWorkspaceCommandText", LocaleUtil.ROOT); //$NON-NLS-1$
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        deletedWorkspaces.clear();

        final MultiCommandHelper helper = new MultiCommandHelper(progressMonitor, new int[] {
            IStatus.OK,
            IStatus.INFO,
            IStatus.WARNING,
            IStatus.ERROR
        });

        helper.beginMainTask(Messages.getString("DeleteWorkspacesCommand.ProgressMonitorTitle"), workspaces.length); //$NON-NLS-1$

        for (int i = 0; i < workspaces.length; i++) {
            final DeleteWorkspaceCommand subCommand = new DeleteWorkspaceCommand(workspaces[i]);

            final IStatus subStatus = helper.runSubCommand(subCommand);

            if (subStatus.isOK()) {
                deletedWorkspaces.add(workspaces[i]);
            }
        }

        return helper.getStatus(Messages.getString("DeleteWorkspacesCommand.ProgressMonitorErrorText")); //$NON-NLS-1$
    }

    public Workspace[] getDeletedWorkspaces() {
        return deletedWorkspaces.toArray(new Workspace[deletedWorkspaces.size()]);
    }
}
