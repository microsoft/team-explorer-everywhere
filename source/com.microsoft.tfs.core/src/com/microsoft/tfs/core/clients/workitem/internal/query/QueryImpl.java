// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.SupportedFeatures;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.exceptions.DuplicateBatchReadParameterException;
import com.microsoft.tfs.core.clients.workitem.exceptions.ValidationException;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.LinkQueryXMLResult;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeAndOperator;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeSelect;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Parser;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.SyntaxException;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.WIQLAdapter;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEnd;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEndCollection;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameterCollection;
import com.microsoft.tfs.core.clients.workitem.query.DisplayFieldList;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.SortFieldList;
import com.microsoft.tfs.core.clients.workitem.query.SortType;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.LinkQueryMode;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.ws.runtime.types.AnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.StaxAnyContentType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;

import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap_QueryWorkitemsResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap_QueryWorkitemsResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap_QueryWorkitemsResponse;
import ms.tfs.workitemtracking.clientservices._03._QuerySortOrderEntry;

public class QueryImpl implements Query {
    private static final Log log = LogFactory.getLog(QueryImpl.class);
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private WITContext witContext;
    private int[] batchIds;
    private int[] batchRevs;

    private DisplayFieldListImpl displayFieldList;
    private SortFieldListImpl sortFieldList;

    private NodeSelect wiqlNode;
    private Date queryAsOf;

    private Element queryXML;
    private NodeAndOperator linkGroup;

    public QueryImpl(final WITContext witContext, final String queryText) {
        initialize(witContext, queryText, null, null, null, true);
    }

    /**
     * Constructor
     *
     * @param witContext
     *        The current work item context.
     * @param queryText
     *        (WIQL query with no where or orderby elements)
     * @param batchReadParams
     *        Collection of Id/Revision pairs
     */
    public QueryImpl(
        final WITContext witContext,
        final String queryText,
        final BatchReadParameterCollection batchReadParams) {
        final int[] ids = new int[batchReadParams.getSize()];
        final int[] revs = new int[batchReadParams.getSize()];
        for (int i = 0; i < batchReadParams.getSize(); i++) {
            ids[i] = batchReadParams.getParameter(i).getID();
            revs[i] = batchReadParams.getParameter(i).getRev();
        }

        initialize(witContext, queryText, null, ids, revs, true);
    }

    /**
     * Public Constructor
     *
     * @param witContext
     *        The current work item context
     * @param queryText
     *        WIQL to execute
     * @param queryContext
     *        Map of @macros and values
     */
    public QueryImpl(final WITContext witContext, final String queryText, final Map<String, Object> queryContext) {
        initialize(witContext, queryText, queryContext, null, null, true);
    }

    /**
     * Public constructor
     *
     * @param witContext
     *        The current work item context
     * @param queryText
     *        WIQL to execute
     * @param queryContext
     *        Map of @macros and values
     * @param dayPrecision
     *        if true all time values are ignored and DateTimes are treated as
     *        Dates
     */
    public QueryImpl(
        final WITContext witContext,
        final String queryText,
        final Map<String, Object> queryContext,
        final boolean dayPrecision) {
        initialize(witContext, queryText, queryContext, null, null, dayPrecision);
    }

    public QueryImpl(final WITContext witContext, final String queryText, final int[] ids, final int[] revs) {
        initialize(witContext, queryText, null, copyArray(ids), copyArray(revs), true);
    }

    public QueryImpl(final WITContext witContext, final String queryText, final int[] ids) {
        initialize(witContext, queryText, null, copyArray(ids), null, true);
    }

    /***************************************************************************
     * START of implementation of Query interface
     **************************************************************************/

    @Override
    public DisplayFieldList getDisplayFieldList() {
        return displayFieldList;
    }

    @Override
    public SortFieldList getSortFieldList() {
        if (isBatchReadMode()) {
            throw new IllegalStateException("sort field list is invalid for a parameterized query"); //$NON-NLS-1$
        }
        return sortFieldList;
    }

