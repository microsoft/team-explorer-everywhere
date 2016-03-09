// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.entities;

import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public interface ProjectCollectionEntity extends TFSEntity {
    /**
     * @return the project collection instance id (GUID).
     */
    public GUID getInstanceID();

    public TeamProjectEntity[] getTeamProjects();

    public TeamProjectEntity getTeamProject(GUID projectId);

    /**
     * @return the reporting configuration for this Project Collection.
     */
    public ReportingConfigurationEntity getReportingConfiguration();

    public ReportingFolderEntity getReportingFolder();
}
