// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescriptionFactory;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class QueryConflictsCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final ItemSpec[] itemSpecs;

    private ConflictDescription[] conflictDescriptions;

    public QueryConflictsCommand(final TFSRepository repository, final ItemSpec[] itemSpecs) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryConflictsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryConflictsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryConflictsCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final Workspace workspace = repository.getWorkspace();
        final Conflict[] conflicts = workspace.queryConflicts(itemSpecs);

        if (conflicts != null) {
            // store these away for callers
            conflictDescriptions = ConflictDescriptionFactory.getConflictDescriptions(workspace, conflicts, itemSpecs);

            // notify the conflict cache
            for (int i = 0; i < conflicts.length; i++) {
                repository.getConflictManager().addConflict(conflicts[i]);
            }
        }

        return Status.OK_STATUS;
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions;
    }
}
