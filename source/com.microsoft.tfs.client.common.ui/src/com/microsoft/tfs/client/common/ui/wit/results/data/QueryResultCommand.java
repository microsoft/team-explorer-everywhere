// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results.data;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;

public class QueryResultCommand extends TFSCommand {
    private WorkItemClient client;
    private String wiql;
    private Map<String, Object> queryContext;
    private Query query = null;
    private QueryResultData queryResultData = null;

    /* Optional data, for logging, may be null */
    private String projectName;
    private final String name;

    public QueryResultCommand(
        final WorkItemClient client,
        final String wiql,
        final Map<String, Object> queryContext,
        final String projectName,
        final String name) {
        super();
        this.client = client;
        this.wiql = wiql;
        this.queryContext = queryContext;
        this.projectName = projectName;
        this.name = name;
    }

    public QueryResultCommand(final Query query, final String projectName, final String name) {
        super();
        this.query = query;
        this.name = name;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (query == null) {
            query = client.createQuery(wiql, queryContext);
        }

        if (query.isLinkQuery()) {
            final WorkItemLinkInfo[] linkInfos = query.runLinkQuery();
            queryResultData = new WorkItemInfoQueryResult(query, linkInfos);
            return Status.OK_STATUS;
        }

        // Run a normal query
        final WorkItemCollection results = query.runQuery();
        queryResultData = new WorkItemCollectionQueryResult(query, results);
        return Status.OK_STATUS;
    }

    public QueryResultData getQueryResultData() {
        return queryResultData;
    }

    @Override
    public String getName() {
        return Messages.getString("QueryResultCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("QueryResultCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        if (name == null) {
            return "Executing (anonymous) work item query"; //$NON-NLS-1$
        }

        if (projectName == null) {
            return MessageFormat.format("Executing work item query {0}", name); //$NON-NLS-1$
        } else {
            return MessageFormat.format("Executing work item query {0} for project {1}", name, projectName); //$NON-NLS-1$
        }
    }
}
