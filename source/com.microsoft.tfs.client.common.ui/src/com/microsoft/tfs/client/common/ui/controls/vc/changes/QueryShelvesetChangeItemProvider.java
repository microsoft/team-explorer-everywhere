// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.commands.vc.QueryShelvedChangesCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.util.Check;

public class QueryShelvesetChangeItemProvider extends AbstractShelvesetChangeItemProvider {
    private final ICommandExecutor commandExecutor;
    private final Shelveset shelveset;

    private boolean queried = false;
    private PendingChange[] pendingChanges = new PendingChange[0];

    public QueryShelvesetChangeItemProvider(
        final TFSRepository repository,
        final ICommandExecutor commandExecutor,
        final Shelveset shelveset) {
        super(repository);

        Check.notNull(commandExecutor, "commandExecutor"); //$NON-NLS-1$
        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$

        this.commandExecutor = commandExecutor;
        this.shelveset = shelveset;
    }

    @Override
    protected final PendingChange[] getPendingChanges() {
        if (queried == false) {
            try {
                final TFSRepository repository = getRepository();

                final QueryShelvedChangesCommand command =
                    new QueryShelvedChangesCommand(repository, shelveset, null, false);

                final IStatus status = commandExecutor.execute(command);

                if (status.isOK()) {
                    final PendingSet pendingSet = command.getPendingSet();

                    if (pendingSet != null) {
                        pendingChanges = pendingSet.getPendingChanges();
                    }
                }
            } finally {
                queried = true;
            }
        }

        return pendingChanges;
    }
}
