// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamFoundationServerEntity;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class TeamFoundationServerCompatibilityEntity extends TFSCompatibilityEntity
    implements TeamFoundationServerEntity {
    private final Object lock = new Object();
    private ProjectCollectionCompatibilityEntity projectCollection;

    public TeamFoundationServerCompatibilityEntity(final TFSCompatibilityEntity parent) {
        super(parent);
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.TEAM_FOUNDATION_SERVER_INSTANCE;
    }

    @Override
    public String getDisplayName() {
        return getConnection().getName();
    }

    @Override
    public ProjectCollectionEntity[] getProjectCollections() {
        return new ProjectCollectionEntity[] {
            getProjectCollection(getConnection().getInstanceID())
        };
    }

    @Override
    public ProjectCollectionEntity getProjectCollection(final GUID instanceId) {
        if (!getConnection().getInstanceID().equals(instanceId)) {
            return null;
        }

        synchronized (lock) {
            if (projectCollection == null) {
                projectCollection = new ProjectCollectionCompatibilityEntity(this);
            }

            return projectCollection;
        }
    }
}
