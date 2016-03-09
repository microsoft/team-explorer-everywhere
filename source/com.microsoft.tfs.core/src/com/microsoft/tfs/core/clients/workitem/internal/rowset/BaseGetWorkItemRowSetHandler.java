// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadata;

/**
 * The base table handler class for all the table handlers that work with tables
 * coming back from the GetWorkItem call.
 */
public abstract class BaseGetWorkItemRowSetHandler extends BaseRowSetHandler {
    private final WorkItemImpl workItem;
    private final IMetadata metadata;

    public BaseGetWorkItemRowSetHandler(final WorkItemImpl workItem, final IMetadata metadata) {
        this.workItem = workItem;
        this.metadata = metadata;
    }

    protected WorkItemImpl getWorkItem() {
        return workItem;
    }

    protected IMetadata getMetadata() {
        return metadata;
    }
}
