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
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetWorkItemByIDCommand extends TFSConnectedCommand {
    private final int workItemId;
    private final WorkItemClient workItemClient;
    private WorkItem workItem;

    public GetWorkItemByIDCommand(final WorkItemClient workItemClient, final int workItemId) {
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        this.workItemId = workItemId;
        this.workItemClient = workItemClient;

        setConnection(workItemClient.getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("GetWorkItemByIdCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(workItemId));
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("GetWorkItemByIdCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("GetWorkItemByIdCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(workItemId));
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        workItem = workItemClient.getWorkItemByID(workItemId);
        return Status.OK_STATUS;
    }

    public WorkItem getWorkItem() {
        return workItem;
    }
}