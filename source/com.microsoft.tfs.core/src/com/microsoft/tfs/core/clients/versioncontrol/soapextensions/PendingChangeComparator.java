// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Comparator;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

/**
 * Compare engine for {@link PendingChange} items. Supports multiple sort
 * strategies.
 *
 * @since TEE-SDK-10.1
 */
public final class PendingChangeComparator implements Comparator<PendingChange> {
    private final PendingChangeComparatorType sortType;

    public PendingChangeComparator(final PendingChangeComparatorType sortType) {
        Check.notNull(sortType, "sortType"); //$NON-NLS-1$
        this.sortType = sortType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final PendingChange first, final PendingChange second) {
        if (sortType == PendingChangeComparatorType.LOCAL_ITEM
            || sortType == PendingChangeComparatorType.LOCAL_ITEM_REVERSE) {
            /*
             * Local item can be null when a pending change doesn't have a local
             * item. We want these to be sorted at the end.
             */
            if (first.getLocalItem() == null && second.getLocalItem() == null) {
                return ServerPath.compareTopDown(first.getLocalItem(), second.getLocalItem());
            } else if (first.getLocalItem() == null) {
                return 1;
            } else if (second.getLocalItem() == null) {
                return -1;
            }

            if (sortType == PendingChangeComparatorType.LOCAL_ITEM) {
                return LocalPath.compareTopDown(first.getLocalItem(), second.getLocalItem());
            } else if (sortType == PendingChangeComparatorType.LOCAL_ITEM_REVERSE) {
                return LocalPath.compareBottomUp(first.getLocalItem(), second.getLocalItem());
            }

        } else if (sortType == PendingChangeComparatorType.SERVER_ITEM) {
            return ServerPath.compareTopDown(first.getServerItem(), second.getServerItem());
        }

        return 0;
    }
}