    @Override
    public WorkItemCollection runQuery() {
        if (isBatchReadMode()) {
            return runBatchReadQuery();
        } else {
            return runNormalQuery();
        }
    }

    @Override
    public boolean isLinkQuery() {
        return ((LinkQueryMode) wiqlNode.getFrom().getTag()).getValue() > LinkQueryMode.WORK_ITEMS.getValue();
    }

    @Override
    public boolean isTreeQuery() {
        return wiqlNode.getFrom().getTag() == LinkQueryMode.LINKS_RECURSIVE;
    }

    @Override
    public boolean isBatchReadMode() {
        return batchIds != null;
    }

    @Override
    public WorkItemLinkInfo[] runLinkQuery() {
        final DOMAnyContentType psQuery = new DOMAnyContentType(new Element[] {
            queryXML
        });

        if (log.isTraceEnabled()) {
            traceQuery();
        }

        if (!isLinkQuery()) {
            throw new ValidationException(
                Messages.getString("QueryImpl.QueryStringNotValidForMethodWithLinkedWorkItems")); //$NON-NLS-1$
        }

        final AnyContentType metadata;
        final String dbStamp;
        final AnyContentType resultIds;
        final Calendar asOfDate;
        final _QuerySortOrderEntry[] sort = sortFieldList.getLinksSortOrder(isTreeQuery());
        if (witContext.isVersion2()) {
            throw new NotSupportedException(Messages.getString("QueryImpl.ServerDoesNotSupportLinkQueryType")); //$NON-NLS-1$
        } else if (witContext.isVersion3()) {
            final _ClientService3Soap_QueryWorkitemsResponse response = witContext.getProxy3().queryWorkitems(
                psQuery,
                sort,
                false,
                witContext.getMetadataUpdateHandler().getHaveEntries(),
                new DOMAnyContentType(),
                new StaxAnyContentType());
            metadata = response.getMetadata();
            dbStamp = response.getDbStamp();
            resultIds = response.getResultIds();
            asOfDate = response.getAsOfDate();
        } else {
            final _ClientService5Soap_QueryWorkitemsResponse response = witContext.getProxy5().queryWorkitems(
                psQuery,
                sort,
                false,
                witContext.getMetadataUpdateHandler().getHaveEntries(),
                new DOMAnyContentType(),
                new StaxAnyContentType());
            metadata = response.getMetadata();
            dbStamp = response.getDbStamp();
            resultIds = response.getResultIds();
            asOfDate = response.getAsOfDate();
        }

        witContext.getMetadataUpdateHandler().updateMetadata(metadata, dbStamp);

        metadata.dispose();

        final LinkQueryResultParser parser =
            new LinkQueryResultParser(((DOMAnyContentType) resultIds).getElements()[0]);

        Calendar resultAsOf;
        if (queryAsOf != null) {
            resultAsOf = Calendar.getInstance();
            resultAsOf.setTime(queryAsOf);
        } else {
            resultAsOf = asOfDate;
        }

        final WorkItemRelation[] links = parser.parse();

        if (log.isDebugEnabled()) {
            final int linksLength = (links == null ? 0 : links.length);
            log.debug(
                MessageFormat.format("runLinkQuery returned {0} WorkItemRelation[{1}]", linksLength, linksLength)); //$NON-NLS-1$
        }

        // Got WorkItemRelation array back - convert to WorkItemLinkInfo
        // hierarchy
        if (isTreeQuery()) {
            return convertTreeResult(links);
        }

        if (sort.length == 0) {
            // no sort criteria sent to server. Sort values on client
            log.trace("<<< Sorting linking query on the client"); //$NON-NLS-1$
            boolean ascending = true;
            if (sortFieldList.getSize() == 1) {
                ascending = sortFieldList.get(0).getSortType() == SortType.ASCENDING;
            }
            Arrays.sort(links, new WorkItemRelationComparator(ascending));
            log.trace("<<< Sorting Done"); //$NON-NLS-1$
        }

        return convertOneHopResult(links);
    }

