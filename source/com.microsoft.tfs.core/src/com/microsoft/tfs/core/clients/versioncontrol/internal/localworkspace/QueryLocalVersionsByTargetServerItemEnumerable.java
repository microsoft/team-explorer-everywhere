// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.util.Iterator;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;

public class QueryLocalVersionsByTargetServerItemEnumerable implements Iterable<WorkspaceLocalItem> {
    private final WorkspaceVersionTable lv;
    private final String targetServerItem;
    private final RecursionType recursion;
    private final String pattern;
    private final boolean includeDeleted;
    private final Iterable<CommittedServerItemQuery> queries;

    public QueryLocalVersionsByTargetServerItemEnumerable(
        final WorkspaceVersionTable lv,
        final String targetServerItem,
        final RecursionType recursion,
        final String pattern,
        final boolean includeDeleted,
        final Iterable<CommittedServerItemQuery> queries) {
        this.lv = lv;
        this.targetServerItem = targetServerItem;
        this.recursion = recursion;
        this.pattern = pattern;
        this.includeDeleted = includeDeleted;
        this.queries = queries;
    }

    @Override
    public Iterator<WorkspaceLocalItem> iterator() {
        return new QueryLocalVersionsByTargetServerItemEnumerator(
            lv,
            targetServerItem,
            recursion,
            pattern,
            includeDeleted,
            queries);
    }
}
