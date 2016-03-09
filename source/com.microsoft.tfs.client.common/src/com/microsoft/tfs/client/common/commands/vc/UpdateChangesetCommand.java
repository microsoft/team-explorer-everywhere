// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class UpdateChangesetCommand extends TFSConnectedCommand {
    private final VersionControlClient client;
    private final Changeset changeset;

    public UpdateChangesetCommand(final VersionControlClient client, final Changeset changeset) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(changeset, "changeset"); //$NON-NLS-1$

        this.client = client;
        this.changeset = changeset;

        setConnection(client.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("UpdateChangesetCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(changeset.getChangesetID()));
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("UpdateChangesetCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("UpdateChangesetCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(changeset.getChangesetID()));
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        client.updateChangeset(changeset);
        return Status.OK_STATUS;
    }
}
