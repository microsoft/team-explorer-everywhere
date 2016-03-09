// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class QueueBuildCommand extends TFSConnectedCommand {
    private final IBuildRequest buildRequest;
    private IQueuedBuild queuedBuild;

    public QueueBuildCommand(final IBuildRequest buildRequest) {
        Check.notNull(buildRequest, "buildRequest"); //$NON-NLS-1$

        this.buildRequest = buildRequest;

        setConnection(buildRequest.getBuildServer().getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("QueueBuildCommand.RequestBuildCommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildRequest.getBuildDefinition().getName());
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("QueueBuildCommand.RequestBuildErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("QueueBuildCommand.RequestBuildCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildRequest.getBuildDefinition().getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        queuedBuild = buildRequest.getBuildDefinition().getBuildServer().queueBuild(buildRequest);

        return Status.OK_STATUS;
    }

    public IQueuedBuild getQueuedBuild() {
        return queuedBuild;
    }

}
