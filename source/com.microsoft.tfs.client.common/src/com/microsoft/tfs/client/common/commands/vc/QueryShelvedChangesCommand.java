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
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class QueryShelvedChangesCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final String name;
    private final String owner;
    private final ItemSpec[] items;
    private final boolean generateDownloadUrls;

    private PendingSet pendingSet;

    public QueryShelvedChangesCommand(
        final TFSRepository repository,
        final Shelveset shelveset,
        final ItemSpec[] items,
        final boolean generateDownloadUrls) {
        this(repository, shelveset.getName(), shelveset.getOwnerName(), items, generateDownloadUrls);
    }

    public QueryShelvedChangesCommand(
        final TFSRepository repository,
        final String name,
        final String owner,
        final ItemSpec[] items,
        final boolean generateDownloadUrls) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(name, "name"); //$NON-NLS-1$
        Check.notNull(owner, "owner"); //$NON-NLS-1$

        this.repository = repository;
        this.name = name;
        this.owner = owner;
        this.items = items;
        this.generateDownloadUrls = generateDownloadUrls;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("QueryShelvedChangesCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, name);
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryShelvedChangesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("QueryShelvedChangesCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, name);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) {
        final PendingSet[] pendingSets =
            repository.getWorkspace().queryShelvedChanges(name, owner, items, generateDownloadUrls);

        if (pendingSets == null || pendingSets.length != 1) {
            final String messageFormat = Messages.getString("QueryShelvedChangesCommand.CouldNotQueryShelveFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, name);
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null);
        }

        pendingSet = pendingSets[0];
        return Status.OK_STATUS;
    }

    public PendingSet getPendingSet() {
        return pendingSet;
    }

    public PendingChange[] getPendingChanges() {
        if (pendingSet == null) {
            return null;
        }

        return pendingSet.getPendingChanges();
    }
}
