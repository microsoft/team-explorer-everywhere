// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class QueryHistoryCommand extends TFSConnectedCommand {
    protected final TFSRepository repository;

    protected final String itemPath;
    protected final VersionSpec version;
    protected final int deletionId;
    protected final RecursionType recursion;
    protected final String user;
    protected final VersionSpec versionFrom;
    protected final VersionSpec versionTo;
    protected final int maxCount;
    protected final boolean includeFileDetails;
    protected final boolean slotMode;
    protected final boolean generateDurls;
    protected final boolean sortAscending;

    private Changeset[] changesets;

    public QueryHistoryCommand(
        final TFSRepository repository,
        final String itemPath,
        final VersionSpec version,
        final RecursionType recursion,
        final VersionSpec versionFrom,
        final VersionSpec versionTo) {
        this(
            repository,
            itemPath,
            version,
            0,
            recursion,
            null,
            versionFrom,
            versionTo,
            Integer.MAX_VALUE,
            true,
            false,
            false,
            false);
    }

    public QueryHistoryCommand(
        final TFSRepository repository,
        final String itemPath,
        final VersionSpec version,
        final int deletionId,
        final RecursionType recursion,
        final String user,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final int maxCount,
        final boolean includeFileDetails,
        final boolean slotMode,
        final boolean generateDurls,
        final boolean sortAscending) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemPath, "itemPath"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$

        this.repository = repository;
        this.itemPath = itemPath;
        this.version = version;
        this.deletionId = deletionId;
        this.recursion = recursion;
        this.user = user;
        this.versionFrom = versionFrom;
        this.versionTo = versionTo;
        this.maxCount = maxCount;
        this.includeFileDetails = includeFileDetails;
        this.slotMode = slotMode;
        this.generateDurls = generateDurls;
        this.sortAscending = sortAscending;

        setConnection(repository.getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("HistoryCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, itemPath);
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("HistoryCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("HistoryCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, itemPath);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        changesets = repository.getWorkspace().queryHistory(
            itemPath,
            version,
            deletionId,
            recursion,
            user,
            versionFrom,
            versionTo,
            maxCount,
            includeFileDetails,
            slotMode,
            generateDurls,
            sortAscending);

        return Status.OK_STATUS;
    }

    public Changeset[] getChangesets() {
        return changesets;
    }
}
