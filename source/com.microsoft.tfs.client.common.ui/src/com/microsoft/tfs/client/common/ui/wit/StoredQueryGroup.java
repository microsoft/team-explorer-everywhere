// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.util.Check;

public class StoredQueryGroup {
    public static StoredQueryGroup[] createGroupsFromProject(final Project project) {
        final StoredQueryGroup publicQueries = new StoredQueryGroup(project, QueryScope.PUBLIC);
        final StoredQueryGroup privateQueries = new StoredQueryGroup(project, QueryScope.PRIVATE);

        if (privateQueries.getQueries().length > 0) {
            return new StoredQueryGroup[] {
                privateQueries,
                publicQueries
            };
        }
        return new StoredQueryGroup[] {
            publicQueries
        };
    }

    private final Project project;
    private final QueryScope queryScope;
    private final StoredQuery[] queries;

    public StoredQueryGroup(final Project project, final QueryScope queryScope) {
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(queryScope, "queryScope"); //$NON-NLS-1$

        this.project = project;
        this.queryScope = queryScope;
        queries = project.getStoredQueries().getQueriesByScope(queryScope);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof StoredQueryGroup) {
            final StoredQueryGroup other = (StoredQueryGroup) obj;
            return project.equals(other.project) && queryScope.equals(other.queryScope);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return project.hashCode() + queryScope.hashCode();
    }

    public Project getProject() {
        return project;
    }

    public String getLabel() {
        return (queryScope == QueryScope.PUBLIC ? Messages.getString("StoredQueryGroup.PublicQueryText") //$NON-NLS-1$
            : Messages.getString("StoredQueryGroup.PrivateQueryText")); //$NON-NLS-1$
    }

    public StoredQuery[] getQueries() {
        return queries;
    }

    public QueryScope getQueryScope() {
        return queryScope;
    }
}
