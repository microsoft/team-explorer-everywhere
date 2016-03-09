// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemQueryConstants;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.internal.AccessDeniedWorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldDefinitionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.PageResultsLargeTextRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.PageResultsRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.RowSetParser;
import com.microsoft.tfs.core.clients.workitem.query.DisplayFieldList;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.SortFieldList;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.ws.runtime.types.AnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.StaxAnyContentType;
import com.microsoft.tfs.util.Check;

import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap_PageWorkitemsByIdRevsResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap_PageWorkitemsByIdsResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap_PageWorkitemsByIdRevsResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap_PageWorkitemsByIdsResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap_PageWorkitemsByIdRevsResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap_PageWorkitemsByIdsResponse;
import ms.tfs.workitemtracking.clientservices._03._IdRevisionPair;

public class WorkItemCollectionImpl implements WorkItemCollection, PageCallback {
    private static final Log log = LogFactory.getLog(WorkItemCollectionImpl.class);

    /*
     * The IDs to page in (or null in batch read mode). The length of the ids
     * array will be the same as the length of the WorkItem array (unless batch
     * read mode).
     */
    private final int[] ids;

    private final int[] revs;

    /*
     * The date to pass the paging web services
     */
    private Calendar asOfDate;

    /*
     * Reference to the Query that created this WorkItemCollection
     */
    private final Query query;

    /*
     * The WIT context
     */
    private final WITContext witContext;

    /*
     * The generic collection class to hold the array of paged-in work items.
     * PagedCollection implements the generic paging algorithm, and
     * WorkItemCollectionImpl is called back to handle the workitem-specific
     * task of calling out to the server.
     */
    private final PagedCollection pagedCollection;

    /*
     * Column names / ids passed to the paging web services. These are computed
     * when needed and then cached.
     */
    private String[] shortTextColumnNames;
    private int[] longTextColumnIds;
    private FieldDefinitionImpl[] longTextColumns;

    public WorkItemCollectionImpl(
        final int[] ids,
        final Calendar asOfDate,
        final Query query,
        final WITContext witContext) {
        Check.notNull(ids, "ids"); //$NON-NLS-1$

        this.ids = ids;
        this.asOfDate = asOfDate;
        this.query = query;
        this.witContext = witContext;
        revs = null;

        pagedCollection =
            new PagedCollection((ids != null ? ids.length : 0), WorkItemQueryConstants.DEFAULT_PAGE_SIZE, this);
    }

    public WorkItemCollectionImpl(final int[] ids, final int[] revs, final Query query, final WITContext witContext) {
        this.ids = ids;
        this.revs = revs;
        this.witContext = witContext;
        this.query = query;

        pagedCollection = new PagedCollection(ids.length, WorkItemQueryConstants.DEFAULT_PAGE_SIZE, this);
    }

    /***************************************************************************
     * START of implementation of WorkItemCollection interface
     **************************************************************************/

    @Override
    public int size() {
        return pagedCollection.getSize();
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public DisplayFieldList getDisplayFieldList() {
        return query.getDisplayFieldList();
    }

    @Override
    public SortFieldList getSortFieldList() {
        return query.getSortFieldList();
    }

    @Override
    public WorkItem getWorkItem(final int index) {
        return (WorkItem) pagedCollection.getItem(index);
    }

    @Override
    public int getPageSize() {
        return pagedCollection.getPageSize();
    }

    @Override
    public void setPageSize(final int pageSize) {
        if (pageSize > WorkItemQueryConstants.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "cannot set page size greater than the maximum of {0}", //$NON-NLS-1$
                    WorkItemQueryConstants.MAX_PAGE_SIZE));
        }
        if (pageSize < WorkItemQueryConstants.MIN_PAGE_SIZE) {
            throw new IllegalArgumentException(MessageFormat.format(
                "cannot set page size less than the minimum of {0}", //$NON-NLS-1$
                WorkItemQueryConstants.MIN_PAGE_SIZE));
        }

