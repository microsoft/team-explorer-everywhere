// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSWebSiteEntity.TFSAbsoluteWebSiteEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.SharePointWebApplicationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;
import com.microsoft.tfs.core.clients.framework.location.AccessMapping;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.clients.framework.location.internal.AccessMappingMonikers;

/**
 * @since TEE-SDK-10.1
 */
public class SharePointWebApplicationCatalogEntity extends TFSCatalogEntity
    implements SharePointWebApplicationEntity, TFSAbsoluteWebSiteEntity {
    public SharePointWebApplicationCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    @Override
    public String getDefaultRelativePath() {
        return getProperty("DefaultRelativePath"); //$NON-NLS-1$
    }

    @Override
    public TFSEntity getReferencedResource() {
        return getSingletonDependency("ReferencedResource"); //$NON-NLS-1$
    }

    @Override
    public String getBaseURL() {
        return getRootURL();
    }

    public ServiceDefinition getAdminDefinition() {
        return getCatalogNode().getResource().getServiceReferences().get("AdminUrl"); //$NON-NLS-1$
    }

    @Override
    public String getAdminURL() {
        final ServiceDefinition adminService = getAdminDefinition();

        final AccessMapping accessMapping =
            ((TFSCatalogEntitySession) getSession()).getConnection().getLocationService().getAccessMapping(
                AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);

        return ((TFSCatalogEntitySession) getSession()).getConnection().getLocationService().locationForAccessMapping(
            adminService,
            accessMapping,
            false);
    }

    public ServiceDefinition getRootDefinition() {
        return getCatalogNode().getResource().getServiceReferences().get("RootUrl"); //$NON-NLS-1$
    }

    @Override
    public String getRootURL() {
        final ServiceDefinition rootService = getRootDefinition();

        final AccessMapping accessMapping =
            ((TFSCatalogEntitySession) getSession()).getConnection().getLocationService().getAccessMapping(
                AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);

        return ((TFSCatalogEntitySession) getSession()).getConnection().getLocationService().locationForAccessMapping(
            rootService,
            accessMapping,
            false);
    }
}
