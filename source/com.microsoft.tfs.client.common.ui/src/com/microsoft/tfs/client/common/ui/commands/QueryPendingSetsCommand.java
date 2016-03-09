// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public class QueryPendingSetsCommand extends TFSCommand {
    private final TFSRepository repository;
    private final ItemSpec[] itemSpecs;
    private final String pendingChangeOwner;

    private PendingSet[] pendingSets;

    public QueryPendingSetsCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final String pendingChangeOwner) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;
        this.pendingChangeOwner = pendingChangeOwner;

    }

    @Override
    public String getName() {
        return Messages.getString("QueryPendingSetsCommand.CommandName"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("QueryPendingSetsCommand.CommandErrorDescription"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return "Querying pending changesets"; //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {

        final Workspace currentWorkspace = repository.getWorkspace();
        final ArrayList<PendingSet> localAndServerPendingSets = new ArrayList<PendingSet>();
        PendingSet[] localPendingSets = null;

        final PendingSet[] serverPendingSets =
            currentWorkspace.queryPendingSets(itemSpecs, null, pendingChangeOwner, false);

        // If the current workspace is a local workspace (and the user wants
        // changes by everyone or by current user)
        // then query local pending changes
        if (currentWorkspace.isLocalWorkspace()
            && (pendingChangeOwner == null || isPendingChangeOwnerCurrentWSOnwer())) {
            localPendingSets = currentWorkspace.queryPendingSets(
                itemSpecs,
                currentWorkspace.getName(),
                currentWorkspace.getOwnerName(),
                false);

        }

        if (serverPendingSets != null) {
            localAndServerPendingSets.addAll(Arrays.asList(serverPendingSets));
        }

        if (localPendingSets != null) {
            localAndServerPendingSets.addAll(Arrays.asList(localPendingSets));
        }

        pendingSets = localAndServerPendingSets.toArray(new PendingSet[localAndServerPendingSets.size()]);

        return Status.OK_STATUS;
    }

    public PendingSet[] getPendingSets() {
        return pendingSets;
    }

    // Checks if the current workspace owner is the pendingChange owner
    private boolean isPendingChangeOwnerCurrentWSOnwer() {
        for (final String ownerAlias : repository.getWorkspace().getOwnerAliases()) {
            if (ownerAlias.equalsIgnoreCase(pendingChangeOwner)) {
                return true;
            }
        }

        return false;
    }
}
