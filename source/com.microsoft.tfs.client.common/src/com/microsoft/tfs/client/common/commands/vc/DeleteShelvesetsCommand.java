// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteShelvesetsCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final Shelveset[] shelvesets;

    private final List deletedShelvesets = new ArrayList();

    private final List failures = new ArrayList();

    public DeleteShelvesetsCommand(final TFSRepository repository, final Shelveset shelveset) {
        this(repository, new Shelveset[] {
            shelveset
        });
    }

    public DeleteShelvesetsCommand(final TFSRepository repository, final Shelveset[] shelvesets) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(shelvesets, "shelvesets"); //$NON-NLS-1$

        this.repository = repository;
        this.shelvesets = shelvesets;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        if (shelvesets.length == 1) {
            final String messageFormat = Messages.getString("DeleteShelvesetsCommand.DeletingShelvesetFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, shelvesets[0].getName());
        } else {
            return Messages.getString("DeleteShelvesetsCommand.DeletingShelvesets"); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("DeleteShelvesetsCommand.ErrorDeletingShelveset")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        if (shelvesets.length == 1) {
            final String messageFormat =
                Messages.getString("DeleteShelvesetsCommand.DeletingShelvesetFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, shelvesets[0].getName());
        } else {
            return Messages.getString("DeleteShelvesetsCommand.DeletingShelvesets", LocaleUtil.ROOT); //$NON-NLS-1$
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        String message = (shelvesets.length == 1) ? Messages.getString("DeleteShelvesetsCommand.DeletingShelveSet") //$NON-NLS-1$
            : Messages.getString("DeleteShelvesetsCommand.DeletingShelvesets"); //$NON-NLS-1$
        progressMonitor.beginTask(message, shelvesets.length);

        for (int i = 0; i < shelvesets.length; i++) {
            try {
                repository.getVersionControlClient().deleteShelveset(
                    shelvesets[i].getName(),
                    shelvesets[i].getOwnerName());
                deletedShelvesets.add(shelvesets[i]);
            } catch (final Exception e) {
                final String messageFormat =
                    Messages.getString("DeleteShelvesetsCommand.CouldNotDeleteShelvesetFormat"); //$NON-NLS-1$
                message = MessageFormat.format(
                    messageFormat,
                    shelvesets[i].getName(),
                    TFSCommandExceptionHandler.getErrorMessage(e.getLocalizedMessage()));
                failures.add(new Status(Status.ERROR, getPluginID(), 0, message, null));
            } finally {
                progressMonitor.worked(1);
            }
        }

        if (failures.size() == 1) {
            return (IStatus) failures.get(0);
        } else if (failures.size() > 0) {
            final String messageFormat = Messages.getString("DeleteShelvesetsCommand.CouldNotDeleteShelvesetsFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, failures.size());

            return new MultiStatus(
                getPluginID(),
                0,
                (IStatus[]) failures.toArray(new IStatus[failures.size()]),
                message,
                null);
        }

        return Status.OK_STATUS;
    }

    public Shelveset[] getDeletedShelvesets() {
        return (Shelveset[]) deletedShelvesets.toArray(new Shelveset[deletedShelvesets.size()]);
    }

    public String getPluginID() {
        return TFSCommonClientPlugin.PLUGIN_ID;
    }
}