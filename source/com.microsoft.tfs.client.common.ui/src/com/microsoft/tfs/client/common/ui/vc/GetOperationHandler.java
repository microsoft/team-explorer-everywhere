// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;

/**
 * Warning: this is unused and only lightly tested.
 */
public class GetOperationHandler {
    public interface GetOperationVisitor {
        public Object visit(GetOperation operation, Object parent) throws CanceledException;
    }

    public static Object handleGetOperations(final GetOperation[] operations, final GetOperationVisitor visitor)
        throws CanceledException {
        /*
         * Implementation note: right now we make the assumption that we always
         * see parent items before we see any of their child items. If this is
         * not true, we would need to first sort the flat item set with a
         * comparator that enforced this.
         */

        Check.notNull(operations, "operations"); //$NON-NLS-1$
        Check.notNull(visitor, "visitor"); //$NON-NLS-1$

        if (operations.length == 0) {
            return null;
        }

        final Map map = new HashMap();
        String rootPath = null;

        for (int i = 0; i < operations.length; i++) {
            /*
             * ServerPath.getDirectoryPart() returns a path with no trailing
             * separator character.
             */
            final String parentPath = ServerPath.getParent(operations[i].getTargetServerItem());

            final Object parent = map.get(parentPath);

            final Object node = visitor.visit(operations[i], parent);

            if (node == null) {
                continue;
            }

            /*
             * Server paths come back from TFS with no trailing separator
             * character.
             */
            final String serverItem = operations[i].getTargetServerItem();
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
