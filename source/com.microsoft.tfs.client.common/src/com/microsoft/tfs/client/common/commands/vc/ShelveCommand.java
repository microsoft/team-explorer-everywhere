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
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.CheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ShelveException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class ShelveCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final Shelveset shelveset;
    private final PendingChange[] pendingChanges;
    private final boolean replace;
    private final boolean move;

    private final NonFatalCommandHelper nonFatalHelper;

    public ShelveCommand(
        final TFSRepository repository,
        final Shelveset shelveset,
        final PendingChange[] pendingChanges,
        final boolean replace,
        final boolean move) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$
        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

        this.repository = repository;
        this.shelveset = shelveset;
        this.pendingChanges = pendingChanges;
        this.replace = replace;
        this.move = move;

        nonFatalHelper = new NonFatalCommandHelper(repository);

        setConnection(repository.getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("ShelveCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, pendingChanges.length, shelveset.getName());
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("ShelveCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("ShelveCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, pendingChanges.length, shelveset.getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        try {
            repository.getWorkspace().shelve(shelveset, pendingChanges, replace, move);
        } catch (final ShelveException e) {
            Exception bestException = e;

            /* ShelveExceptions may wrap CheckinExceptions */
            if (e.getCause() != null && e.getCause() instanceof CheckinException) {
                bestException = (CheckinException) e.getCause();

                /* Which may wrap detailed VersionControlExceptions */
                if (bestException.getCause() != null && bestException.getCause() instanceof VersionControlException) {
                    bestException = (VersionControlException) bestException.getCause();
                }
            }

            return new Status(
                IStatus.ERROR,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                bestException.getLocalizedMessage(),
                null);
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (nonFatalHelper.hasNonFatals()) {
            return nonFatalHelper.getMultiStatus(IStatus.WARNING, null);
        }

        return Status.OK_STATUS;
    }
}
