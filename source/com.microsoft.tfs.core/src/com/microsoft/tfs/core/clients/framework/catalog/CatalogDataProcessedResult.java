// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

/**
 * Container to hold the node and resource results from the post processing
 * performed following a catalog web service call that returns
 * {@link CatalogData}.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class CatalogDataProcessedResult {
    private final CatalogResource[] matchingResources;
    private final CatalogNode[] matchingNodes;

    /**
     * Constructor.
     */
    public CatalogDataProcessedResult(final CatalogResource[] resources, final CatalogNode[] nodes) {
        matchingResources = resources;
        matchingNodes = nodes;
    }

    /**
     * Returns the matching catalog resources.
     */
    public CatalogResource[] getMatchingResources() {
        return matchingResources;
    }

    /**
     * Returns the matching catalog nodes.
     */
    public CatalogNode[] getMatchingNodes() {
        return matchingNodes;
    }
}
