// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.events;

import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;

public class QueryFolderEventArg extends TeamExplorerEventArg {
    private final QueryFolder queryFolder;

    public QueryFolderEventArg(final QueryFolder queryFolder) {
        this.queryFolder = queryFolder;
    }

    public QueryFolder getQueryFolder() {
        return queryFolder;
    }
}
