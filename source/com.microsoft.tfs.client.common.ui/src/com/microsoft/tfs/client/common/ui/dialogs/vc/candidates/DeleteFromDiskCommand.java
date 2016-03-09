// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteFromDiskCommand extends Command {
    private final static Log log = LogFactory.getLog(DeleteFromDiskCommand.class);

    private final ChangeItem[] changes;
    private final List<ChangeItem> successfulDeletes = new ArrayList<ChangeItem>();

    public DeleteFromDiskCommand(final ChangeItem[] changes) {
        setCancellable(true);
        this.changes = changes;
    }

    @Override
    public String getName() {
        return Messages.getString("DeleteFromDiskCommand.DeleteCommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("DeleteFromDiskCommand.DeleteCommandErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("DeleteFromDiskCommand.DeleteCommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final List<IStatus> errorStatuses = new ArrayList<IStatus>();

        progressMonitor.beginTask("", changes.length); //$NON-NLS-1$

        try {
            for (final ChangeItem change : changes) {
                if (progressMonitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                final String path = change.getPendingChange().getLocalItem();
                progressMonitor.subTask(path);

                if (path == null) {
                    errorStatuses.add(
                        new Status(
                            Status.ERROR,
                            TFSCommonUIClientPlugin.PLUGIN_ID,
                            MessageFormat.format(
                                Messages.getString("DeleteFromDiskCommand.ChangeHasNoLocalItemFormat"), //$NON-NLS-1$
                                change.getPendingChange().getLocalItem())));
                } else {
                    final File file = new File(path);
                    try {
                        if (file.delete()) {
                            successfulDeletes.add(change);
                        } else {
                            log.info(MessageFormat.format("Error deleting file {0}, no info available", file)); //$NON-NLS-1$

                            errorStatuses.add(
                                new Status(
                                    Status.ERROR,
                                    TFSCommonUIClientPlugin.PLUGIN_ID,
                                    MessageFormat.format(
                                        Messages.getString("DeleteFromDiskCommand.FileDeletedErrorNoInfoFormat"), //$NON-NLS-1$
                                        file)));
                        }
                    } catch (final Throwable t) {
                        log.info(MessageFormat.format("Error deleting file {0}", file), t); //$NON-NLS-1$

                        errorStatuses.add(
                            new Status(
                                Status.ERROR,
                                TFSCommonUIClientPlugin.PLUGIN_ID,
                                MessageFormat.format(
                                    Messages.getString("DeleteFromDiskCommand.FileDeletionErrorFormat"), //$NON-NLS-1$
                                    file),
                                t));
                    }
                }

                progressMonitor.worked(1);
            }
        } finally {
            progressMonitor.done();
        }

        if (errorStatuses.size() > 0) {
            return new MultiStatus(
                TFSCommonUIClientPlugin.PLUGIN_ID,
                Status.ERROR,
                errorStatuses.toArray(new IStatus[errorStatuses.size()]),
                Messages.getString("DeleteFromDiskCommand.MultiStatusText"), //$NON-NLS-1$
                null);
        }

        return Status.OK_STATUS;
    }

    public List<ChangeItem> getSuccessfulDeletes() {
        return successfulDeletes;
    }
}
