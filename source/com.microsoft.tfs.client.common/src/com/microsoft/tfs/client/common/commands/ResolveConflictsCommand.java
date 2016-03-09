// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class ResolveConflictsCommand extends TFSConnectedCommand {
    private final Object lock = new Object();

    private final TFSRepository repository;
    private final ConflictResolution[] resolutions;
    private ConflictResolutionStatus[] statuses;

    private final List<ResolveConflictsCompletedListener> resolveCompletedListeners =
        new ArrayList<ResolveConflictsCompletedListener>();

    public ResolveConflictsCommand(final TFSRepository repository, final ConflictResolution resolution) {
        this(repository, new ConflictResolution[] {
            resolution
        });
    }

    public ResolveConflictsCommand(final TFSRepository repository, final ConflictResolution[] resolutions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(resolutions, "resolutions"); //$NON-NLS-1$

        this.repository = repository;
        this.resolutions = resolutions;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        if (resolutions.length == 1) {
            return (Messages.getString("ResolveConflictsCommand.SingleConflictCommandText")); //$NON-NLS-1$
        } else {
            return (Messages.getString("ResolveConflictsCommand.MultiConflictCommandText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        if (resolutions.length == 1) {
            return (Messages.getString("ResolveConflictsCommand.SingleConflictErrorText")); //$NON-NLS-1$
        } else {
            return (Messages.getString("ResolveConflictsCommand.MultiConflictErrorText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (resolutions.length == 1) {
            return (Messages.getString("ResolveConflictsCommand.SingleConflictCommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
        } else {
            return (Messages.getString("ResolveConflictsCommand.MultiConflictCommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
        }
    }

    public void addCompletedListener(final ResolveConflictsCompletedListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (resolveCompletedListeners) {
            resolveCompletedListeners.add(listener);
        }
    }

    public void removeCompletedListener(final ResolveConflictsCompletedListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (resolveCompletedListeners) {
            resolveCompletedListeners.remove(listener);
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        synchronized (lock) {
            final String message =
                (resolutions.length == 1) ? Messages.getString("ResolveConflictsCommand.SingleConflictProgressText") //$NON-NLS-1$
                    : Messages.getString("ResolveConflictsCommand.MultiConflictProgressText"); //$NON-NLS-1$
            progressMonitor.beginTask(message, resolutions.length);

            statuses = new ConflictResolutionStatus[resolutions.length];

            for (int i = 0; i < resolutions.length; i++) {
                statuses[i] = resolutions[i].resolveConflict();
                progressMonitor.worked(1);

                final Conflict conflict = resolutions[i].getConflictDescription().getConflict();
                if (conflict.isResolved()) {
                    repository.getConflictManager().removeConflict(conflict);
                }
            }

            progressMonitor.done();
        }

        for (final Iterator<ResolveConflictsCompletedListener> i = resolveCompletedListeners.iterator(); i.hasNext();) {
            i.next().resolveConflictsCompleted(this);
        }

        return Status.OK_STATUS;
    }

    public ConflictResolutionStatus getStatusForResolution(final ConflictResolution resolution) {
        Check.notNull(statuses, "statuses"); //$NON-NLS-1$

        synchronized (lock) {
            for (int i = 0; i < resolutions.length; i++) {
                if (resolutions[i].equals(resolution)) {
                    return statuses[i];
                }
            }
        }

        return null;
    }

    public ConflictResolution[] getResolutions() {
        synchronized (lock) {
            return resolutions;
        }
    }

    public ConflictResolutionStatus[] getStatuses() {
        synchronized (lock) {
            return statuses;
        }
    }

    public interface ResolveConflictsCompletedListener {
        void resolveConflictsCompleted(ResolveConflictsCommand command);
    }
}
