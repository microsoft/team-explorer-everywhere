// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildHelper;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class DownloadBuildDropCommand extends TFSCommand {
    private final File localTarget;
    private final URL downloadURL;
    private final TFSTeamProjectCollection connection;

    public DownloadBuildDropCommand(
        final URL downloadURL,
        final File localTarget,
        final TFSTeamProjectCollection connection) {
        Check.notNull(downloadURL, "downloadURL"); //$NON-NLS-1$
        Check.notNull(localTarget, "localTarget"); //$NON-NLS-1$
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.localTarget = localTarget;
        this.downloadURL = downloadURL;
        this.connection = connection;

        setCancellable(true);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("DownloadBuildDropCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, localTarget.getName());
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("DownloadBuildDropCommand.CommandTextFormat")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("DownloadBuildDropCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, localTarget.getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        TeamBuildHelper.download(downloadURL, localTarget, connection);
        return Status.OK_STATUS;
    }

}
