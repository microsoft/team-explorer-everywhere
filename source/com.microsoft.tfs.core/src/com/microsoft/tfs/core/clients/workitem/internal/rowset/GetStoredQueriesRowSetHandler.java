// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.internal.GetStoredQueriesFieldNames;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.util.GUID;

/**
 * A table handler that handles the table returned from the GetStoredQueries
 * call.
 */
public class GetStoredQueriesRowSetHandler extends BaseRowSetHandler {
    private final WITContext witContext;
    private final Set<StoredQueryImpl> storedQueries = new HashSet<StoredQueryImpl>();

    public GetStoredQueriesRowSetHandler(final WITContext witContext) {
        this.witContext = witContext;
    }

    @Override
    public void handleBeginParsing() {
        super.handleBeginParsing();
        storedQueries.clear();
    }

    @Override
    protected void doHandleRow() {
        QueryScope queryScope;

        if (getBooleanValue(GetStoredQueriesFieldNames.PUBLIC)) {
            queryScope = QueryScope.PUBLIC;
        } else {
            queryScope = QueryScope.PRIVATE;
        }

        final int projectId = getIntValue(GetStoredQueriesFieldNames.PROJECT_ID);
        ProjectImpl project = null;

        if (projectId != 0) {
            final ProjectCollection projects = witContext.getClient().getProjects();
            for (final Iterator<Project> it = projects.iterator(); it.hasNext();) {
                final ProjectImpl currentProject = (ProjectImpl) it.next();
                if (currentProject.getID() == projectId) {
                    project = currentProject;
                    break;
                }
            }
        }

        final GUID id = new GUID(getStringValue(GetStoredQueriesFieldNames.ID));

        final StoredQueryImpl query = new StoredQueryImpl(
            id,
            getStringValue(GetStoredQueriesFieldNames.QUERY_NAME),
            getStringValue(GetStoredQueriesFieldNames.QUERY_TEXT),
            getStringValue(GetStoredQueriesFieldNames.DESCRIPTION),
            getStringValue(GetStoredQueriesFieldNames.OWNER),
            getDateValue(GetStoredQueriesFieldNames.CREATE_TIME),
            getDateValue(GetStoredQueriesFieldNames.LAST_WRITE_TIME),
            queryScope,
            projectId,
            project,
            getBooleanValue(GetStoredQueriesFieldNames.DELETED),
            getLongValue(GetStoredQueriesFieldNames.CACHESTAMP),
            witContext.getQueryProvider(),
            witContext);

        storedQueries.add(query);
    }

    public StoredQueryImpl[] getQueries() {
        return storedQueries.toArray(new StoredQueryImpl[] {});
    }
}
