// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.AnalysisDatabaseEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingFolderEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.WarehouseDatabaseEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.ReportingFolderEntityUtils;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;

/**
 * @since TEE-SDK-10.1
 */
public class ReportingFolderCatalogEntity extends TFSCatalogEntity implements ReportingFolderEntity {
    public ReportingFolderCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    @Override
    public ReportingServerEntity getReportingServer() {
        return getDependencyOfType(ReportingServerEntity.class);
    }

    @Override
    public AnalysisDatabaseEntity getAnalysisDatabase() {
        return getDependencyOfType(AnalysisDatabaseEntity.class);
    }

    @Override
    public WarehouseDatabaseEntity getWarehouseDatabase() {
        return getDependencyOfType(WarehouseDatabaseEntity.class);
    }

    @Override
    public String getItemPath() {
        return getProperty("ItemPath"); //$NON-NLS-1$
    }

    @Override
    public TFSEntity getReferencedResource() {
        return getSingletonDependency("ReferencedResource"); //$NON-NLS-1$
    }

    @Override
    public String getFullItemPath() {
        return ReportingFolderEntityUtils.getFullItemPath(this);
    }
}
