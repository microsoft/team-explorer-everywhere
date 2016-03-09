// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.WorkItemClient;

/**
 * Represents a query to the work item tracking service.
 *
 *
 * @since TEE-SDK-10.1
 */
public interface Query {
    public DisplayFieldList getDisplayFieldList();

    public SortFieldList getSortFieldList();

    public WorkItemCollection runQuery();

    public boolean isBatchReadMode();

    public boolean isLinkQuery();

    public boolean isTreeQuery();

    public WorkItemLinkInfo[] runLinkQuery();

    public WorkItemClient getWorkItemClient();
}
