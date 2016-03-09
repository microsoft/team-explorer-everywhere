// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingConfigurationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.SharePointWebApplicationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamFoundationServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;

/**
 * @since TEE-SDK-10.1
 */
public class OrganizationalRootCatalogEntity extends TFSCatalogEntity implements OrganizationalRootEntity {
    public OrganizationalRootCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    @Override
    public TeamFoundationServerEntity getTeamFoundationServer() {
        return getChildOfType(TeamFoundationServerEntity.class);
    }

    @Override
    public ReportingConfigurationEntity getReportingConfiguration() {
        return getChildOfType(ReportingConfigurationEntity.class);
    }

    @Override
    public ReportingServerEntity getReportingServer() {
        return getChildOfType(ReportingServerEntity.class);
    }

    @Override
    public SharePointWebApplicationEntity getSharePointWebApplication() {
        return getChildOfType(SharePointWebApplicationEntity.class);
    }
}
