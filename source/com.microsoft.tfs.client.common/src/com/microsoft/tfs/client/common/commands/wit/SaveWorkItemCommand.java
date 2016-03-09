// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.wit;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class SaveWorkItemCommand extends TFSConnectedCommand {
    private final WorkItem workItem;

    public SaveWorkItemCommand(final WorkItem workItem) {
        Check.notNull(workItem, "workItem"); //$NON-NLS-1$

        this.workItem = workItem;

        setConnection(workItem.getClient().getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        if (workItem.getFields().getID() == 0) {
            return Messages.getString("SaveWorkItemCommand.CommandText"); //$NON-NLS-1$
        } else {
            return MessageFormat.format(
                Messages.getString("SaveWorkItemCommand.CommandTextFormat"), //$NON-NLS-1$
                Integer.toString(workItem.getFields().getID()));
        }
    }

    @Override
    public String getLoggingDescription() {
        if (workItem.getFields().getID() == 0) {
            return Messages.getString("SaveWorkItemCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
        } else {
            return MessageFormat.format(
                Messages.getString("SaveWorkItemCommand.CommandTextFormat", LocaleUtil.ROOT), //$NON-NLS-1$
                Integer.toString(workItem.getFields().getID()));
        }
    }

    @Override
    public String getErrorDescription() {
        if (workItem.getFields().getID() == 0) {
            return Messages.getString("SaveWorkItemCommand.ErrorText"); //$NON-NLS-1$
        } else {
            return MessageFormat.format(
                Messages.getString("SaveWorkItemCommand.ErrorTextFormat"), //$NON-NLS-1$
                Integer.toString(workItem.getFields().getID()));
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        workItem.save();
        return Status.OK_STATUS;
    }
}
