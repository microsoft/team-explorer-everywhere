// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class QueryShelvesetConflictsCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final String name;
    private final String owner;

    private ConflictDescription[] conflictDescriptions;

    public QueryShelvesetConflictsCommand(final TFSRepository repository, final Shelveset shelveset) {
        this(repository, shelveset.getName(), shelveset.getOwnerName());
    }

    public QueryShelvesetConflictsCommand(final TFSRepository repository, final String name, final String owner) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(name, "name"); //$NON-NLS-1$
        Check.notNull(owner, "owner"); //$NON-NLS-1$

        this.repository = repository;
        this.name = name;
        this.owner = owner;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("QueryShelvesetConflictsCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, name);
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryShelvesetConflictsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("QueryShelvesetConflictsCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, name);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), 2);

        final QueryShelvedChangesCommand queryShelvesetCommand =
            new QueryShelvedChangesCommand(repository, name, owner, null, false);

        final SubProgressMonitor queryShelvesetMonitor = new SubProgressMonitor(progressMonitor, 1);
        final IStatus queryShelvesetStatus = queryShelvesetCommand.run(queryShelvesetMonitor);

        if (!queryShelvesetStatus.isOK()) {
            return queryShelvesetStatus;
        }

        final PendingChange[] pendingChanges = queryShelvesetCommand.getPendingChanges();

        final List itemList = new ArrayList();

        for (int i = 0; i < pendingChanges.length; i++) {
            if (pendingChanges[i].getServerItem() == null) {
                continue;
            }

            final RecursionType recursionType =
                (pendingChanges[i].getItemType() == ItemType.FILE) ? RecursionType.NONE : RecursionType.FULL;

            itemList.add(new ItemSpec(pendingChanges[i].getServerItem(), recursionType));
        }

        final ItemSpec[] items = (ItemSpec[]) itemList.toArray(new ItemSpec[itemList.size()]);

        final QueryConflictsCommand queryConflictsCommand = new QueryConflictsCommand(repository, items);

        final SubProgressMonitor queryConflictsMonitor = new SubProgressMonitor(progressMonitor, 1);
        final IStatus queryConflictsStatus = queryConflictsCommand.run(queryConflictsMonitor);

        if (!queryConflictsStatus.isOK()) {
            return queryConflictsStatus;
        }

        conflictDescriptions = queryConflictsCommand.getConflictDescriptions();

        return Status.OK_STATUS;
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions;
    }
}
