// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;

/**
 * Handles the LongTextItems rowset returned from a Page* webservice method.
 */
public class PageResultsLargeTextRowSetHandler extends BaseRowSetHandler {
    private final PageResultsRowSetHandler pageHandler;
    private final Map<Integer, LongTextRowCollection> workItemIdToLongTextRowCollection =
        new HashMap<Integer, LongTextRowCollection>();

    public PageResultsLargeTextRowSetHandler(final PageResultsRowSetHandler handler) {
        pageHandler = handler;
    }

    @Override
    protected void doHandleRow() {
        final LongTextRow row = new LongTextRow(
            getDateValue("AddedDate"), //$NON-NLS-1$
            getIntValue("FldID"), //$NON-NLS-1$
            getIntValue("ID"), //$NON-NLS-1$
            getStringValue("Words")); //$NON-NLS-1$

        final Integer key = new Integer(row.workItemId);

        LongTextRowCollection collection = workItemIdToLongTextRowCollection.get(key);
        if (collection == null) {
            collection = new LongTextRowCollection();
            workItemIdToLongTextRowCollection.put(key, collection);
        }
        collection.add(row);
    }

    @Override
    public void handleEndParsing() {
        for (final Iterator<Integer> it = workItemIdToLongTextRowCollection.keySet().iterator(); it.hasNext();) {
            final Integer key = it.next();
            final int workItemId = key.intValue();
            final WorkItemImpl workItem = pageHandler.getByID(workItemId);

            if (workItem == null) {
                continue;
            }

            final LongTextRowCollection collection = workItemIdToLongTextRowCollection.get(key);
            final LongTextRow[] values = collection.values();
            for (int i = 0; i < values.length; i++) {
                workItem.getFieldsInternal().addOriginalFieldValueFromServer(values[i].fieldId, values[i].text, true);
            }
        }
    }

    private static class LongTextRowCollection {
        private final Map<Integer, LongTextRow> fieldIdToLongTextRow = new HashMap<Integer, LongTextRow>();

        public void add(final LongTextRow newRow) {
            final Integer key = new Integer(newRow.fieldId);

            final LongTextRow existingRow = fieldIdToLongTextRow.get(key);
            if (existingRow == null || newRow.addedDate.after(existingRow.addedDate)) {
                fieldIdToLongTextRow.put(key, newRow);
            }
        }

        public LongTextRow[] values() {
            return fieldIdToLongTextRow.values().toArray(new LongTextRow[fieldIdToLongTextRow.size()]);
        }
    }

    private static class LongTextRow {
        public final Date addedDate;
        public final int fieldId;
        public final int workItemId;
        public final String text;

        public LongTextRow(final Date addedDate, final int fieldId, final int workItemId, final String text) {
            this.addedDate = addedDate;
            this.fieldId = fieldId;
            this.workItemId = workItemId;
            this.text = text;
        }
    }
}
