// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSWebSiteEntity.TFSRelativeWebSiteEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProcessGuidanceEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSWebSiteEntityUtils;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.sharepoint.internal.WSSConstants;
import com.microsoft.tfs.core.pguidance.ProcessGuidanceConstants;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class ProcessGuidanceCompatibilityEntity extends TFSCompatibilityEntity
    implements ProcessGuidanceEntity, TFSRelativeWebSiteEntity {
    protected ProcessGuidanceCompatibilityEntity(final TFSCompatibilityEntity parent) {
        super(parent);
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.PROCESS_GUIDANCE_SITE;
    }

    @Override
    public String getDisplayName() {
        return "Process Guidance"; //$NON-NLS-1$
    }

    @Override
    public String getResourceSubType() {
        return "WssDocumentLibrary"; //$NON-NLS-1$
    }

    @Override
    public String getASCIIName() {
        return ProcessGuidanceConstants.FOLDER;
    }

    @Override
    public String getRelativePath() {
        final TeamProjectEntity teamProject = getAncestorOfType(TeamProjectEntity.class);

        if (teamProject == null) {
            return null;
        }

        return URIUtils.combinePaths(getConnection().getRegistrationClient().getServiceInterfaceURL(
            ToolNames.SHAREPOINT,
            WSSConstants.BASE_SITE_URL_SERVICE_NAME), teamProject.getProjectName());
    }

    @Override
    public TFSEntity getReferencedResource() {
        final OrganizationalRootEntity organizationalRoot = getAncestorOfType(OrganizationalRootEntity.class);

        if (organizationalRoot != null) {
            return organizationalRoot.getSharePointWebApplication();
        }

        return null;
    }

    @Override
    public String getFullyQualifiedURL() {
        return TFSWebSiteEntityUtils.getFullItemPath(this);
    }
}
