// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class RefreshPendingChangesCommand extends TFSCommand {
    private final TFSRepository repository;

    public RefreshPendingChangesCommand(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
    }

    @Override
    public String getName() {
        return (Messages.getString("RefreshPendingChangesCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("RefreshPendingChangesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("RefreshPendingChangesCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        repository.getPendingChangeCache().refresh();

        return Status.OK_STATUS;
    }
}
