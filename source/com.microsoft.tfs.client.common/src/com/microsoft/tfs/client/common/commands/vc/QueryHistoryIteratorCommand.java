// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

public class QueryHistoryIteratorCommand extends QueryHistoryCommand {
    private Iterator<Changeset> iterator = new ArrayList<Changeset>().iterator();

    public QueryHistoryIteratorCommand(
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
        super(
            repository,
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
    }

    public QueryHistoryIteratorCommand(
        final TFSRepository repository,
        final String itemPath,
        final VersionSpec version,
        final RecursionType recursion,
        final VersionSpec versionFrom,
        final VersionSpec versionTo) {
        super(repository, itemPath, version, recursion, versionFrom, versionTo);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        iterator = repository.getWorkspace().queryHistoryIterator(
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

    public Iterator<Changeset> getIterator() {
        return iterator;
    }
}
