// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Comparator;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

/**
 * A comparer used to compare conflicts.
 *
 *
 * @threadsafety unknown
 */
public class ConflictComparator implements Comparator<Conflict> {
    /**
     * Compares two conflicts by server path for display purposes (culture
     * sensitive and case insensitive).
     */
    @Override
    public int compare(final Conflict c1, final Conflict c2) {
        // Compare based on YourServerItem.
        if (c1.getYourServerItem() != null && c2.getYourServerItem() != null) {
            final int result = ServerPath.compareTopDown(c1.getYourServerItem(), c2.getYourServerItem());
            if (result != 0) {
                return result;
            }
        }

        // Next compare TheirServerItem.
        if (c1.getTheirServerItem() != null && c2.getTheirServerItem() != null) {
            final int result = ServerPath.compareTopDown(c1.getTheirServerItem(), c2.getTheirServerItem());
            if (result != 0) {
                return result;
            }
        }

        // Fallback to sorting by conflict ID to force an ordering.
        if (c1.getConflictID() > c2.getConflictID()) {
            return 1;
        } else if (c1.getConflictID() < c2.getConflictID()) {
            return -1;
        }

        return 0;
    }
}
