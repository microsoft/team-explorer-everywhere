// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.AnalysisDatabaseEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingFolderEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamFoundationServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.WarehouseDatabaseEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.ReportingFolderEntityUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class ReportingFolderCompatibilityEntity extends TFSCompatibilityEntity implements ReportingFolderEntity {
    private static final Log log = LogFactory.getLog(ReportingFolderCompatibilityEntity.class);

    private final String itemPath;

    public ReportingFolderCompatibilityEntity(final TFSCompatibilityEntity parent, final String itemPath) {
        super(parent);

        Check.notNull(itemPath, "itemPath"); //$NON-NLS-1$
        this.itemPath = itemPath;
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.REPORTING_FOLDER;
    }

    @Override
    public String getDisplayName() {
        return "Reporting Folder"; //$NON-NLS-1$
    }

    @Override
    public String getItemPath() {
        return itemPath;
    }

    @Override
    public String getFullItemPath() {
        return ReportingFolderEntityUtils.getFullItemPath(this);
    }

    @Override
    public TFSEntity getReferencedResource() {
        return null;
    }

    @Override
    public WarehouseDatabaseEntity getWarehouseDatabase() {
        return null;
    }

    @Override
    public AnalysisDatabaseEntity getAnalysisDatabase() {
        return null;
    }

    @Override
    public ReportingServerEntity getReportingServer() {
        TFSEntity ancestor = getParent();

        /* We may be parented by a team project, find the project collection */
        if (ancestor instanceof TeamProjectEntity) {
            ancestor = ancestor.getParent();
        }

        /* Walk up the ancestry tree to the organizational root */
        if (ancestor instanceof ProjectCollectionEntity) {
            ancestor = ancestor.getParent();

            if (ancestor instanceof TeamFoundationServerEntity) {
                ancestor = ancestor.getParent();

                if (ancestor instanceof OrganizationalRootEntity) {
                    return ((OrganizationalRootEntity) ancestor).getReportingServer();
                }
            }
        }

        log.warn("Inconsistent parentage in ReportingFolderCompatibilityEntity"); //$NON-NLS-1$
        return null;
    }
}
