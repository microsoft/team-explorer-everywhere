// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.UndonePendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public class UndoCommand extends TFSCommand {
    private final TFSRepository repository;
    private final ItemSpec[] items;
    private final GetOptions getOptions;

    private final NonFatalCommandHelper nonFatalHelper;

    private int undoCount;

    private final boolean queryConflicts = true;
    private ConflictDescription[] conflictDescriptions = new ConflictDescription[0];

    public UndoCommand(final TFSRepository repository, final ItemSpec[] items) {
        this(repository, items, GetOptions.NONE);
    }

    public UndoCommand(final TFSRepository repository, final ItemSpec[] items, final GetOptions getOptions) {
        Check.notNull(items, "items"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.items = items;
        this.getOptions = getOptions;

        nonFatalHelper = new NonFatalCommandHelper(repository);

        setCancellable(true);
    }

    @Override
    public String getName() {
        if (items.length == 1) {
            final String messageFormat = Messages.getString("UndoCommand.SingleCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, items[0].getItem());
        } else {
            final String messageFormat = Messages.getString("UndoCommand.MultiCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, items.length);
        }
    }

    @Override
    public String getErrorDescription() {
        if (items.length == 1) {
            return (Messages.getString("UndoCommand.SingleChangeErrorText")); //$NON-NLS-1$
        } else {
            return (Messages.getString("UndoCommand.MutiChangeErrorText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (items.length == 1) {
            final String messageFormat = Messages.getString("UndoCommand.SingleCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, items[0].getItem());
        } else {
            final String messageFormat = Messages.getString("UndoCommand.MultiCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, items.length);
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        try {
            final UndoConflictListener conflictListener = new UndoConflictListener();

            repository.getWorkspace().getClient().getEventEngine().addUndonePendingChangeListener(conflictListener);

            try {
                undoCount = repository.getWorkspace().undo(items, getOptions);
            } finally {
                repository.getWorkspace().getClient().getEventEngine().removeUndonePendingChangeListener(
                    conflictListener);
            }

            if (queryConflicts && conflictListener.hasConflicts()) {
                final QueryConflictsCommand queryCommand = new QueryConflictsCommand(repository, items);

                final IStatus queryStatus = queryCommand.run(new SubProgressMonitor(progressMonitor, 1));

                if (!queryStatus.isOK()) {
                    return queryStatus;
                }

                conflictDescriptions = queryCommand.getConflictDescriptions();
            }
        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (hasConflicts()) {
            return new Status(
                IStatus.WARNING,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("UndoCommand.ConflictsWhileUndo"), //$NON-NLS-1$
                null);
        }

        return Status.OK_STATUS;
    }

    public int getUndoCount() {
        return undoCount;
    }

    public boolean hasConflicts() {
        return (conflictDescriptions.length > 0);
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions;
    }

    private final static class UndoConflictListener implements UndonePendingChangeListener {
        private boolean hasConflicts = false;

        @Override
        public void onUndonePendingChange(final PendingChangeEvent e) {
            if (e.getOperationStatus() == OperationStatus.TARGET_WRITABLE
                || e.getOperationStatus() == OperationStatus.TARGET_IS_DIRECTORY) {
                hasConflicts = true;
            }
        }

        public boolean hasConflicts() {
            return hasConflicts;
        }
    }
}
