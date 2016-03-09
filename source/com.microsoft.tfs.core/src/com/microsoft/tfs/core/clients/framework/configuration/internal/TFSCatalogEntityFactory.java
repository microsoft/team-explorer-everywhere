// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.internal;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.AnalysisDatabaseCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.InfrastructureRootCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.OrganizationalRootCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.ProcessGuidanceCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.ProjectCollectionCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.ProjectPortalCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.ReportingConfigurationCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.ReportingFolderCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.ReportingServerCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.SharePointWebApplicationCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.TFSCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.TFSUnknownCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.TeamFoundationServerCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.TeamProjectCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.WarehouseDatabaseCatalogEntity;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class TFSCatalogEntityFactory {
    private static final Log log = LogFactory.getLog(TFSCatalogEntityFactory.class);

    public static TFSCatalogEntity newEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        Check.notNull(session, "session"); //$NON-NLS-1$
        Check.notNull(catalogNode, "catalogNode"); //$NON-NLS-1$

        TFSCatalogEntity configurationObject;

        final GUID resourceTypeId = new GUID(catalogNode.getResource().getResourceTypeIdentifier());

        if (CatalogResourceTypes.ORGANIZATIONAL_ROOT.equals(resourceTypeId)) {
            configurationObject = new OrganizationalRootCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.INFRASTRUCTURE_ROOT.equals(resourceTypeId)) {
            configurationObject = new InfrastructureRootCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.REPORTING_CONFIGURATION.equals(resourceTypeId)) {
            configurationObject = new ReportingConfigurationCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.TEAM_FOUNDATION_SERVER_INSTANCE.equals(resourceTypeId)) {
            configurationObject = new TeamFoundationServerCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.PROJECT_COLLECTION.equals(resourceTypeId)) {
            configurationObject = new ProjectCollectionCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.TEAM_PROJECT.equals(resourceTypeId)) {
            configurationObject = new TeamProjectCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.PROCESS_GUIDANCE_SITE.equals(resourceTypeId)) {
            configurationObject = new ProcessGuidanceCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.PROJECT_PORTAL.equals(resourceTypeId)) {
            configurationObject = new ProjectPortalCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.REPORTING_FOLDER.equals(resourceTypeId)) {
            configurationObject = new ReportingFolderCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.REPORTING_SERVER.equals(resourceTypeId)) {
            configurationObject = new ReportingServerCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.ANALYSIS_DATABASE.equals(resourceTypeId)) {
            configurationObject = new AnalysisDatabaseCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.WAREHOUSE_DATABASE.equals(resourceTypeId)) {
            configurationObject = new WarehouseDatabaseCatalogEntity(session, catalogNode);
        } else if (CatalogResourceTypes.SHARE_POINT_WEB_APPLICATION.equals(resourceTypeId)) {
            configurationObject = new SharePointWebApplicationCatalogEntity(session, catalogNode);
        } else {
            log.debug(MessageFormat.format("Unknown configuration object type {0}", resourceTypeId)); //$NON-NLS-1$

            configurationObject = new TFSUnknownCatalogEntity(session, catalogNode);
        }

        return configurationObject;
    }
}
