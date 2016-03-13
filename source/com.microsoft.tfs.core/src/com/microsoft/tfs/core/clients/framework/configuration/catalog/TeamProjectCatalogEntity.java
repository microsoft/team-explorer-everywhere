// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProcessGuidanceEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectPortalEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingFolderEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;

/**
 * @since TEE-SDK-10.1
 */
public class TeamProjectCatalogEntity extends TFSCatalogEntity implements TeamProjectEntity {
    private final String SOURCE_CONTROL_CAPABILITY_FLAGS = "SourceControlCapabilityFlags"; //$NON-NLS-1$
    private final String SOURCE_CONTROL_GIT_ENABLED = "SourceControlGitEnabled"; //$NON-NLS-1$
    private final String SOURCE_CONTROL_TFVC_ENABLED = "SourceControlTfvcEnabled"; //$NON-NLS-1$
    private final String SOURCE_CONTROL_CAPABILITY_FLAG_TFVC_VALUE = "1"; //$NON-NLS-1$
    private final String SOURCE_CONTROL_CAPABILITY_FLAG_GIT_VALUE = "2"; //$NON-NLS-1$

    public TeamProjectCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    @Override
    public GUID getProjectID() {
        final String projectId = getProperty("ProjectId"); //$NON-NLS-1$

        if (projectId == null) {
            return null;
        }

        return new GUID(projectId);
    }

    @Override
    public String getProjectName() {
        return getProperty("ProjectName"); //$NON-NLS-1$
    }

    @Override
    public String getProjectURI() {
        return getProperty("ProjectUri"); //$NON-NLS-1$
    }

    public boolean isGitSupported() {
        final String gitEnabledProperty = getProperty(SOURCE_CONTROL_GIT_ENABLED);
        final String capabilityFlagsProperty = getProperty(SOURCE_CONTROL_CAPABILITY_FLAGS);

        if (!StringUtil.isNullOrEmpty(gitEnabledProperty)) {
            // Hybrid project with Git support
            return gitEnabledProperty.equalsIgnoreCase("true"); //$NON-NLS-1$
        } else if (!StringUtil.isNullOrEmpty(capabilityFlagsProperty)) {
            // Git or Tfs only project
            return capabilityFlagsProperty.equals(SOURCE_CONTROL_CAPABILITY_FLAG_GIT_VALUE);
        } else {
            // Pre-Git era
            return false;
        }
    }

    public boolean isTfvcSupported() {
        final String tfvcEnabledProperty = getProperty(SOURCE_CONTROL_TFVC_ENABLED);
        final String capabilityFlagsProperty = getProperty(SOURCE_CONTROL_CAPABILITY_FLAGS);

        if (!StringUtil.isNullOrEmpty(tfvcEnabledProperty)) {
            // Hybrid project with Tfvc support
            return tfvcEnabledProperty.equalsIgnoreCase("true"); //$NON-NLS-1$
        } else if (!StringUtil.isNullOrEmpty(capabilityFlagsProperty)) {
            // Git or Tfvc only project
            return capabilityFlagsProperty.equals(SOURCE_CONTROL_CAPABILITY_FLAG_TFVC_VALUE);
        } else {
            // Pre-Git era
            return true;
        }
    }

    @Deprecated
    public String getSourceControlCapabilityFlags() {
        return getProperty(SOURCE_CONTROL_CAPABILITY_FLAGS);
    }

    @Override
    public ProcessGuidanceEntity getProcessGuidance() {
        return getChildOfType(ProcessGuidanceEntity.class);
    }

    @Override
    public ProjectPortalEntity getProjectPortal() {
        return getChildOfType(ProjectPortalEntity.class);
    }

    @Override
    public ReportingFolderEntity getReportingFolder() {
        return getChildOfType(ReportingFolderEntity.class);
    }
}
