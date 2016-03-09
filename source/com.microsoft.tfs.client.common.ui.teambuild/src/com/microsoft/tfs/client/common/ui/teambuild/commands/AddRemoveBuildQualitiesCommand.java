// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class AddRemoveBuildQualitiesCommand extends TFSCommand {

    private final IBuildServer buildServer;
    private final String teamProject;
    private final String[] qualitiesToAdd;
    private final String[] qualitiesToRemove;

    public AddRemoveBuildQualitiesCommand(
        final IBuildServer buildServer,
        final String teamProject,
        final String[] qualitiesToAdd,
        final String[] qualitiesToRemove) {
        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNullOrEmpty(teamProject, "teamProject"); //$NON-NLS-1$
        Check.notNull(qualitiesToAdd, "qualitiesToAdd"); //$NON-NLS-1$
        Check.notNull(qualitiesToRemove, "qualitiesToRemove"); //$NON-NLS-1$

        this.buildServer = buildServer;
        this.teamProject = teamProject;
        this.qualitiesToAdd = qualitiesToAdd;
        this.qualitiesToRemove = qualitiesToRemove;
    }

    @Override
    public String getName() {
        return Messages.getString("AddRemoveBuildQualitiesCommand.EditBuildQualityCommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("AddRemoveBuildQualitiesCommand.EditBuildQualityErrorText"); //$NON-NLS-1$

    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("AddRemoveBuildQualitiesCommand.EditBuildQualityCommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    /**
     * @see com.microsoft.tfs.client.common.framework.command.Command#doRun(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (qualitiesToAdd.length > 0) {
            buildServer.addBuildQuality(teamProject, qualitiesToAdd);
        }
        if (qualitiesToRemove.length > 0) {
            buildServer.deleteBuildQuality(teamProject, qualitiesToRemove);
        }

        // Force re-population of cache.
        TeamBuildCache.getInstance(buildServer, teamProject).getBuildQualities(true);

        return Status.OK_STATUS;
    }
}
