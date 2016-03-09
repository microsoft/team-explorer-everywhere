// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectImpl;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.query.StoredQueryCollection;
import com.microsoft.tfs.util.GUID;

/**
 * <p>
 * Equivalent to:
 * Microsoft.TeamFoundation.WorkItemTracking.Client.StoredQueryCollection
 * </p>
 * <p>
 * This class is a very close copy of Visual Studio's StoredQueryCollection. A
 * few convenience methods (getQueryByNameAndScope, getQueriesByScope) have been
 * added and a number of unneeded methods have been ommitted.
 * </p>
 * <p>
 * A StoredQueryCollection allows access to a Project's stored queries.
 * </p>
 */
public class StoredQueryCollectionImpl implements StoredQueryCollection {
    private final ProjectImpl project;
    private final int projectId;
    private StoredQueryBucket queryBucket;
    private List<StoredQuery> queryList;
    private StoredQueryProviderImpl queryProvider;
    private final WITContext witContext;

    public StoredQueryCollectionImpl(final ProjectImpl project, final WITContext witContext) {
        this.project = project;
        projectId = project.getID();
        this.witContext = witContext;

        initialize();
    }

    /*
     * ************************************************************************
     * START of implementation of StoredQueryCollection interface
     * ***********************************************************************
     */

    @Override
    public void add(final StoredQuery storedQuery) throws InvalidQueryTextException {
        if (storedQuery == null) {
            throw new IllegalArgumentException("storedQuery must not be null"); //$NON-NLS-1$
        }
        if (storedQuery.isSaved()) {
            throw new IllegalArgumentException("the input stored query is already saved"); //$NON-NLS-1$
        }

        final StoredQueryImpl internalQuery = (StoredQueryImpl) storedQuery;

        internalQuery.projectId = projectId;
        internalQuery.witContext = witContext;
        internalQuery.queryProvider = queryProvider;
        internalQuery.project = project;

        queryProvider.addStoredQuery(internalQuery);

        project.notifyModicationListeners();
    }

    /*
     * Note that several methods from Visual Studio's StoredQueryCollection are
     * not implemented as we have no use for them.
     */

    /*
     * Equivalent to:
     * Microsoft.TeamFoundation.WorkItemTracking.Client.StoredQueryCollection
     * .Item[Int32]
     */
    @Override
    public StoredQuery getQuery(final int index) {
        final StoredQueryBucket bucket1 = queryProvider.getQueryBucket(projectId);
        return bucket1.getQueryList().get(index);
    }

    @Override
    public void refresh() {
        queryProvider.refresh(projectId);
    }

    @Override
    public void remove(final StoredQuery storedQuery) {
        if (storedQuery == null) {
            throw new IllegalArgumentException("storedQuery must not be null"); //$NON-NLS-1$
        }
        queryProvider.deleteStoredQuery((StoredQueryImpl) storedQuery);

        project.notifyModicationListeners();

    }

    /*
     * Equivalent to:
     * Microsoft.TeamFoundation.WorkItemTracking.Client.StoredQueryCollection
     * .Count
     */
    @Override
    public int size() {
        return queryProvider.getQueryBucket(projectId).getQueryList().size();
    }

    @Override
    public StoredQuery getQueryByNameAndScope(final String name, final QueryScope scope) {
        final StoredQuery[] queries = queryList.toArray(new StoredQuery[queryList.size()]);

        for (int i = 0; i < queries.length; i++) {
            if ((scope == null || queries[i].getQueryScope() == scope) &&

            /*
             * I18N: need to use a java.text.Collator with a specified Locale
             */
                (queries[i].getName().equalsIgnoreCase(name))) {
                return queries[i];
            }
        }
        return null;
    }

    @Override
    public StoredQuery[] getQueriesByScope(final QueryScope scope) {
        final StoredQuery[] allQueries = queryList.toArray(new StoredQuery[queryList.size()]);
        final List<StoredQuery> queriesByScope = new ArrayList<StoredQuery>();

        for (int i = 0; i < allQueries.length; i++) {
            if ((scope == null || allQueries[i].getQueryScope() == scope)) {
                queriesByScope.add(allQueries[i]);
            }
        }

        return queriesByScope.toArray(new StoredQuery[queriesByScope.size()]);
    }

    @Override
    public StoredQuery getByGUID(final GUID guid) {
        return queryProvider.getQuery(guid);
    }

    /*
     * ************************************************************************
     * END of implementation of StoredQueryCollection interface
     * ***********************************************************************
     */

    private void initialize() {
        queryProvider = witContext.getQueryProvider();
        queryBucket = queryProvider.getQueryBucket(projectId);
        queryList = queryBucket.getQueryList();
    }

    public ProjectImpl getProjectInternal() {
        return project;
    }
}