        pagedCollection.setPageSize(pageSize);
    }

    @Override
    public int[] getIDs() {
        return ids.clone();
    }

    /***************************************************************************
     * END of implementation of WorkItemCollection interface
     **************************************************************************/

    /***************************************************************************
     * START of implementation of PageCallback interface
     **************************************************************************/

    @Override
    public Object[] pageInItems(final int startingIx, final int length) {
        if (shortTextColumnNames == null) {
            computeColumns();
        }

        if (!query.isBatchReadMode() || revs == null) {
            return pageWorkitemsByIDs(startingIx, length);
        } else {
            return pageWorkitemsByIDRevs(startingIx, length);
        }
    }

    /***************************************************************************
     * END of implementation of PageCallback interface
     **************************************************************************/

    /***************************************************************************
     * START of internal (WorkItemCollectionImpl) methods
     **************************************************************************/

    private void computeColumns() {
        final List<String> shortTextColumnNameList = new ArrayList<String>();
        final List<FieldDefinitionImpl> longTextFieldDefinitionList = new ArrayList<FieldDefinitionImpl>();

        /*
         * add in the always-paged work item fields none of these are long text,
         * so they can be added directly to shortTextColumnNames
         */
        for (final String pageWorkItemFieldName : PageResultsRowSetHandler.getPageWorkItemsFieldNames(witContext)) {
            shortTextColumnNameList.add(pageWorkItemFieldName);
        }

        /*
         * iterate over all the specified display fields
         */
        final DisplayFieldList displayFieldList = query.getDisplayFieldList();
        for (int i = 0; i < displayFieldList.getSize(); i++) {
            FieldDefinitionImpl displayField = (FieldDefinitionImpl) displayFieldList.getField(i);

            displayField = translateCalculatedField(displayField);

            if (shortTextColumnNameList.contains(displayField.getReferenceName())) {
                /*
                 * skip duplicates (these would be either a) the always-paged
                 * fields we've already added above or b) distinct calculated
                 * fields that map to the same non-calculated field - like
                 * System.TeamProject and System.NodeName)
                 */
                continue;
            }

            if (displayField.isLargeText()) {
                longTextFieldDefinitionList.add(displayField);
            } else {
                shortTextColumnNameList.add(displayField.getReferenceName());
            }
        }

        shortTextColumnNames = shortTextColumnNameList.toArray(new String[] {});
        longTextColumns = longTextFieldDefinitionList.toArray(new FieldDefinitionImpl[] {});
        longTextColumnIds = new int[longTextFieldDefinitionList.size()];

        for (int i = 0; i < longTextFieldDefinitionList.size(); i++) {
            final FieldDefinition longTextFieldDefinition = longTextFieldDefinitionList.get(i);
            longTextColumnIds[i] = longTextFieldDefinition.getID();
        }
    }

    private FieldDefinitionImpl translateCalculatedField(final FieldDefinitionImpl field) {
        switch (field.getID()) {
            case WorkItemFieldIDs.AUTHORIZED_AS:
                return witContext.getFieldDefinitions().getFieldDefinitionInternal(WorkItemFieldIDs.PERSON_ID);

            case WorkItemFieldIDs.ITERATION_PATH:
                return witContext.getFieldDefinitions().getFieldDefinitionInternal(WorkItemFieldIDs.ITERATION_ID);

            case WorkItemFieldIDs.TEAM_PROJECT:
            case WorkItemFieldIDs.NODE_NAME:
            case WorkItemFieldIDs.AREA_PATH:
                return witContext.getFieldDefinitions().getFieldDefinitionInternal(WorkItemFieldIDs.AREA_ID);

            default:
                return field;
        }
    }

    private WorkItem[] handlePageResponse(final Element[] pageResponse, final int[] ids, final int pageStart) {
        final Element itemsTable = pageResponse[0];
        final RowSetParser parser = new RowSetParser();

        final PageResultsRowSetHandler handler = new PageResultsRowSetHandler(witContext);
        parser.parse(itemsTable, handler);

        if (pageResponse.length > 1) {
            final Element longTextTable = pageResponse[1];
            final PageResultsLargeTextRowSetHandler textHandler = new PageResultsLargeTextRowSetHandler(handler);
            parser.parse(longTextTable, textHandler);
        }

        final WorkItem[] results = new WorkItem[ids.length];
        for (int i = 0; i < ids.length; i++) {
            WorkItemImpl workItem = handler.getByID(ids[i]);

            if (workItem == null) {
                workItem = new AccessDeniedWorkItemImpl(witContext, ids[i]);
            }

            workItem.getFieldsInternal().ensureFieldsExist(longTextColumns);
            results[i] = workItem;
        }

        return results;
    }

    private WorkItem[] pageWorkitemsByIDs(final int pageStart, final int pageLength) {
        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format(
                "pageWorkitemsByIds({0},{1})", //$NON-NLS-1$
                Integer.toString(pageStart),
                Integer.toString(pageLength)));
        }

        final int[] idsToPage = new int[pageLength];
        System.arraycopy(ids, pageStart, idsToPage, 0, pageLength);

        final AnyContentType metadata;
        final AnyContentType items;

        if (witContext.isVersion2()) {
            final _ClientService2Soap_PageWorkitemsByIdsResponse response = witContext.getProxy().pageWorkitemsByIds(
                idsToPage,
                shortTextColumnNames,
                longTextColumnIds,
                asOfDate,
                false,
                witContext.getMetadataUpdateHandler().getHaveEntries(),
                new DOMAnyContentType(),
                new StaxAnyContentType());
            metadata = response.getMetadata();
            items = response.getItems();
        } else if (witContext.isVersion3()) {
            final _ClientService3Soap_PageWorkitemsByIdsResponse response = witContext.getProxy3().pageWorkitemsByIds(
                idsToPage,
                shortTextColumnNames,
                longTextColumnIds,
                asOfDate,
                false,
                witContext.getMetadataUpdateHandler().getHaveEntries(),
                new DOMAnyContentType(),
                new StaxAnyContentType());
            metadata = response.getMetadata();
            items = response.getItems();
        } else {
            final _ClientService5Soap_PageWorkitemsByIdsResponse response = witContext.getProxy5().pageWorkitemsByIds(
                idsToPage,
                shortTextColumnNames,
                longTextColumnIds,
                asOfDate,
                false,
                witContext.getMetadataUpdateHandler().getHaveEntries(),
                new DOMAnyContentType(),
                new StaxAnyContentType());
            metadata = response.getMetadata();
            items = response.getItems();
        }

        witContext.getMetadataUpdateHandler().updateMetadata(metadata, null);

        metadata.dispose();

        return handlePageResponse(((DOMAnyContentType) items).getElements(), idsToPage, pageStart);
    }

    private WorkItem[] pageWorkitemsByIDRevs(final int pageStart, final int pageLength) {
        final int[] pagedIds = new int[pageLength];
        final _IdRevisionPair[] pairs = new _IdRevisionPair[pageLength];

        for (int i = 0; i < pageLength; i++) {
            final int index = pageStart + i;
            pairs[i] = new _IdRevisionPair(ids[index], revs[index]);
            pagedIds[i] = ids[index];
        }

        final AnyContentType items;
        if (witContext.isVersion2()) {
            final _ClientService2Soap_PageWorkitemsByIdRevsResponse response =
                witContext.getProxy().pageWorkitemsByIdRevs(
                    pairs,
                    shortTextColumnNames,
                    longTextColumnIds,
                    asOfDate,
                    false,
                    new DOMAnyContentType());
            items = response.getItems();
        } else if (witContext.isVersion3()) {
            final _ClientService3Soap_PageWorkitemsByIdRevsResponse response =
                witContext.getProxy3().pageWorkitemsByIdRevs(
                    pairs,
                    shortTextColumnNames,
                    longTextColumnIds,
                    asOfDate,
                    false,
                    new DOMAnyContentType());
            items = response.getItems();
        } else {
            final _ClientService5Soap_PageWorkitemsByIdRevsResponse response =
                witContext.getProxy5().pageWorkitemsByIdRevs(
                    pairs,
                    shortTextColumnNames,
                    longTextColumnIds,
                    asOfDate,
                    false,
                    new DOMAnyContentType());
            items = response.getItems();
        }

        return handlePageResponse(((DOMAnyContentType) items).getElements(), pagedIds, pageStart);
    }

    /***************************************************************************
     * END of internal (WorkItemCollectionImpl) methods
     **************************************************************************/
}
