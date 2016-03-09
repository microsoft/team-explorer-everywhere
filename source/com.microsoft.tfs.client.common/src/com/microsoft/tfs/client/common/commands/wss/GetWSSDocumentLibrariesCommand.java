// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.wss;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.sharepoint.WSSClient;
import com.microsoft.tfs.core.clients.sharepoint.WSSDocumentLibrary;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetWSSDocumentLibrariesCommand extends TFSConnectedCommand {
    private final TFSTeamProjectCollection connection;
    private final ProjectInfo projectInfo;
    private final boolean refresh;

    private WSSDocumentLibrary[] libraries;

    public GetWSSDocumentLibrariesCommand(
        final TFSTeamProjectCollection connection,
        final ProjectInfo projectInfo,
        final boolean refresh) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$

        this.connection = connection;
        this.projectInfo = projectInfo;
        this.refresh = refresh;

        setConnection(connection);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("GetWssDocumentLibrariesCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, projectInfo.getName());
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("GetWssDocumentLibrariesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("GetWssDocumentLibrariesCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, projectInfo.getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final WSSClient wssClient = connection.getWSSClient(projectInfo);

        if (wssClient == null) {
            libraries = new WSSDocumentLibrary[0];
        } else {
            libraries = wssClient.getDocumentLibraries(refresh);
        }

        return Status.OK_STATUS;
    }

    public WSSDocumentLibrary[] getDocumentLibraries() {
        return libraries;
    }
}
