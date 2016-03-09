// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results.data;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.qe.DisplayField;

/**
 * Interface to remove dependency on WorkItemCollection for the query results.
 *
 * This is similar to IResultListDataProvider in the .NET object model.
 */
public interface QueryResultData {
    public String getFieldValue(int displayRowIndex, DisplayField[] displayFields, int fieldIndex);

    //
    // public int getItemId(int displayRowIndex);

    public WorkItem getItem(int displayRowIndex);

    // public WorkItem[] pageItems(int[] itemIndexers);
    //
    // public void addFieldForPaging(String fieldName);
    //
    // public int getItemIndex(int workItemId);
    //
    public int getCount();

    //
    // public int getFieldId(String fieldName);
    //
    // public boolean isSortable(String fieldName);

    public Query getQuery();

    public int getLevel(int displayRowIndex);

}
