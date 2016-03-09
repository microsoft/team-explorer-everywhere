// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.GetStoredQueriesRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.RowSetParser;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;

/**
 * <p>
 * Equivalent to:
 * Microsoft.TeamFoundation.WorkItemTracking.Client.StoredQueryBucket.
 * </p>
 * <p>
 * This class is almost an exact copy of StoredQueryBucket in Visual Studio's
 * object model. A StoredQueryBucket is responsible for calling the
 * GetStoredQueries webservice and implementing the client-side stored query
 * caching / incremental stored query update mechanism. Each instance of a
 * StoredQueryBucket is specific to a particular team project ID.
 * </p>
 */
public class StoredQueryBucket {
    private static final Log log = LogFactory.getLog(StoredQueryBucket.class);

    /*
     * The ID of the project that this bucket holds queries for.
     */
    private final int projectId;

    /*
     * A List of StoredQueryImpl - this is the important data held by this
     * bucket.
     */
    private final List<StoredQuery> queries = new ArrayList<StoredQuery>();

    /*
     * Used in the stored query caching mechanism to receive incremental updates
     * to this bucket.
     */
    private long rowVersion;

    /*
     * The WITContext
     */
    private final WITContext witContext;

    public StoredQueryBucket(final int projectId, final WITContext witContext) {
        this.projectId = projectId;
        this.witContext = witContext;

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format("created a new StoredQueryBucket: {0}", toString())); //$NON-NLS-1$
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "p={0} q={1} r={2} h={3}", //$NON-NLS-1$
            Integer.toString(projectId),
            queries.size(),
            Long.toString(rowVersion),
            Integer.toHexString(System.identityHashCode(this)));
    }

    public void add(final StoredQueryImpl storedQuery) {
        /*
         * Here we take advantage of the .equals() method of StoredQueryImpl
         * which compares GUIDs.
         */
        if (!queries.contains(storedQuery)) {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("adding query [{0}]: {1}", storedQuery, toString())); //$NON-NLS-1$
            }
            queries.add(storedQuery);
        }
    }

    public void refresh() {
        DOMAnyContentType payload;

        if (witContext.isVersion2()) {
            payload = (DOMAnyContentType) witContext.getProxy().getStoredQueries(
                rowVersion,
                projectId,
                new DOMAnyContentType());
        } else if (witContext.isVersion3()) {
            payload = (DOMAnyContentType) witContext.getProxy3().getStoredQueries(
                rowVersion,
                projectId,
                new DOMAnyContentType());
        } else {
            payload = (DOMAnyContentType) witContext.getProxy5().getStoredQueries(
                rowVersion,
                projectId,
                new DOMAnyContentType());
        }

        final RowSetParser parser = new RowSetParser();
        final GetStoredQueriesRowSetHandler handler = new GetStoredQueriesRowSetHandler(witContext);

        parser.parse(payload.getElements()[0], handler);

        final StoredQueryImpl[] serverQueries = handler.getQueries();

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format(
                "refresh, got response length {0} from GetStoredQueries: {1}", //$NON-NLS-1$
                serverQueries.length,
                toString()));
        }

        for (int i = 0; i < serverQueries.length; i++) {
            /*
             * ensure that the row version of this bucket is the max of any
             * stored query row version
             */
            if (serverQueries[i].getRowVersion() > rowVersion) {
                rowVersion = serverQueries[i].getRowVersion();
            }

            if (serverQueries[i].isDeleted()) {
                remove(serverQueries[i]);
                witContext.getQueryProvider().getQueryMap().remove(serverQueries[i].getQueryGUID());

                if (log.isDebugEnabled()) {
                    log.debug(MessageFormat.format("refresh, deleted query [{0}]: {1}", serverQueries[i], toString())); //$NON-NLS-1$
                }
            } else {
                if (witContext.getQueryProvider().getQueryMap().containsKey(serverQueries[i].getQueryGUID())) {
                    remove(serverQueries[i]);
                    witContext.getQueryProvider().getQueryMap().remove(serverQueries[i].getQueryGUID());

                    if (log.isDebugEnabled()) {
                        log.debug(MessageFormat.format(
                            "refresh, got update for existing query [{0}]: {1}", //$NON-NLS-1$
                            serverQueries[i].getQueryGUID(),
                            toString()));
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug(MessageFormat.format("refresh, got new query [{0}]: {1}", serverQueries[i], toString())); //$NON-NLS-1$
                }
                add(serverQueries[i]);
                witContext.getQueryProvider().getQueryMap().put(serverQueries[i].getQueryGUID(), serverQueries[i]);
            }
        }

        /*
         * StoredQuery extends Comparable
         */
        Collections.sort(queries);
    }

    public void remove(final StoredQueryImpl query) {
        /*
         * Here we take advantage of the .equals() method of StoredQueryImpl
         * which compares GUIDs.
         */
        if (queries.remove(query)) {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("deleted query [{0}]: {1}", query, toString())); //$NON-NLS-1$
            }
        }
    }

    public List<StoredQuery> getQueryList() {
        return queries;
    }
}
