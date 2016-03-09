// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class RestoreConnectionCommand extends TFSConnectedCommand {
    private final TFSConnection connection;

    public RestoreConnectionCommand(final TFSConnection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
    }

    @Override
    public String getName() {
        return MessageFormat.format(
            Messages.getString("RestoreConnectionCommand.CommandNameFormat"), //$NON-NLS-1$
            connection.getBaseURI());
    }

    @Override
    public String getErrorDescription() {
        return MessageFormat.format(
            Messages.getString("RestoreConnectionCommand.CommandErrorFormat"), //$NON-NLS-1$
            connection.getBaseURI());
    }

    @Override
    public String getLoggingDescription() {
        return MessageFormat.format(
            Messages.getString("RestoreConnectionCommand.CommandNameFormat", LocaleUtil.ROOT), //$NON-NLS-1$
            connection.getBaseURI());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        ensureConnected(connection, progressMonitor);

        return Status.OK_STATUS;
    }
}
