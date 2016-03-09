// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.wit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

public class QueryWorkItemsCommand extends TFSConnectedCommand {
    private final Query query;

    private WorkItem[] workItems;

    public QueryWorkItemsCommand(final Query query) {
        Check.notNull(query, "query"); //$NON-NLS-1$

        this.query = query;

        setConnection(query.getWorkItemClient().getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryWorkItemsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryWorkItemsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryWorkItemsCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final TaskMonitor tm = TaskMonitorService.getTaskMonitor();
        tm.beginWithUnknownTotalWork(getName());

        final WorkItemCollection collection = getUniqueWorkItemsFromQuery(query);

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

    private WorkItemCollection getUniqueWorkItemsFromQuery(final Query query) {
        if (!query.isLinkQuery()) {
            return query.runQuery();
        }

        // this is a link query - flatten results into a WorkItemCollection
        // for back-compat with older controls that do not know how to
        // display a tree of work item results.
        final WorkItemLinkInfo[] links = query.runLinkQuery();

        // Build up a list of unique ID's
        final Set idSet = new HashSet();
        final List idList = new ArrayList();
        for (int i = 0; i < links.length; i++) {
            final Integer sourceId = new Integer(links[i].getSourceID());
            final Integer targetId = new Integer(links[i].getTargetID());

            if (sourceId.intValue() != 0 && !idSet.contains(sourceId)) {
                idSet.add(sourceId);
                idList.add(sourceId);
            }
            if (targetId.intValue() != 0 && !idSet.contains(targetId)) {
                idSet.add(targetId);
                idList.add(targetId);
            }
        }

        // Build up a WIQL string that contains the IDs that we need to query.
        final StringBuffer wiql = new StringBuffer();

        // Field we need to select all the fields in the original order
        wiql.append("SELECT "); //$NON-NLS-1$

        for (int i = 0; i < query.getDisplayFieldList().getSize(); i++) {
            final FieldDefinition fd = query.getDisplayFieldList().getField(i);
            if (!fd.getReferenceName().equalsIgnoreCase(CoreFieldReferenceNames.LINK_TYPE)) {
                wiql.append("[" + query.getDisplayFieldList().getField(i).getReferenceName() + "],"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        // remove trailing comma
        wiql.setLength(wiql.length() - 1);

        if (idList.size() == 0) {
            // No work items to get, just generate dummy collection with no
            // results
            wiql.append(" FROM WorkItems where [System.ID] = -1"); //$NON-NLS-1$
            return query.getWorkItemClient().query(wiql.toString());
        }

        // We have work items to get.
        wiql.append(" FROM WorkItems"); //$NON-NLS-1$

        final int[] idInts = new int[idList.size()];
        for (int i = 0; i < idInts.length; i++) {
            idInts[i] = ((Integer) idList.get(i)).intValue();
        }
        return query.getWorkItemClient().query(idInts, wiql.toString());
    }

    public WorkItem[] getWorkItems() {
        return workItems;
    }
}
