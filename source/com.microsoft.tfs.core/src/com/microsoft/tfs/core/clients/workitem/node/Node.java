// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.node;

import com.microsoft.tfs.util.GUID;

/**
 * Describes a Node object that is used in the work item tracking data
 * structures.
 *
 * @since TEE-SDK-10.1
 */
public interface Node extends Comparable<Node> {
    public int getID();

    public GUID getGUID();

    public String getName();

    public NodeCollection getChildNodes();

    public String getURI();

    public Node getParent();

    public String getPath();

    public static class TreeType {
        public static final TreeType AREA = new TreeType("AREA"); //$NON-NLS-1$
        public static final TreeType ITERATION = new TreeType("ITERATION"); //$NON-NLS-1$

        private final String type;

        private TreeType(final String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }
}
