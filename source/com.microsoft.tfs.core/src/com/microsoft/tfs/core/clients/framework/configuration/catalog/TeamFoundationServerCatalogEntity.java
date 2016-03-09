// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamFoundationServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class TeamFoundationServerCatalogEntity extends TFSCatalogEntity implements TeamFoundationServerEntity {
    public TeamFoundationServerCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    @Override
    public ProjectCollectionEntity[] getProjectCollections() {
        return getChildrenOfType(ProjectCollectionEntity.class);
    }

    @Override
    public ProjectCollectionEntity getProjectCollection(final GUID instanceId) {
        Check.notNull(instanceId, "instanceId"); //$NON-NLS-1$

        final ProjectCollectionEntity[] collections = getProjectCollections();

        for (int i = 0; i < collections.length; i++) {
            if (instanceId.equals(collections[i].getInstanceID())) {
                return collections[i];
            }
        }

        return null;
    }
}
