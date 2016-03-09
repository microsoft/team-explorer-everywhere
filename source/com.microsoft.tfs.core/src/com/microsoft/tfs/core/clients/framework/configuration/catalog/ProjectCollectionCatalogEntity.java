// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingConfigurationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingFolderEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class ProjectCollectionCatalogEntity extends TFSCatalogEntity implements ProjectCollectionEntity {
    public ProjectCollectionCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    /**
     * @return the project collection instance id (GUID).
     */
    @Override
    public GUID getInstanceID() {
        final String instanceId = getProperty("InstanceId"); //$NON-NLS-1$

        if (instanceId == null) {
            return null;
        }

        return new GUID(instanceId);
    }

    @Override
    public TeamProjectEntity[] getTeamProjects() {
        return getChildrenOfType(TeamProjectEntity.class);
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

    /**
     * @return the reporting configuration for this Project Collection.
     */
    @Override
    public ReportingConfigurationEntity getReportingConfiguration() {
        /*
         * Note: reporting configuration is configured on a per-server basis.
         */
        final OrganizationalRootEntity organizationalRoot = getAncestorOfType(OrganizationalRootEntity.class);

        if (organizationalRoot == null) {
            return null;
        }

        return organizationalRoot.getReportingConfiguration();
    }

    @Override
    public ReportingFolderEntity getReportingFolder() {
        return getChildOfType(ReportingFolderEntity.class);
    }
}
