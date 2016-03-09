// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;

public class CommittedServerItemQuery {
    private final String committedServerItem;
    private final RecursionType recursion;
    private final List<String> excludedItems;

    public CommittedServerItemQuery(final String committedServerItem, final RecursionType recursion) {
        this.committedServerItem = committedServerItem;
        this.recursion = recursion;
        this.excludedItems = new ArrayList<String>();
    }

    public String getCommittedServerItem() {
        return committedServerItem;
    }

    public RecursionType getRecursionType() {
        return recursion;
    }

    public List<String> getExcludedItems() {
        return excludedItems;
    }
}
