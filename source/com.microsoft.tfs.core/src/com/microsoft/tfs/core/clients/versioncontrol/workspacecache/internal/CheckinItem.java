// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

/**
 * CheckinItem is a simple class to hold the server path and item ID for each
 * item being checked in. We store itemId poastOrcas for backward compatibility
 * - the xml is shared across different clients. ItemID is ignored in the
 * current implementation and we use only ServerPath.
 *
 * @threadsafety unknown
 */
public class CheckinItem {
    private final int itemID;
    private final String serverItem;

    public CheckinItem(final String serverItem, final int itemID) {
        Check.notNull(serverItem, "serverItem"); //$NON-NLS-1$

        this.serverItem = serverItem;
        this.itemID = itemID;
    }

    /**
     * Creates a {@link Map} keyed on server path containing persistent
     * information for {@link PendingChange} objects
     *
     * @param pendingChanges
     *        the changes (may be <code>null</code> or empty)
     */
    public static Map<String, CheckinItem> fromPendingChanges(final PendingChange[] pendingChanges) {
        if (pendingChanges == null || pendingChanges.length == 0) {
            return newMap();
        }

        final Map<String, CheckinItem> checkinItems = newMap();

        for (final PendingChange change : pendingChanges) {
            checkinItems.put(change.getServerItem(), new CheckinItem(change.getServerItem(), change.getItemID()));
        }

        return checkinItems;
    }

    /**
     * Creates a Map keyed on server path containing persistent information for
     * {@link PendingChange} objects
     *
     * @param pendingChanges
     *        the changes (may be <code>null</code> or empty)
     */
    public static Map<String, CheckinItem> fromServerPaths(final Collection<String> serverPaths) {
        if (serverPaths == null || serverPaths.size() == 0) {
            return newMap();
        }

        final Map<String, CheckinItem> checkinItems = newMap();
        for (final String serverItem : serverPaths) {
            checkinItems.put(serverItem, new CheckinItem(serverItem, 0));
        }
        return checkinItems;
    }

    /**
     * Creates Dictionary indexed by ServerPath containing persistent
     * information
     *
     * @param sourceCheckinItems
     *        the source checkin items to copy (may be <code>null</code> or
     *        empty)
     */
    public static Map<String, CheckinItem> fromCheckinItems(final Map<String, CheckinItem> sourceCheckinItems) {
        if (sourceCheckinItems == null || sourceCheckinItems.size() == 0) {
            return newMap();
        }

        final Map<String, CheckinItem> checkinItems = newMap();
        for (final String serverItem : sourceCheckinItems.keySet()) {
            checkinItems.put(serverItem, sourceCheckinItems.get(serverItem));
        }
        return checkinItems;
    }

    private static TreeMap<String, CheckinItem> newMap() {
        return new TreeMap<String, CheckinItem>(ServerPath.TOP_DOWN_COMPARATOR);
    }

    public int getItemID() {
        return itemID;
    }

    public String getServerItem() {
        return serverItem;
    }
}
