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
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.util.LocaleUtil;

public class PostponeResumeCommand extends TFSCommand {

    private final IBuildServer buildServer;
    private final IQueuedBuild[] queuedBuilds;
    private final boolean resume;

    private IQueuedBuild[] affectedQueuedBuilds;

    public PostponeResumeCommand(
        final IBuildServer buildServer,
        final IQueuedBuild[] queuedBuilds,
        final boolean resume) {
        this.buildServer = buildServer;
        this.queuedBuilds = queuedBuilds;
        this.resume = resume;
    }

    @Override
    public String getName() {
        if (resume) {
            return (Messages.getString("PostponeResumeCommand.ResumingBuildsCommandText")); //$NON-NLS-1$
        } else {
            return (Messages.getString("PostponeResumeCommand.PostponingBuildsCommandText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        if (resume) {
            return (Messages.getString("PostponeResumeCommand.ResumingBuildsErrorText")); //$NON-NLS-1$
        } else {
            return (Messages.getString("PostponeResumeCommand.PostponingBuildsErrorText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (resume) {
            return (Messages.getString("PostponeResumeCommand.ResumingBuildsCommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
        } else {
            return (Messages.getString("PostponeResumeCommand.PostponingBuildsCommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
        }
    }

    /**
     * @see com.microsoft.tfs.client.common.framework.command.Command#doRun(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final List<IQueuedBuild> buildsToChange = new ArrayList<IQueuedBuild>();

        for (int i = 0; i < queuedBuilds.length; i++) {
            if (!resume && queuedBuilds[i].getStatus().contains(QueueStatus.QUEUED)) {
                // We are postponing
                queuedBuilds[i].postpone();
                buildsToChange.add(queuedBuilds[i]);
            } else if (resume && queuedBuilds[i].getStatus().contains(QueueStatus.POSTPONED)) {
                queuedBuilds[i].resume();
                buildsToChange.add(queuedBuilds[i]);
            }
        }

        affectedQueuedBuilds = buildsToChange.toArray(new IQueuedBuild[buildsToChange.size()]);

        buildServer.saveQueuedBuilds(affectedQueuedBuilds);

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
