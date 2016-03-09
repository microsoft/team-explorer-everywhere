// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.util.NotYetImplementedException;

public class QueryLocalVersionsByTargetServerItemEnumerator implements Iterator<WorkspaceLocalItem> {
    private WorkspaceLocalItem current;
    private CommittedServerItemQuery query;
    private Iterator<WorkspaceLocalItem> queryIt;
    private boolean enumeratedUncommittedItems;
    private int excludedItemIndex;

    private final WorkspaceVersionTable lv;
    private final String targetServerItem;
    private final RecursionType recursion;
    private final String pattern;
    private final boolean includeDeleted;
    private final Iterator<CommittedServerItemQuery> queries;

    public QueryLocalVersionsByTargetServerItemEnumerator(
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
        this.queries = queries.iterator();
    }

    public boolean moveNext() {
        while (true) {
            if (null == queryIt) {
                // We don't have a current query, so let's go get one.
                if (queries.hasNext()) {
                    query = queries.next();
                    queryIt = lv.queryByServerItem(
                        query.getCommittedServerItem(),
                        query.getRecursionType(),
                        pattern,
                        CommittedState.COMMITTED,
                        includeDeleted).iterator();

                    excludedItemIndex = 0;
                } else if (!enumeratedUncommittedItems) {
                    // Last enumerator -- the one which matches all uncommitted
                    // server items
                    // under the target server item
                    queryIt = lv.queryByServerItem(
                        targetServerItem,
                        recursion,
                        pattern,
                        CommittedState.UNCOMMITTED,
                        includeDeleted).iterator();

                    enumeratedUncommittedItems = true;
                } else {
                    current = null;
                    return false;
                }
            }

            while (queryIt.hasNext()) {
                final WorkspaceLocalItem currentItem = queryIt.next();
                boolean enumerateItem = true;

                while (null != query && excludedItemIndex < query.getExcludedItems().size()) {
                    if (ServerPath.isChild(
                        query.getExcludedItems().get(excludedItemIndex),
                        currentItem.getServerItem())) {
                        enumerateItem = false;
                    } else if (ServerPath.compareTopDown(
                        currentItem.getServerItem(),
                        query.getExcludedItems().get(excludedItemIndex)) > 0) {
                        excludedItemIndex++;
                        continue;
                    }

                    break;
                }

                if (enumerateItem) {
                    current = currentItem;
                    return true;
                }
            }

            queryIt = null;
            query = null;
        }
    }

    @Override
    public boolean hasNext() {
        if (current != null) {
            return true;
        }
        return moveNext();
    }

    @Override
    public WorkspaceLocalItem next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final WorkspaceLocalItem toReturn = current;
        moveNext();
        return toReturn;
    }

    @Override
    public void remove() {
        throw new NotYetImplementedException();
    }
}
