// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.util.LocaleUtil;

public class SetQueuePriorityCommand extends TFSCommand {

    private final IBuildServer buildServer;
    private final IQueuedBuild[] queuedBuilds;
    private final QueuePriority queuePriority;

    private IQueuedBuild[] affectedQueuedBuilds;

    public SetQueuePriorityCommand(
        final IBuildServer buildServer,
        final IQueuedBuild[] queuedBuilds,
        final QueuePriority queuePriority) {
        this.buildServer = buildServer;
        this.queuedBuilds = queuedBuilds;
        this.queuePriority = queuePriority;
    }

    @Override
    public String getName() {
        return (Messages.getString("SetQueuePriorityCommand.CommandName")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("SetQueuePriorityCommand.CommandErrorMessage")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("SetQueuePriorityCommand.CommandName", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    /**
     * @see com.microsoft.tfs.client.common.framework.command.Command#doRun(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final List<IQueuedBuild> buildsToChange = new ArrayList<IQueuedBuild>();
        for (int i = 0; i < queuedBuilds.length; i++) {
            if (queuedBuilds[i].getStatus().containsAny(QueueStatus.QUEUED.combine(QueueStatus.POSTPONED))) {
                queuedBuilds[i].setPriority(queuePriority);
                buildsToChange.add(queuedBuilds[i]);
            }
        }

        affectedQueuedBuilds = buildsToChange.toArray(new IQueuedBuild[buildsToChange.size()]);

        buildServer.saveQueuedBuilds(affectedQueuedBuilds);

        BuildHelpers.getBuildManager().fireBuildPrioritiesChangedEvent(this, affectedQueuedBuilds);

        return Status.OK_STATUS;
    }

    /**
     * @return the affectedQueuedBuilds
     */
    public IQueuedBuild[] getAffectedQueuedBuilds() {
        if (affectedQueuedBuilds == null) {
            throw new IllegalStateException("Attempt to get the affected builds before the command has been run."); //$NON-NLS-1$
        }
        return affectedQueuedBuilds;
    }
}
