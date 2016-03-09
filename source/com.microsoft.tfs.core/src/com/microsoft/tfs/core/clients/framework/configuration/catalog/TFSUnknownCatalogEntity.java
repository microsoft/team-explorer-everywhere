// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntityFactory;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;

/**
 * Exists to provide nodes that the configuration server provided us that we do
 * not entirely understand. (Ie, our {@link TFSCatalogEntityFactory} cannot
 * construct a more detailed node. These nodes must still exist in the tree,
 * however, for parentage.
 *
 * @since TEE-SDK-10.1
 */
public class TFSUnknownCatalogEntity extends TFSCatalogEntity {
    public TFSUnknownCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }
}
