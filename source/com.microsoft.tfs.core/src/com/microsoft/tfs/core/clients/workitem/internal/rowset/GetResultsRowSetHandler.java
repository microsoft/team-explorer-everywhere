// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;

/**
 * A table handler that handles the WorkItemInfo table returned from the
 * GetWorkItem call.
 */
public class GetResultsRowSetHandler implements RowSetParseHandler {
    private final List<String> columns = new ArrayList<String>();
    private final WorkItemImpl workItem;
    private boolean parsedRow;

    public GetResultsRowSetHandler(final WorkItemImpl workItem) {
        this.workItem = workItem;
    }

    @Override
    public void handleBeginParsing() {
        columns.clear();
        parsedRow = false;
    }

    @Override
    public void handleTableName(final String tableName) {
    }

    @Override
    public void handleColumn(final String name, final String type) {
        columns.add(name);
    }

    @Override
    public void handleFinishedColumns() {
    }

    @Override
    public void handleRow(final String[] rowValues) {
        if (parsedRow) {
            throw new IllegalStateException("unexpected multiple rows in WorkItemInfo rowset"); //$NON-NLS-1$
        }
        parsedRow = true;

        for (int i = 0; i < rowValues.length; i++) {
            final String fieldReferenceName = columns.get(i);
            workItem.getFieldsInternal().addOriginalFieldValueFromServer(fieldReferenceName, rowValues[i], false);
        }
    }

    @Override
    public void handleEndParsing() {
    }

    public boolean parsedRow() {
        return parsedRow;
    }
}
