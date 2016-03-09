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
import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.DateVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;

public class QueryItemsCommand extends TFSCommand {
    private final VersionControlClient vcClient;

    private final ItemSpec[] itemSpecs;
    private final VersionSpec versionSpec;
    private final DeletedState deletedState;
    private final ItemType itemType;
    private final GetItemsOptions options;

    private ItemSet[] itemSets;

    private String name = null;

    public QueryItemsCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final VersionSpec versionSpec,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        this(repository.getWorkspace().getClient(), itemSpecs, versionSpec, deletedState, itemType, options);
    }

    public QueryItemsCommand(
        final VersionControlClient vcClient,
        final ItemSpec[] itemSpecs,
        final VersionSpec versionSpec,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.itemSpecs = itemSpecs;
        this.versionSpec = versionSpec;
        this.deletedState = deletedState;
        this.itemType = itemType;
        this.options = options;

        addExceptionHandler(new QueryItemsExceptionHandler());
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
            final String messageFormat = Messages.getString("QueryItemsCommand.SingleItemCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs[0].getItem());
        } else {
            final String messageFormat = Messages.getString("QueryItemsCommand.MultiItemCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs.length);
        }
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("QueryItemsCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
        try {
            itemSets = vcClient.getItems(itemSpecs, versionSpec, deletedState, itemType, options);
        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        } finally {
            progressMonitor.done();
        }

        return Status.OK_STATUS;
    }

    public ItemSet[] getItemSets() {
        return itemSets;
    }

    private class QueryItemsExceptionHandler implements ICommandExceptionHandler {
        @Override
        public IStatus onException(final Throwable t) {
            if (t instanceof TECoreException
                && t.getMessage().startsWith("TF14021: ") //$NON-NLS-1$
                && versionSpec instanceof DateVersionSpec) {
                final String messageFormat = Messages.getString("QueryItemsCommand.DateIsBeforeAnyChangesetFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    DateHelper.getDefaultDateTimeFormat().format(((DateVersionSpec) versionSpec).getDate().getTime()));

                return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 14021, message, null);
            }

            return null;
        }
    }
}
