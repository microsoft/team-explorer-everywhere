// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProcessGuidanceEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectPortalEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingFolderEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class TeamProjectCompatibilityEntity extends TFSCompatibilityEntity implements TeamProjectEntity {
    private final ProjectInfo projectInfo;

    private final Object lock = new Object();
    private ProjectPortalEntity projectPortal;
    private ReportingFolderEntity reportingFolder;

    public TeamProjectCompatibilityEntity(final TFSCompatibilityEntity parent, final ProjectInfo projectInfo) {
        super(parent);

        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$
        this.projectInfo = projectInfo;
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.TEAM_PROJECT;
    }

    @Override
    public String getDisplayName() {
        return getProjectName();
    }

    @Override
    public GUID getProjectID() {
        return new GUID(projectInfo.getGUID());
    }

    @Override
    public String getProjectName() {
        return projectInfo.getName();
    }

    @Override
    public String getProjectURI() {
        return projectInfo.getURI();
    }

    @Override
    public ProcessGuidanceEntity getProcessGuidance() {
        return null;
    }

    @Override
    public ProjectPortalEntity getProjectPortal() {
        synchronized (lock) {
            if (projectPortal == null) {
                projectPortal = new ProjectPortalCompatibilityEntity(this, projectInfo.getName());
            }

            return projectPortal;
        }
    }

    @Override
    public ReportingFolderEntity getReportingFolder() {
        synchronized (lock) {
            if (reportingFolder == null) {
                reportingFolder = new ReportingFolderCompatibilityEntity(this, projectInfo.getName());
            }

            return reportingFolder;
        }
    }
}
