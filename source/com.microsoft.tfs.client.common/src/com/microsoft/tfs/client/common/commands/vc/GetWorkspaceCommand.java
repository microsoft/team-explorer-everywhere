// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetWorkspaceCommand extends TFSCommand {
    private final TFSTeamProjectCollection connection;
    private final String localPath;

    private Workspace workspace;

    public GetWorkspaceCommand(final TFSTeamProjectCollection connection, final String localPath) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        this.connection = connection;
        this.localPath = localPath;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("QueryWorkspaceCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, localPath);
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryWorkspaceCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("QueryWorkspaceCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, localPath);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        workspace = connection.getVersionControlClient().getWorkspace(localPath);

        if (workspace == null) {
            final String messageFormat = Messages.getString("QueryWorkspaceCommand.NoWorkspaceContainsMappingFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, connection.getName(), localPath);
            return new Status(Status.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
        }

        return Status.OK_STATUS;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}
