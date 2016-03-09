// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;

public class ItemSetHandler {
    public interface ItemSetVisitor {
        public Object visit(Item item, Object parent) throws CanceledException;
    }

    public static Object handleItemSet(final ItemSet itemSet, final ItemSetVisitor visitor) throws CanceledException {
        /*
         * Implementation note: right now we make the assumption that we always
         * see parent items before we see any of their child items. If this is
         * not true, we would need to first sort the flat item set with a
         * comparator that enforced this.
         */

        Check.notNull(itemSet, "itemSet"); //$NON-NLS-1$
        Check.notNull(visitor, "visitor"); //$NON-NLS-1$

        final Item[] items = itemSet.getItems();

        if (items.length == 0) {
            return null;
        }

        final Map map = new HashMap();
        String rootPath = null;

        for (int i = 0; i < items.length; i++) {
            /*
             * ServerPath.getDirectoryPart() returns a path with no trailing
             * separator character.
             */
            final String parentPath = ServerPath.getParent(items[i].getServerItem());

            final Object parent = map.get(parentPath);

            final Object node = visitor.visit(items[i], parent);

            if (node == null) {
                continue;
            }

            /*
             * Server paths come back from TFS with no trailing separator
             * character.
             */
            final String serverItem = items[i].getServerItem();
            map.put(serverItem, node);

            /*
             * We're assuming that the root path is the first item we see.
             */
            if (rootPath == null) {
                rootPath = serverItem;
            }
        }

        return map.get(rootPath);
    }
}