    /***************************************************************************
     * END of implementation of Query interface
     **************************************************************************/

    /***************************************************************************
     * START of internal (QueryImpl) methods
     **************************************************************************/

    /**
     * Convert query results for tree query.
     */
    private WorkItemLinkInfo[] convertTreeResult(final WorkItemRelation[] relations) {
        if (relations == null) {
            return new WorkItemLinkInfo[0];
        }
        final WorkItemLinkInfo[] links = new WorkItemLinkInfo[relations.length];
        for (int i = 0; i < relations.length; i++) {
            links[i] = new WorkItemLinkInfo(
                relations[i].getSourceID(),
                relations[i].getTargetID(),
                relations[i].getLinkTypeID(),
                relations[i].isLocked());
        }
        return links;
    }

    /**
     * Convert query result for one-hop query
     */
    private WorkItemLinkInfo[] convertOneHopResult(final WorkItemRelation[] relations) {
        // calculate number of extra rows that we need to add
        int extra = 0;
        int root = 0;
        for (int i = 0; i < relations.length; i++) {
            if (relations[i].getSourceID() != root && relations[i].getTargetID() != 0) {
                extra++;
            }
            root = relations[i].getSourceID();
        }

        final WorkItemLinkInfo[] links = new WorkItemLinkInfo[relations.length + extra];

        root = 0;
        int index = 0;
        for (int i = 0; i < relations.length; i++) {
            final WorkItemRelation r = relations[i];
            if (r.getSourceID() != root) {
                // add root record
                links[index++] = new WorkItemLinkInfo(0, r.getSourceID(), 0, false);
                root = r.getSourceID();
            }
            if (r.getTargetID() != 0) {
                // add leaf record
                links[index++] =
                    new WorkItemLinkInfo(r.getSourceID(), r.getTargetID(), r.getLinkTypeID(), r.isLocked());
            }
        }

        return links;
    }

    private WorkItemCollection runBatchReadQuery() {
        return new WorkItemCollectionImpl(batchIds, batchRevs, this, witContext);
    }

