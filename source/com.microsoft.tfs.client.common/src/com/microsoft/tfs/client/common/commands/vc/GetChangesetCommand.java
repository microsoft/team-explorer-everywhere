// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetChangesetCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final int changesetId;

    private Changeset changeset;

    public GetChangesetCommand(final TFSRepository repository, final int changesetId) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.changesetId = changesetId;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("GetChangesetCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(changesetId));
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("GetChangesetCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("GetChangesetCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(changesetId));
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        changeset = repository.getVersionControlClient().getChangeset(changesetId, true, false, null, null);

        return Status.OK_STATUS;
    }

    public Changeset getChangeset() {
        return changeset;
    }
}
