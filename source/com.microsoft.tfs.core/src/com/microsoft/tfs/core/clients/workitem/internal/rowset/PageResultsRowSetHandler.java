// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;

/**
 * Handles the Items rowset returned from a Page* webservice method. This
 * handler creates work item instances.
 */
public class PageResultsRowSetHandler implements RowSetParseHandler {
    /**
     * The set of paged work item field names for Dev10 and before.
     */
    private static String[] PAGE_WORK_ITEMS_FIELD_NAMES_V1 = new String[] {
        CoreFieldReferenceNames.ID,
        CoreFieldReferenceNames.REVISION,
        CoreFieldReferenceNames.AREA_ID,
        CoreFieldReferenceNames.WORK_ITEM_TYPE,
        CoreFieldReferenceNames.CHANGED_DATE
    };

    /**
     * The set of paged work item field names for Dev11 and beyond.
     */
    private static String[] PAGE_WORK_ITEMS_FIELD_NAMES_V2 = new String[] {
        CoreFieldReferenceNames.ID,
        CoreFieldReferenceNames.REVISION,
        CoreFieldReferenceNames.AREA_ID,
        CoreFieldReferenceNames.WORK_ITEM_TYPE,
        CoreFieldReferenceNames.AUTHORIZED_DATE
    };

    private final List<String> columnNames = new ArrayList<String>();
    private final Map<Integer, WorkItemImpl> workItems = new HashMap<Integer, WorkItemImpl>();

    private final WITContext witContext;

    public PageResultsRowSetHandler(final WITContext witContext) {
        this.witContext = witContext;
    }

    public static String[] getPageWorkItemsFieldNames(final WITContext witContext) {
        return witContext.getFieldDefinitions().contains(CoreFieldReferenceNames.AUTHORIZED_DATE)
            ? PAGE_WORK_ITEMS_FIELD_NAMES_V2 : PAGE_WORK_ITEMS_FIELD_NAMES_V1;
    }

    @Override
    public void handleBeginParsing() {
        columnNames.clear();
        workItems.clear();
    }

    @Override
    public void handleTableName(final String tableName) {
    }

    @Override
    public void handleColumn(final String name, final String type) {
        columnNames.add(name);
    }

    @Override
    public void handleFinishedColumns() {
        for (final String requiredFieldReferenceName : getPageWorkItemsFieldNames(witContext)) {
            if (!columnNames.contains(requiredFieldReferenceName)) {
                throw new IllegalStateException(
                    MessageFormat.format(
                        "required field [{0}] was not contained in page results columns: [{1}]", //$NON-NLS-1$
                        requiredFieldReferenceName,
                        columnNames));
            }
        }
    }

    @Override
    public void handleRow(final String[] rowValues) {
        final WorkItemImpl workItem = new WorkItemImpl(witContext);

        for (int i = 0; i < rowValues.length; i++) {
            final String fieldReferenceName = columnNames.get(i);
            final String fieldValueAsString = rowValues[i];
            workItem.getFieldsInternal().addOriginalFieldValueFromServer(fieldReferenceName, fieldValueAsString, true);
        }

        workItems.put(new Integer(workItem.getFields().getID()), workItem);
    }

    @Override
    public void handleEndParsing() {
    }

    public WorkItemImpl getByID(final Integer id) {
        return workItems.get(id);
    }

    public WorkItemImpl getByID(final int id) {
        return getByID(new Integer(id));
    }
}
