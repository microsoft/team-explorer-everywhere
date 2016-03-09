// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.LocaleUtil;

public class FindWorkspacesCommand extends TFSCommand {
    private final VersionControlClient server;
    private final String owner;

    private Workspace[] workspaces;

    public FindWorkspacesCommand(final VersionControlClient server, final String owner) {
        this.server = server;
        this.owner = owner;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("FindWorkspacesCommand.QueryWorkspaceMessageFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, owner);
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("FindWorkspacesCommand.QueryWorkspaceErrorMessage"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("FindWorkspacesCommand.QueryWorkspaceMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, owner);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.shared.command.Command#doRun(org.eclipse
     * .core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        workspaces = server.queryWorkspaces(null, owner, null);
        return Status.OK_STATUS;
    }

    public Workspace[] getWorkspaces() {
        return workspaces;
    }
}
