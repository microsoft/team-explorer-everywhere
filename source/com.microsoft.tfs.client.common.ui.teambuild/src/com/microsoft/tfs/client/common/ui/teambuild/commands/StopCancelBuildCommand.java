// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.util.LocaleUtil;

public class StopCancelBuildCommand extends TFSCommand {
    private final IBuildServer buildServer;
    private final IQueuedBuild[] queuedBuilds;
    private final boolean stop;

    public StopCancelBuildCommand(
        final IBuildServer buildServer,
        final IQueuedBuild[] queuedBuilds,
        final boolean stop) {
        this.buildServer = buildServer;
        this.queuedBuilds = queuedBuilds;
        this.stop = stop;
    }

    @Override
    public String getName() {
        if (queuedBuilds.length == 1 && queuedBuilds[0].getBuildDefinition() != null) {
            if (stop) {
                final String messageFormat =
                    Messages.getString("StopCancelBuildCommand.StoppingSingleBuildMessageFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, queuedBuilds[0].getBuildDefinition().getName());
            } else {
                final String messageFormat =
                    Messages.getString("StopCancelBuildCommand.CancellingSingleBuildMessageFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, queuedBuilds[0].getBuildDefinition().getName());
            }
        } else {
            if (stop) {
                return Messages.getString("StopCancelBuildCommand.StoppingMultipleBuildsMessage"); //$NON-NLS-1$
            } else {
                return Messages.getString("StopCancelBuildCommand.CancellingMultipleBuildsMessage"); //$NON-NLS-1$
            }
        }
    }

    @Override
    public String getErrorDescription() {
        if (stop) {
            return Messages.getString("StopCancelBuildCommand.StoppingBuildsErrorMessage"); //$NON-NLS-1$
        } else {
            return Messages.getString("StopCancelBuildCommand.CancellingBuildsErrorMessage"); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (queuedBuilds.length == 1 && queuedBuilds[0].getBuildDefinition() != null) {
            if (stop) {
                final String messageFormat =
                    Messages.getString("StopCancelBuildCommand.StoppingSingleBuildMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, queuedBuilds[0].getBuildDefinition().getName());
            } else {
                final String messageFormat =
                    Messages.getString("StopCancelBuildCommand.CancellingSingleBuildMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, queuedBuilds[0].getBuildDefinition().getName());
            }
        } else {
            if (stop) {
                return Messages.getString("StopCancelBuildCommand.StoppingMultipleBuildsMessage", LocaleUtil.ROOT); //$NON-NLS-1$
            } else {
                return Messages.getString("StopCancelBuildCommand.CancellingMultipleBuildsMessage", LocaleUtil.ROOT); //$NON-NLS-1$
            }
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (stop) {
            final List<IBuildDetail> builds = new ArrayList<IBuildDetail>();
            for (int i = 0; i < queuedBuilds.length; i++) {
                if (queuedBuilds[i].getBuild() != null) {
                    builds.add(queuedBuilds[i].getBuild());
                }
            }
            buildServer.stopBuilds(builds.toArray(new IBuildDetail[builds.size()]));
        } else {
            buildServer.cancelBuilds(queuedBuilds);
        }

        return Status.OK_STATUS;
    }
}