    private WorkItemCollection runNormalQuery() {
        final DOMAnyContentType psQuery = new DOMAnyContentType(new Element[] {
            queryXML
        });

        if (log.isTraceEnabled()) {
            traceQuery();
        }

        if (isLinkQuery() || isBatchReadMode()) {
            throw new ValidationException(
                Messages.getString("QueryImpl.QueryStringNotValidForMethodWithFlatWorkItemList")); //$NON-NLS-1$
        }

        final AnyContentType metadata;
        final String dbStamp;
        final AnyContentType resultIds;
        final Calendar asOfDate;
        if (witContext.isVersion2()) {
            final _ClientService2Soap_QueryWorkitemsResponse response = witContext.getProxy().queryWorkitems(
                psQuery,
                sortFieldList.getSortOrderEntries(),
                false,
                witContext.getMetadataUpdateHandler().getHaveEntries(),
                new DOMAnyContentType(),
                new StaxAnyContentType());
            metadata = response.getMetadata();
            dbStamp = response.getDbStamp();
            resultIds = response.getResultIds();
            asOfDate = response.getAsOfDate();
        } else if (witContext.isVersion3()) {
            final _ClientService3Soap_QueryWorkitemsResponse response = witContext.getProxy3().queryWorkitems(
                psQuery,
                sortFieldList.getSortOrderEntries(),
                false,
                witContext.getMetadataUpdateHandler().getHaveEntries(),
                new DOMAnyContentType(),
                new StaxAnyContentType());
            metadata = response.getMetadata();
            dbStamp = response.getDbStamp();
            resultIds = response.getResultIds();
            asOfDate = response.getAsOfDate();
        } else {
            final _ClientService5Soap_QueryWorkitemsResponse response = witContext.getProxy5().queryWorkitems(
                psQuery,
                sortFieldList.getSortOrderEntries(),
                false,
                witContext.getMetadataUpdateHandler().getHaveEntries(),
                new DOMAnyContentType(),
                new StaxAnyContentType());
            metadata = response.getMetadata();
            dbStamp = response.getDbStamp();
            resultIds = response.getResultIds();
            asOfDate = response.getAsOfDate();
        }

        witContext.getMetadataUpdateHandler().updateMetadata(metadata, dbStamp);

        metadata.dispose();

        final QueryResultParser parser = new QueryResultParser(((DOMAnyContentType) resultIds).getElements()[0]);

        Calendar resultAsOf;
        if (queryAsOf != null) {
            resultAsOf = Calendar.getInstance();
            resultAsOf.setTime(queryAsOf);
        } else {
            resultAsOf = asOfDate;
        }

        final int[] ids = parser.parseIDs();

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format(
                "queryWorkitems returned {0} id{1}", //$NON-NLS-1$
                (ids == null ? 0 : ids.length),
                (ids != null && ids.length != 1 ? "s" : ""))); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return new WorkItemCollectionImpl(ids, resultAsOf, this, witContext);
    }

    private void initialize(
        final WITContext witContext,
        final String wiql,
        final Map<String, Object> queryContext,
        final int[] ids,
        final int[] revs,
        final boolean dayPrecision) {
        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("using WIQL: [{0}]", wiql)); //$NON-NLS-1$
        }

        Check.notNull(witContext, "witContext"); //$NON-NLS-1$
        Check.notNull(wiql, "wiql"); //$NON-NLS-1$

        if (ids != null) {
            if (revs != null && ids.length != revs.length) {
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        "Should be equal numbers of Ids and Revs passed. ids=int[{0}], revs=[{1}]", //$NON-NLS-1$
                        ids.length,
                        revs.length));
            }

            // Check all ID's are unique
            final HashSet<Integer> idSet = new HashSet<Integer>(ids.length);
            for (int i = 0; i < ids.length; i++) {
                if (idSet.contains(new Integer(ids[i]))) {
                    throw new DuplicateBatchReadParameterException();
                }

                idSet.add(new Integer(ids[i]));
            }

        }

        this.witContext = witContext;

        try {
            final WIQLAdapter wiqlAdapter = new WIQLAdapter(witContext);
            wiqlAdapter.setContext(queryContext);
            wiqlAdapter.setDayPrecision(dayPrecision);

            // Parse syntax
            wiqlNode = Parser.parseSyntax(wiql);
            wiqlNode.bind(wiqlAdapter, null, null);

            final boolean serverSupportsWIQLEvaluation =
                witContext.getServerInfo().isSupported(SupportedFeatures.WIQL_EVALUATION_ON_SERVER);

            if (!serverSupportsWIQLEvaluation) {
                // Optimize on the client
                wiqlNode = (NodeSelect) wiqlNode.optimize(wiqlAdapter, null, null);
            }

            if (ids != null) {
                // Check for where clause in batch mode
                if (wiqlNode.getWhere() != null || wiqlNode.getOrderBy() != null) {
                    throw new ValidationException(
                        Messages.getString("QueryImpl.WhereAndOrderByClausesNotSupportedOnParameterizedQuery")); //$NON-NLS-1$
                }

                if (isLinkQuery()) {
                    throw new ValidationException(
                        Messages.getString("QueryImpl.FromClauseCannotSpecifyLinksOnParameterizedQuery")); //$NON-NLS-1$
                }

                batchIds = ids;
                batchRevs = revs;
            } else {
                if (serverSupportsWIQLEvaluation) {
                    // Provide WIQL statement to the server as it is and
                    // include context for macros resolution (e.g. current
                    // project and team names)
                    queryXML = wiqlAdapter.getQueryXML(wiql, queryContext, isLinkQuery(), dayPrecision);
                } else {
                    // Use parser to build the query XML
                    if (isLinkQuery()) {
                        final LinkQueryXMLResult result = wiqlAdapter.getLinkQueryXML(wiqlNode);
                        queryXML = result.getLinkXML();
                        linkGroup = result.getLinkGroup();
                    } else {
                        queryXML = wiqlAdapter.getQueryXML(wiqlNode);
                    }
                }
            }

            queryAsOf = wiqlAdapter.getAsOfUTC(wiqlNode);
            // resultAsOf = queryAsOf;

            // Create display field list
            displayFieldList = (DisplayFieldListImpl) wiqlAdapter.getDisplayFieldList(witContext, wiqlNode);

            // Create sort field list
            sortFieldList = (SortFieldListImpl) wiqlAdapter.getSortFieldList(witContext, wiqlNode);
        } catch (final SyntaxException ex) {
            throw new InvalidQueryTextException(ex.getDetails(), wiql, ex);
        }
    }

    public WorkItemLinkTypeEnd[] getLinkTypes() {
        // return null for non-link queries
        if (!isLinkQuery()) {
            return null;
        }

        if (linkGroup != null) {
            // calculate and copy types
            final WIQLAdapter wiqlAdapter = new WIQLAdapter(witContext);
            final Map map = wiqlAdapter.computeLinkTypes(linkGroup);
            final WorkItemLinkTypeEnd[] types = new WorkItemLinkTypeEnd[map.size()];
            int index = 0;
            for (final Iterator<Integer> it = map.keySet().iterator(); it.hasNext();) {
                final int id = it.next().intValue();
                types[index++] = witContext.getClient().getLinkTypes().getLinkTypeEnds().getByID(id);
            }
            return types;
        }

        // All types
        final WorkItemLinkTypeEndCollection linkTypes = witContext.getClient().getLinkTypes().getLinkTypeEnds();
        return linkTypes.toArray(new WorkItemLinkTypeEnd[linkTypes.getCount()]);
    }

    private void traceQuery() {
        final StringBuffer sb = new StringBuffer();

        sb.append("running query:").append(NEWLINE); //$NON-NLS-1$
        sb.append(DOMSerializeUtils.toString(queryXML)).append(NEWLINE);

        sb.append("sort: "); //$NON-NLS-1$
        final _QuerySortOrderEntry[] sortOrderEntries = sortFieldList.getSortOrderEntries();
        for (int i = 0; i < sortOrderEntries.length; i++) {
            sb.append(sortOrderEntries[i].getColumnName()).append(" "); //$NON-NLS-1$
            sb.append(sortOrderEntries[i].isAscending() ? "ASC" : "DESC"); //$NON-NLS-1$ //$NON-NLS-2$
            if (i < sortOrderEntries.length - 1) {
                sb.append(" "); //$NON-NLS-1$
            }
        }
        sb.append(NEWLINE);
        String mode = "flat"; //$NON-NLS-1$
        if (isLinkQuery()) {
            if (isTreeQuery()) {
                mode = "tree"; //$NON-NLS-1$
            } else {
                mode = "one-hop"; //$NON-NLS-1$
            }
        }
        sb.append("mode:" + mode); //$NON-NLS-1$

        log.trace(sb.toString());
    }

    /**
     * Helper method to provide a safe copy of the passed array
     */
    private int[] copyArray(final int[] toCopy) {
        if (toCopy == null) {
            return null;
        }
        final int[] retArray = new int[toCopy.length];
        System.arraycopy(toCopy, 0, retArray, 0, toCopy.length);
        return retArray;
    }

    @Override
    public WorkItemClient getWorkItemClient() {
        return witContext.getClient();
    }

    /***************************************************************************
     * END of internal (QueryImpl) methods
     **************************************************************************/
}
