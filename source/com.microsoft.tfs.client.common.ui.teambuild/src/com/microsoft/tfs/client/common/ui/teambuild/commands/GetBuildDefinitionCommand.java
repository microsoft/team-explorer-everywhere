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
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetBuildDefinitionCommand extends TFSCommand {
    private final IBuildDefinition buildDefinition;
    private IBuildDefinition newBuildDefinition;

    public GetBuildDefinitionCommand(final IBuildDefinition buildDefinition) {
        super();

        Check.notNull(buildDefinition, "buildDefinition"); //$NON-NLS-1$
        this.buildDefinition = buildDefinition;
    }

    @Override
    public String getName() {
        final String messageFormat =
            Messages.getString("GetBuildDefinitionCommand.LoadingBuildDefinitionMessageFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildDefinition.getName());
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("GetBuildDefinitionCommand.LoadingBuildDefinitionErrorMessage"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        /* Don't log unless we're going to hit the server */
        if (buildDefinition.getURI() == null) {
            return null;
        }

        final String messageFormat =
            Messages.getString("GetBuildDefinitionCommand.LoadingBuildDefinitionMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildDefinition.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.shared.command.Command#doRun(org.eclipse
     * .core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        newBuildDefinition = getUpToDateBuildDefinition(buildDefinition);

        return Status.OK_STATUS;
    }

    private IBuildDefinition getUpToDateBuildDefinition(final IBuildDefinition buildDefinition) {
        if (buildDefinition.getURI() == null) {
            // This is a new one - no need to refresh.
            return buildDefinition;
        }

        final IBuildServer buildServer = buildDefinition.getBuildServer();

        // Note that Microsoft's client does it this way - personally I would
        // have thought that getUri made more sense
        // however doing it this way to ensure our clients behave the same way
        // if a build is renamed / deleted.

        final IBuildDefinition newDefinition =
            buildServer.getBuildDefinition(buildDefinition.getTeamProject(), buildDefinition.getName());

        return newDefinition;
    }

    public IBuildDefinition getBuildDefinition() {
        return newBuildDefinition;
    }

}
