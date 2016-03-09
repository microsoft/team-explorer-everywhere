// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class QueryShelvesetsCommand extends TFSConnectedCommand {
    private final VersionControlClient client;
    private final String name;
    private final String owner;

    private Shelveset[] shelvesets;

    public QueryShelvesetsCommand(final VersionControlClient client, final String name, final String owner) {
        super();

        Check.notNull(client, "client"); //$NON-NLS-1$

        this.client = client;
        this.name = name;
        this.owner = owner;

        setConnection(client.getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryShelvesetsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryShelvesetsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryShelvesetsCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        shelvesets = client.queryShelvesets(name, owner, null);

        return Status.OK_STATUS;
    }

    public Shelveset[] getShelvesets() {
        return shelvesets;
    }
}
