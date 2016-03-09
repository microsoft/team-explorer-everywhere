// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;

/**
 * Represents a collection of work items that result from a query to the
 * {@link WorkItemClient}.
 *
 * @since TEE-SDK-10.1
 */
public interface WorkItemCollection {
    public int size();

    public Query getQuery();

    public DisplayFieldList getDisplayFieldList();

    public SortFieldList getSortFieldList();

    public WorkItem getWorkItem(int index);

    public int getPageSize();

    public void setPageSize(int pageSize);

    public int[] getIDs();
}
