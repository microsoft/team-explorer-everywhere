// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSWebSiteEntity.TFSRelativeWebSiteEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectPortalEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSWebSiteEntityUtils;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class ProjectPortalCatalogEntity extends TFSCatalogEntity
    implements ProjectPortalEntity, TFSRelativeWebSiteEntity {
    public ProjectPortalCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    @Override
    public String getResourceSubType() {
        return getProperty("ResourceSubType"); //$NON-NLS-1$
    }

    @Override
    public GUID getOwnedWebIdentifier() {
        final String identifier = getProperty("OwnedWebIdentifier"); //$NON-NLS-1$

        if (identifier == null) {
            return null;
        }

        return new GUID(identifier);
    }

    @Override
    public String getRelativePath() {
        return getProperty("RelativePath"); //$NON-NLS-1$
    }

    @Override
    public TFSEntity getReferencedResource() {
        return getSingletonDependency("ReferencedResource"); //$NON-NLS-1$
    }

    @Override
    public String getFullItemPath() {
        /*
         * If this is a sharepoint site, unravel the path by looking at our
         * parentage.
         */
        if ("WssSite".equals(getResourceSubType())) //$NON-NLS-1$
        {
            return TFSWebSiteEntityUtils.getFullItemPath(this);
        }
        /*
         * If this is an external web site, simply return the URL contained in
         * the node resource data.
         */
        else if ("WebSite".equals(getResourceSubType())) //$NON-NLS-1$
        {
            return getProperty("FullyQualifiedUrl"); //$NON-NLS-1$
        }

        /* Should not happen. */
        return null;
    }
}
