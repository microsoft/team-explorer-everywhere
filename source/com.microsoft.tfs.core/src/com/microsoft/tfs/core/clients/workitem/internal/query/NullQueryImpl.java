// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.query.DisplayFieldList;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.SortFieldList;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;

public class NullQueryImpl implements Query {
    private final WITContext witContext;
    private final DisplayFieldListImpl displayFieldList;
    private final SortFieldListImpl sortFieldList;

    public NullQueryImpl(final WITContext witContext) {
        this.witContext = witContext;
        displayFieldList = new DisplayFieldListImpl(witContext);
        sortFieldList = new SortFieldListImpl(witContext);
    }

    @Override
    public DisplayFieldList getDisplayFieldList() {
        return displayFieldList;
    }

    @Override
    public SortFieldList getSortFieldList() {
        return sortFieldList;
    }

    @Override
    public WorkItemCollection runQuery() {
        return new WorkItemCollectionImpl(new int[] {}, Calendar.getInstance(), this, witContext);
    }

    @Override
    public boolean isBatchReadMode() {
        return false;
    }

    @Override
    public boolean isLinkQuery() {
        return false;
    }

    @Override
    public boolean isTreeQuery() {
        return false;
    }

    @Override
    public WorkItemLinkInfo[] runLinkQuery() {
        return new WorkItemLinkInfo[0];
    }

    @Override
    public WorkItemClient getWorkItemClient() {
        return witContext.getClient();
    }
}
