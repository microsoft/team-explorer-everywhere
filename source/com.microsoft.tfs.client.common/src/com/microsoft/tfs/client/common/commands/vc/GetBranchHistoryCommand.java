// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetBranchHistoryCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final ItemSpec[] itemSpecs;
    private final VersionSpec versionSpec = LatestVersionSpec.INSTANCE;

    private BranchHistory[] branchHistory;

    public GetBranchHistoryCommand(final TFSRepository repository, final ItemSpec itemSpec) {
        this(repository, new ItemSpec[] {
            itemSpec
        });
    }

    public GetBranchHistoryCommand(final TFSRepository repository, final ItemSpec[] itemSpecs) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("GetBranchHistoryCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("GetBranchHistoryCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("GetBranchHistoryCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        branchHistory = repository.getWorkspace().getBranchHistory(itemSpecs, versionSpec);

        return Status.OK_STATUS;
    }

    public BranchHistory[] getBranchHistory() {
        return branchHistory;
    }
}
