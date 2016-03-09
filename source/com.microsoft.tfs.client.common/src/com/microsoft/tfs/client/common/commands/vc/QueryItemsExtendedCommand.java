// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;

public class QueryItemsExtendedCommand extends TFSCommand {
    private final TFSRepository repository;

    private final ItemSpec[] itemSpecs;
    private final DeletedState deletedState;
    private final ItemType itemType;
    private final GetItemsOptions options;
    private final String[] itemPropertyFilters;

    private ExtendedItem[][] extendedItems;

    private String name = null;

    public QueryItemsExtendedCommand(final TFSRepository repository, final ItemSpec[] itemSpecs) {
        this(repository, itemSpecs, DeletedState.NON_DELETED, ItemType.ANY, GetItemsOptions.NONE);
    }

    public QueryItemsExtendedCommand(
        final TFSRepository repository,
        final String path,
        final ItemType itemType,
        final DeletedState deletedState,
        final RecursionType recursionType,
        final GetItemsOptions options) {
        this(repository, new ItemSpec(path, recursionType), deletedState, itemType, options);
    }

    public QueryItemsExtendedCommand(
        final TFSRepository repository,
        final ItemSpec itemSpec,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        this(repository, new ItemSpec[] {
            itemSpec
        }, deletedState, itemType, options);
    }

    public QueryItemsExtendedCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        this(repository, itemSpecs, deletedState, itemType, options, null);
    }

    /**
     * Specifying non-<code>null</code> itemPropertyFilters for TFS servers <
     * {@link WebServiceLevel#TFS_2012_2} will cause a client-side exception.
     * Pass null for older servers.
     *
     * @param itemPropertyFilters
     *        specifies the item properties to return with the items (may be
     *        <code>null</code>)
     */
    public QueryItemsExtendedCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;
        this.deletedState = deletedState;
        this.itemType = itemType;
        this.options = options;
        this.itemPropertyFilters = itemPropertyFilters;
    }

    /**
     * Clients may override the name shown to the user (see
     * {@link RenameCommand}).
     *
     * @param name
     */
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }

        if (itemSpecs.length == 1) {
            final String messageFormat = Messages.getString("QueryItemsExtendedCommand.SingleCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs[0].getItem());
        } else {
            final String messageFormat = Messages.getString("QueryItemsExtendedCommand.MultiCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs.length);
        }
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryItemsExtendedCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        /*
         * Called too frequently (by Version Control Explorer) to log every call
         * at info level
         */
        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            extendedItems = repository.getWorkspace().getExtendedItems(
                itemSpecs,
                deletedState,
                itemType,
                options,
                itemPropertyFilters);
        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        } catch (final Exception e) {
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), null);
        }

        return Status.OK_STATUS;
    }

    public ExtendedItem[][] getItems() {
        return extendedItems;
    }
}
