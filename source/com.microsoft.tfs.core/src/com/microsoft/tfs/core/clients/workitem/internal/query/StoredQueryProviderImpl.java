// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.exceptions.DeniedOrNotExistException;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.GetStoredQueriesRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.RowSetParser;
import com.microsoft.tfs.core.clients.workitem.internal.update.DeleteStoredQueryUpdatePackage;
import com.microsoft.tfs.core.clients.workitem.internal.update.InsertStoredQueryUpdatePackage;
import com.microsoft.tfs.core.clients.workitem.internal.update.UpdateStoredQueryUpdatePackage;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.exceptions.mappers.WorkItemExceptionMapper;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.GUID;

/**
 * <p>
 * Equivalent to:
 * Microsoft.TeamFoundation.WorkItemTracking.Client.StoredQueryProvider
 * </p>
 * <p>
 * This class is almost an exact copy of StoredQueryProvider in Visual Studio's
 * object model. A StoredQueryProvider manages a collection of
 * StoredQueryBuckets and also serves as an "indexer" of stored queries,
 * providing fast lookup by GUID.
 * </p>
 */
public class StoredQueryProviderImpl {
    private static final Log log = LogFactory.getLog(StoredQueryProviderImpl.class);

    /*
     * Integer (projectId) -> StoredQueryBucket
     */
    private final Map<Integer, StoredQueryBucket> buckets = new HashMap<Integer, StoredQueryBucket>();

    /*
     * String (stored query GUID) -> StoredQueryImpl We use a TreeMap with a
     * specified Comparator here so that the keys, which are GUIDs, are compared
     * case insensitive.
     */
    private final Map<GUID, StoredQuery> queryMap = new TreeMap<GUID, StoredQuery>();

    /*
     * The WITContext
     */
    private final WITContext witContext;

    public StoredQueryProviderImpl(final WITContext witContext) {
        this.witContext = witContext;
    }

    public void addStoredQuery(final StoredQueryImpl storedQuery) throws InvalidQueryTextException {
        /*
         * Call the static validateWIQL method to sanity check the stored
         * query's text. This will throw InvalidQueryTextException if the query
         * text is invalid.
         */
        StoredQueryImpl.validateWIQL(witContext, storedQuery.getQueryText());

        /*
         * Call the webservice to add the query to the database.
         */
        sendInsert(storedQuery);

        /*
         * Add the new query to the bucket for the query's project, if such a
         * bucket exists. If such a bucket does not exist we do not
         * automatically create it here.
         */
        final StoredQueryBucket bucket = buckets.get(new Integer(storedQuery.getProjectID()));
        if (bucket != null) {
            bucket.add(storedQuery);

            /*
             * StoredQuery extends Comparable
             */
            Collections.sort(bucket.getQueryList());
        }

        /*
         * Add the query to this indexer
         */
        if (!queryMap.containsKey(storedQuery.getQueryGUID())) {
            queryMap.put(storedQuery.getQueryGUID(), storedQuery);
        }
    }

    public void deleteStoredQuery(final StoredQueryImpl storedQuery) {
        /*
         * Call the webservice to delete the query from the database
         */
        sendDelete(storedQuery);

        /*
         * Set state on the StoredQuery object
         */
        storedQuery.setDeleted(true);
        storedQuery.setQueryProvider(null);

        /*
         * Remove the query from the bucket for the query's project, if such a
         * bucket exists. If such a bucket does not exist we do not
         * automatically create it here.
         */
        final StoredQueryBucket bucket = buckets.get(new Integer(storedQuery.getProjectID()));

        if (bucket != null) {
            bucket.remove(storedQuery);
        }

        /*
         * Remove the query from this indexer
         */
        queryMap.remove(storedQuery.getQueryGUID());
    }

    /*
     * Note: the following methods are not implemented as we currently have no
     * need for them: void deleteStoredQuery(int projectId, int position) int
     * getCount(int projectId)
     */

    public StoredQuery getQuery(final GUID guid) {
        /*
         * First we check to see whether this indexer already contains a query
         * for the given GUID.
         */
        if (queryMap.containsKey(guid)) {
            return queryMap.get(guid);
        }

        /*
         * If not, we then call the GetStoredQuery web service with the GUID.
         */
        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("calling webservice GetStoredQuery({0})", guid)); //$NON-NLS-1$
        }
        DOMAnyContentType payload = null;
        try {
            if (witContext.isVersion2()) {
                payload = (DOMAnyContentType) witContext.getProxy().getStoredQuery(
                    guid.getGUIDString(),
                    new DOMAnyContentType());
            } else if (witContext.isVersion3()) {
                payload = (DOMAnyContentType) witContext.getProxy3().getStoredQuery(
                    guid.getGUIDString(),
                    new DOMAnyContentType());
            } else {
                payload = (DOMAnyContentType) witContext.getProxy5().getStoredQuery(
                    guid.getGUIDString(),
                    new DOMAnyContentType());
            }
        } catch (final RuntimeException exception) {
            throw WorkItemExceptionMapper.map(exception);
        }
        final RowSetParser parser = new RowSetParser();
        final GetStoredQueriesRowSetHandler handler = new GetStoredQueriesRowSetHandler(witContext);
        parser.parse(payload.getElements()[0], handler);

        if (handler.getQueries().length != 1) {
            throw new DeniedOrNotExistException();
        }

        final StoredQueryImpl storedQuery = handler.getQueries()[0];

        /*
         * Add the newly retrieved query to this indexer
         */
        queryMap.put(storedQuery.getQueryGUID(), storedQuery);

        return storedQuery;
    }

    public StoredQueryBucket getQueryBucket(final int projectId) {
        final Integer key = new Integer(projectId);

        if (buckets.containsKey(key)) {
            return buckets.get(key);
        }

        final StoredQueryBucket bucket = new StoredQueryBucket(projectId, witContext);
        bucket.refresh();
        buckets.put(key, bucket);
        return bucket;
    }

    public void refresh(final int projectId) {
        final StoredQueryBucket bucket = buckets.get(new Integer(projectId));

        if (bucket != null) {
            bucket.refresh();
        }
    }

    private void sendDelete(final StoredQueryImpl storedQuery) {
        final DeleteStoredQueryUpdatePackage updatePackage =
            new DeleteStoredQueryUpdatePackage(storedQuery, witContext);
        updatePackage.update();

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("sent delete for: {0}", storedQuery.getQueryGUID())); //$NON-NLS-1$
        }
    }

    private void sendInsert(final StoredQueryImpl storedQuery) {
        final InsertStoredQueryUpdatePackage updatePackage =
            new InsertStoredQueryUpdatePackage(storedQuery, witContext);
        updatePackage.update();

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("sent insert for: {0}", storedQuery.getQueryGUID())); //$NON-NLS-1$
        }
    }

    private void sendUpdate(final StoredQueryImpl storedQuery) {
        final UpdateStoredQueryUpdatePackage updatePackage =
            new UpdateStoredQueryUpdatePackage(storedQuery, witContext);
        updatePackage.update();

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("sent update for: {0}", storedQuery.getQueryGUID())); //$NON-NLS-1$
        }
    }

    public void updateStoredQuery(final StoredQueryImpl storedQuery) {
        sendUpdate(storedQuery);
    }

    public Map<GUID, StoredQuery> getQueryMap() {
        return queryMap;
    }
}
