// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.EnumSubTreeOptions;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.EnumeratedSparseTreeNode;
import com.microsoft.tfs.util.NotYetImplementedException;

public class WorkspaceLocalItemEnumerable implements Iterator<WorkspaceLocalItem>, Iterable<WorkspaceLocalItem> {
    private WorkspaceLocalItem current;

    // Next item to be enumerated. Used by ServerItemMoveNext to generate 2
    // items from 1 WorkspaceLocalItemPair
    private WorkspaceLocalItem onDeck;

    private interface MoveNextDelegate {
        public boolean invoke();
    }

    private final MoveNextDelegate moveNextDelegate;
    private Iterator<EnumeratedSparseTreeNode<WorkspaceLocalItem>> localEnumerator;
    private Iterator<EnumeratedSparseTreeNode<WorkspaceLocalItemPair>> serverEnumerator;

    private String pattern;
    private final boolean includeDeleted;
    private CommittedState committedState; // Server item only

    /**
     *
     *
     *
     * @param lv
     * @param recursion
     * @param localItem
     * @param pattern
     * @param includeDeleted
     */
    public WorkspaceLocalItemEnumerable(
        final WorkspaceVersionTable lv,
        final RecursionType recursion,
        final String localItem,
        final String pattern,
        final boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
        this.pattern = ("*" == pattern) ? null : pattern; //$NON-NLS-1$

        EnumSubTreeOptions options = EnumSubTreeOptions.NONE;
        if (pattern == null) {
            options = EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT;
        }

        final int depth = depthFromRecursionType(recursion);
        this.localEnumerator = lv.local.EnumSubTree(localItem, options, depth).iterator();

        this.moveNextDelegate = new MoveNextDelegate() {
            @Override
            public boolean invoke() {
                return localItemMoveNext();
            }
        };
    }

    /**
     *
     *
     *
     * @param lv
     * @param recursion
     * @param serverItem
     * @param committedState
     * @param pattern
     * @param includeDeleted
     */
    public WorkspaceLocalItemEnumerable(
        final WorkspaceVersionTable lv,
        final RecursionType recursion,
        final String serverItem,
        final CommittedState committedState,
        final String pattern,
        final boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
        this.pattern = ("*" == pattern) ? null : pattern; //$NON-NLS-1$
        this.committedState = committedState;

        EnumSubTreeOptions options = EnumSubTreeOptions.NONE;
        if (pattern == null) {
            options = EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT;
        }

        final int depth = depthFromRecursionType(recursion);
        this.serverEnumerator = lv.server.EnumSubTree(serverItem, options, depth).iterator();

        this.moveNextDelegate = new MoveNextDelegate() {
            @Override
            public boolean invoke() {
                return serverItemMoveNext();
            }
        };
    }

    boolean moveNext() {
        return moveNextDelegate.invoke();
    }

    private boolean localItemMoveNext() {
        while (localEnumerator.hasNext()) {
            final EnumeratedSparseTreeNode<WorkspaceLocalItem> currentItem = localEnumerator.next();

            final boolean patternCondition =
                (null == pattern) || ItemPath.matchesWildcardFile(LocalPath.getFileName(currentItem.token), pattern);

            final boolean includeDeletedCondition = includeDeleted || !currentItem.referencedObject.isDeleted();

            if (patternCondition && includeDeletedCondition) {
                current = currentItem.referencedObject;
                return true;
            }
        }

        current = null;
        return false;
    }

    private boolean serverItemMoveNext() {
        if (null != onDeck) {
            current = onDeck;
            onDeck = null;
            return true;
        }

        while (serverEnumerator.hasNext()) {
            final EnumeratedSparseTreeNode<WorkspaceLocalItemPair> currentItem = serverEnumerator.next();

            if (null == pattern || ItemPath.matchesWildcardFile(ServerPath.getFileName(currentItem.token), pattern)) {
                current = null;
                final WorkspaceLocalItemPair pair = currentItem.referencedObject;

                if (null != pair.getCommitted() && committedState.contains(CommittedState.COMMITTED)) {
                    final WorkspaceLocalItem committed = pair.getCommitted();
                    if (includeDeleted || !committed.isDeleted()) {
                        current = committed;
                    }
                }

                if (null != pair.getUncommitted() && committedState.contains(CommittedState.UNCOMMITTED)) {
                    final WorkspaceLocalItem uncommitted = pair.getUncommitted();
                    if (includeDeleted || !uncommitted.isDeleted()) {
                        if (null == current) {
                            current = uncommitted;
                        } else {
                            // We'll enumerate this the next time we're called.
                            onDeck = uncommitted;
                        }
                    }
                }

                if (null != current) {
                    return true;
                }
            }
        }

        current = null;
        return false;
    }

    @Override
    public Iterator<WorkspaceLocalItem> iterator() {
        return this;
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

    /**
     * Given a RecursionType, return the depth to use for a SparseTree
     * EnumSubTree call.
     */
    private static int depthFromRecursionType(final RecursionType recursion) {
        if (recursion == RecursionType.NONE) {
            return 0;
        } else if (recursion == RecursionType.ONE_LEVEL) {
            return 1;
        } else {
            return Integer.MAX_VALUE;
        }
    }
}
