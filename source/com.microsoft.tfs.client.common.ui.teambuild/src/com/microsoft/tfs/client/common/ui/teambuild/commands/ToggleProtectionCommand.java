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
import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;

public class ToggleProtectionCommand extends TFSCommand {
    private final IBuildServer buildServer;
    private final IBuildDetail[] builds;
    private final boolean keepForever;

    public ToggleProtectionCommand(
        final IBuildServer buildServer,
        final IBuildDetail[] builds,
        final boolean keepForever) {
        this.buildServer = buildServer;
        this.builds = builds;
        this.keepForever = keepForever;

        addExceptionHandler(new ToggleProtectedExceptionHandler());
    }

    @Override
    public String getName() {
        return (Messages.getString("ToggleProtectionCommand.SavingBuildCommentText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("ToggleProtectionCommand.SavingBuildErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        if (builds.length == 1) {
            return MessageFormat.format(
                "{0} build {1}", //$NON-NLS-1$
                keepForever ? "Protecting" : "Unprotecting", //$NON-NLS-1$ //$NON-NLS-2$
                builds[0].getBuildNumber());
        } else {
            return MessageFormat.format("Toggling protection on {0} builds", builds.length); //$NON-NLS-1$
        }
    }

    /**
     * @see com.microsoft.tfs.client.common.framework.command.Command#doRun(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final List<IBuildDetail> modifiedBuilds = new ArrayList<IBuildDetail>(builds.length);
        for (int i = 0; i < builds.length; i++) {
            if (builds[i].isKeepForever() != keepForever) {
                builds[i].setKeepForever(keepForever);
                modifiedBuilds.add(builds[i]);
            }
        }

        buildServer.saveBuilds(modifiedBuilds.toArray(new IBuildDetail[modifiedBuilds.size()]));

        return Status.OK_STATUS;
    }

    private class ToggleProtectedExceptionHandler implements ICommandExceptionHandler {
        @Override
        public IStatus onException(final Throwable t) {
            if (BuildExplorer.getInstance() != null && !BuildExplorer.getInstance().isDisposed()) {
                BuildExplorer.getInstance().refresh();
            }
            return null;
        }
    }
}
