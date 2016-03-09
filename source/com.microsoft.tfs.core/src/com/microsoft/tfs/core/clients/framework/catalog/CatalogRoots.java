// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

/**
 * A convenience class providing static methods and constants for use when
 * dealing with nodes from a TFS catalog hierarchy.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogRoots {
    public static final String ORGANIZATIONAL_PATH = "3eYRYkJOok6GHrKam0AcAA=="; //$NON-NLS-1$
    public static final String INFRASTRUCTURE_PATH = "Vc1S6XwnTEe/isOiPfhmxw=="; //$NON-NLS-1$

    /**
     * Determines which tree the current node is in.
     *
     * @param path
     *        The path of the current node.
     *
     * @return The Catalog Tree that the node is in.
     */
    public static CatalogTree determineTree(final String path) {
        final String pathPrefix = path.substring(0, CatalogConstants.MANDATORY_NODE_PATH_LENGTH);

        if (pathPrefix.equals(CatalogRoots.ORGANIZATIONAL_PATH)) {
            return CatalogTree.ORGANIZATIONAL;
        }

        return CatalogTree.INFRASTRUCTURE;
    }

    /**
     * Determines the path for the specified tree.
     *
     * @param tree
     *        The tree.
     *
     * @return The path.
     */
    public static String determinePath(final CatalogTree tree) {
        if (tree.equals(CatalogTree.ORGANIZATIONAL)) {
            return CatalogRoots.ORGANIZATIONAL_PATH;
        }

        return CatalogRoots.INFRASTRUCTURE_PATH;
    }
}
