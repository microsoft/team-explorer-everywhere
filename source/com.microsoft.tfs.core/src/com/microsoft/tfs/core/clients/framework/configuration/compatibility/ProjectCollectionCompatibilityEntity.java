// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingConfigurationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingFolderEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class ProjectCollectionCompatibilityEntity extends TFSCompatibilityEntity implements ProjectCollectionEntity {
    private static final Log log = LogFactory.getLog(ProjectCollectionCompatibilityEntity.class);

    private final Object lock = new Object();
    private TeamProjectCompatibilityEntity[] teamProjects;
    private ReportingFolderEntity reportingFolder;

    public ProjectCollectionCompatibilityEntity(final TFSCompatibilityEntity parent) {
        super(parent);
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.PROJECT_COLLECTION;
    }

    @Override
    public String getDisplayName() {
        return getConnection().getName();
    }

    @Override
    public GUID getInstanceID() {
        return GUID.EMPTY;
    }

    @Override
    public TeamProjectEntity[] getTeamProjects() {
        synchronized (lock) {
            if (teamProjects == null) {
                final CommonStructureClient cssClient =
                    (CommonStructureClient) getConnection().getClient(CommonStructureClient.class);

                final ProjectInfo[] projects = cssClient.listProjects();

                teamProjects = new TeamProjectCompatibilityEntity[projects.length];

                for (int i = 0; i < projects.length; i++) {
                    teamProjects[i] = new TeamProjectCompatibilityEntity(this, projects[i]);
                }
            }

            return teamProjects;
        }
    }

    @Override
    public TeamProjectEntity getTeamProject(final GUID projectId) {
        Check.notNull(projectId, "projectId"); //$NON-NLS-1$

        final TeamProjectEntity[] projects = getTeamProjects();

        for (int i = 0; i < projects.length; i++) {
            if (projectId.equals(projects[i].getProjectID())) {
                return projects[i];
            }
        }

        return null;
    }

    @Override
    public ReportingConfigurationEntity getReportingConfiguration() {
        /* Get the TeamFoundationServerEntity */
        TFSEntity ancestor = getParent();

        if (ancestor != null) {
            /* Get the OrganizationalRootEntity */
            ancestor = ancestor.getParent();

            if (ancestor != null && ancestor instanceof OrganizationalRootEntity) {
                return ((OrganizationalRootEntity) ancestor).getReportingConfiguration();
            }
        }

        log.warn("Compatibility configuration hierarchy unbalanced"); //$NON-NLS-1$
        return null;
    }

    @Override
    public ReportingFolderEntity getReportingFolder() {
        synchronized (lock) {
            if (reportingFolder == null) {
                reportingFolder = new ReportingFolderCompatibilityEntity(this, ""); //$NON-NLS-1$
            }

            return reportingFolder;
        }
    }
}
