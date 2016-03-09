// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.vc.HistoryManager;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.LocaleUtil;

public class FindChangesetChildrenCommand extends TFSCommand {
    private final TFSRepository repository;

    private final Changeset changeset;

    private Changeset[] childrenChangesets;

    public FindChangesetChildrenCommand(final TFSRepository repository, final Changeset changeset) {
        this.repository = repository;
        this.changeset = changeset;
    }

    @Override
    public String getName() {
        return (Messages.getString("FindChangesetChildrenCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("FindChangesetChildrenCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("FindChangesetChildrenCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        childrenChangesets = HistoryManager.findChangesetChildren(repository, changeset);
        return Status.OK_STATUS;
    }

    public Changeset[] getChildrenChangesets() {
        return childrenChangesets;
    }

}
