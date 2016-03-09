// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteBuildDefinitionCommand extends TFSCommand {
    private final IBuildServer buildServer;
    private final IBuildDefinition buildDefinition;

    /**
     * @param buildServer
     * @param buildDefinition
     */
    public DeleteBuildDefinitionCommand(final IBuildServer buildServer, final IBuildDefinition buildDefinition) {
        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNull(buildDefinition, "buildDefinition"); //$NON-NLS-1$

        this.buildServer = buildServer;
        this.buildDefinition = buildDefinition;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("DeleteBuildDefinitionCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildDefinition.getName());
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("DeleteBuildDefinitionCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("DeleteBuildDefinitionCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildDefinition.getName());
    }

    /**
     * @see com.microsoft.tfs.client.common.framework.command.Command#doRun(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        buildServer.deleteBuildDefinitions(new IBuildDefinition[] {
            buildDefinition
        });

        // Refresh Cache
        TeamBuildCache.getInstance(buildServer, buildDefinition.getTeamProject()).getBuildDefinitions(true);

        return Status.OK_STATUS;
    }

}
