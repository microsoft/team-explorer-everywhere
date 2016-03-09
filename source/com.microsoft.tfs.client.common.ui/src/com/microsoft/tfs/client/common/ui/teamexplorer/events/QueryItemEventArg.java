// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.events;

import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;

public class QueryItemEventArg extends TeamExplorerEventArg {
    private final QueryItem queryItem;

    public QueryItemEventArg(final QueryItem queryItem) {
        this.queryItem = queryItem;
    }

    public QueryItem getQueryItem() {
        return queryItem;
    }
}
