// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSWebSiteEntity.TFSRelativeWebSiteEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectPortalEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSWebSiteEntityUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class ProjectPortalCompatibilityEntity extends TFSCompatibilityEntity
    implements ProjectPortalEntity, TFSRelativeWebSiteEntity {
    private final String projectName;

    protected ProjectPortalCompatibilityEntity(final TFSCompatibilityEntity parent, final String projectName) {
        super(parent);

        Check.notNull(projectName, "projectName"); //$NON-NLS-1$
        this.projectName = projectName;
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.PROJECT_PORTAL;
    }

    @Override
    public String getDisplayName() {
        return "Project Portal"; //$NON-NLS-1$
    }

    @Override
    public String getResourceSubType() {
        return "WssSite"; //$NON-NLS-1$
    }

    @Override
    public GUID getOwnedWebIdentifier() {
        return null;
    }

    @Override
    public TFSEntity getReferencedResource() {
        final OrganizationalRootEntity organizationalRoot = getAncestorOfType(OrganizationalRootEntity.class);

        if (organizationalRoot != null) {
            return organizationalRoot.getSharePointWebApplication();
        }

        return null;
    }

    @Override
    public String getRelativePath() {
        return projectName;
    }

    @Override
    public String getFullItemPath() {
        return TFSWebSiteEntityUtils.getFullItemPath(this);
    }
}
