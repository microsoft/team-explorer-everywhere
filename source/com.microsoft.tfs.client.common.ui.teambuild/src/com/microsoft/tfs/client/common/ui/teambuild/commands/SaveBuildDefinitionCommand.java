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
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.util.LocaleUtil;

public class SaveBuildDefinitionCommand extends TFSCommand {
    private final IBuildDefinition buildDefinition;

    public SaveBuildDefinitionCommand(final IBuildDefinition buildDefinition) {
        super();
        this.buildDefinition = buildDefinition;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("SaveBuildDefinitionCommand.SavingCommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildDefinition.getName());
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("SaveBuildDefinitionCommand.SavingErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("SaveBuildDefinitionCommand.SavingCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildDefinition.getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        buildDefinition.save();

        /*
         * We may have added a new build definition and/or added build agents at
         * this point - best to destroy the cache to allow it to be rebuilt when
         * next needed.
         */
        TeamBuildCache.getInstance(buildDefinition.getBuildServer(), buildDefinition.getTeamProject()).destroy();
        return Status.OK_STATUS;
    }
}
