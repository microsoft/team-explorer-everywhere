// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.vc.BranchObjectManager;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class ConvertFolderToBranchCommand extends TFSConnectedCommand {
    private final TFSRepository repository;

    private final String item, owner, desc;

    private final boolean recurse;

    public static final CodeMarker CODEMARKER_CONVERTTOBRANCH_COMPLETE = new CodeMarker(
        "com.microsoft.tfs.client.common.commands.vc.ConvertFolderToBranchCommand#ConvertToBranchComplete"); //$NON-NLS-1$

    public ConvertFolderToBranchCommand(
        final TFSRepository repository,
        final String item,
        final String owner,
        final String desc,
        final boolean recurse) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.item = item;
        this.owner = owner;
        this.desc = desc;
        this.recurse = recurse;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("ConvertFolderToBranchCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("ConvertFolderToBranchCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("ConvertFolderToBranchCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            new BranchObjectManager(repository.getWorkspace()).convertFolderToBranchObject(item, owner, desc, recurse);
        } catch (final Exception e) {
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), null);
        } finally {
            CodeMarkerDispatch.dispatch(CODEMARKER_CONVERTTOBRANCH_COMPLETE);
        }

        return Status.OK_STATUS;
    }
}
