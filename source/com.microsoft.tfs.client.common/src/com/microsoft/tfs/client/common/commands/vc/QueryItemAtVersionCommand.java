// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.vc.VersionSpecHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.LocaleUtil;

public class QueryItemAtVersionCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final ItemSpec itemSpec;
    private final VersionSpec itemVersion;
    private final VersionSpec requestedVersion;

    private Item item;

    public QueryItemAtVersionCommand(
        final TFSRepository repository,
        final ItemSpec itemSpec,
        final VersionSpec itemVersion,
        final VersionSpec requestedVersion) {
        this.repository = repository;
        this.itemSpec = itemSpec;
        this.itemVersion = itemVersion;
        this.requestedVersion = requestedVersion;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("QueryItemAtVersionCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            itemSpec.getItem(),
            VersionSpecHelper.getVersionSpecDescription(requestedVersion));
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryItemAtVersionCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("QueryItemAtVersionCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            itemSpec.getItem(),
            VersionSpecHelper.getVersionSpecDescription(requestedVersion));
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final Changeset[] changesets = repository.getVersionControlClient().queryHistory(
            itemSpec.getItem(),
            itemVersion,
            itemSpec.getDeletionID(),
            itemSpec.getRecursionType(),
            null,
            null,
            requestedVersion,
            1,
            true,
            false,
            true,
            false);

        if (changesets != null && changesets.length == 1) {
            final Change[] changes = changesets[0].getChanges();
            if (changes != null && changes.length == 1) {
                item = changes[0].getItem();
                return Status.OK_STATUS;
            }
        }

        final String messageFormat = Messages.getString("QueryItemAtVersionCommand.ItemDoesNotExistFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, itemSpec.getItem(), requestedVersion.toString());
        return new Status(Status.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
    }

    public Item getItem() {
        return item;
    }
}
