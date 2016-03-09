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

public class DestroyWorkItemCommand extends TFSConnectedCommand {
    private final WorkItem workItem;

    public DestroyWorkItemCommand(final WorkItem workItem) {
        Check.notNull(workItem, "workItem"); //$NON-NLS-1$

        this.workItem = workItem;

        setConnection(workItem.getClient().getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("DestroyWorkItemCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(workItem.getFields().getID()));
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("DestroyWorkItemCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("DestroyWorkItemCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(workItem.getFields().getID()));
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        workItem.getClient().deleteWorkItemByID(workItem.getFields().getID());

        return Status.OK_STATUS;
    }
}
