// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results.data;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.query.qe.DisplayField;

public class WorkItemCollectionQueryResult implements QueryResultData {
    private final Query query;
    private final WorkItemCollection collection;

    public WorkItemCollectionQueryResult(final Query query, final WorkItemCollection collection) {
        this.query = query;
        this.collection = collection;
    }

    @Override
    public int getCount() {
        return collection.size();
    }

    @Override
    public WorkItem getItem(final int index) {
        return collection.getWorkItem(index);
    }

    public boolean isSortable(final String fieldName) {

        return false;
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public int getLevel(final int displayRowIndex) {
        return 0;
    }

    @Override
    public String getFieldValue(final int displayRowIndex, final DisplayField[] displayFields, final int fieldIndex) {
        return null;
    }

}
