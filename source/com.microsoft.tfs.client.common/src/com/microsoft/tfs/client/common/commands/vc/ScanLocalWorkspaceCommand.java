// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public class ScanLocalWorkspaceCommand extends TFSCommand {
    private final TFSRepository repository;
    private final Collection<String> paths;

    public static final CodeMarker CODEMARKER_DETECT_LOCAL_CHANGES_FINISHED = new CodeMarker(
        "com.microsoft.tfs.client.common.commands.vc.ScanLocalWorkspaceCommand.#DetectLocalChangesFinished"); //$NON-NLS-1$

    public ScanLocalWorkspaceCommand(final TFSRepository repository) {
        this(repository, null);
    }

    public ScanLocalWorkspaceCommand(final TFSRepository repository, final Collection<String> paths) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.paths = paths;

        setCancellable(true);
    }

    @Override
    public String getName() {
        return Messages.getString("ScanLocalWorkspaceCommand.CommandName"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("ScanLocalWorkspaceCommand.CommandErrorDescription"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("ScanLocalWorkspaceCommand.CommandName", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            if (paths == null || paths.size() == 0) {
                repository.getPathWatcherManager().forceFullScan();
            } else {
                repository.getPathWatcherManager().notifyWatchers(paths);
            }
        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        }

        return Status.OK_STATUS;
    }
}
