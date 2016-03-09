// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.util.LocaleUtil;

public class SaveBuildDetailCommand extends TFSCommand {
    private final IBuildDetail buildDetail;

    public SaveBuildDetailCommand(final IBuildDetail buildDetail) {
        super();
        this.buildDetail = buildDetail;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("SaveBuildDetailCommand.SaveBuildDetailMessageFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildDetail.getBuildNumber());
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("SaveBuildDetailCommand.SaveBuildDetailErrorMessage"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("SaveBuildDetailCommand.SaveBuildDetailMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, buildDetail.getBuildNumber());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        buildDetail.save();
        return Status.OK_STATUS;
    }
}
