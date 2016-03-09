// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.util.Check;

public class QueryItemsByIDCommand extends TFSConnectedCommand {
    private final TFSRepository repository;

    private final int[] itemIds;
    private final int changesetNumber;
    private final GetItemsOptions options;

    private Item[] items;

    public QueryItemsByIDCommand(
        final TFSRepository repository,
        final int[] itemIds,
        final int changesetNumber,
        final GetItemsOptions options) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemIds, "itemIds"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        this.repository = repository;
        this.itemIds = itemIds;
        this.changesetNumber = changesetNumber;
        this.options = options;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return Messages.getString("QueryItemsByIdCommand.Name"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("QueryItemsByIdCommand.ErrorDescription"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        /* Don't log queries */
        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        items = repository.getWorkspace().getClient().getItems(itemIds, changesetNumber, options);

        return Status.OK_STATUS;
    }

    public Item[] getItems() {
        return items;
    }
}
