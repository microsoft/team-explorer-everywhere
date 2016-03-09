// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.wit;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.artifact.ArtifactIDFactory;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

public class GetWorkItemsForChangesetCommand extends TFSConnectedCommand {
    private final WorkItemClient client;
    private final int changesetId;
    private String[] extraFieldNames;

    private WorkItem[] workItems;

    public GetWorkItemsForChangesetCommand(final TFSRepository repository, final int changesetId) {
        this(repository.getVersionControlClient().getConnection().getWorkItemClient(), changesetId);
    }

    public GetWorkItemsForChangesetCommand(final TFSServer server, final int changesetId) {
        this(server.getConnection().getWorkItemClient(), changesetId);
    }

    private GetWorkItemsForChangesetCommand(final WorkItemClient client, final int changesetId) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        this.client = client;
        this.changesetId = changesetId;

        setConnection(client.getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("GetWorkItemsForChangesetCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(changesetId));
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("GetWorkItemsForChangesetCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("GetWorkItemsForChangesetCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(changesetId));
    }

    public void setExtraFieldNames(final String[] extraFieldNames) {
        this.extraFieldNames = extraFieldNames;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final TaskMonitor tm = TaskMonitorService.getTaskMonitor();
        tm.beginWithUnknownTotalWork(getName());

        final ArtifactID changesetArtifactId = ArtifactIDFactory.newChangesetArtifactID(changesetId);

        final Query query = client.createReferencingQuery(changesetArtifactId.encodeURI());

        if (tm.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        if (extraFieldNames != null) {
            for (int i = 0; i < extraFieldNames.length; i++) {
                query.getDisplayFieldList().add(extraFieldNames[i]);
            }
        }
        final WorkItemCollection collection = query.runQuery();

        final WorkItem[] workItems = new WorkItem[collection.size()];

        for (int i = 0; i < collection.size(); i++) {
            if (tm.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            workItems[i] = collection.getWorkItem(i);
        }

        this.workItems = workItems;

        return Status.OK_STATUS;
    }

    public WorkItem[] getWorkItems() {
        return workItems;
    }
}
