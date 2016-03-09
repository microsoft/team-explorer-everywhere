// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSWebSiteEntity.TFSAbsoluteWebSiteEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.SharePointWebApplicationEntity;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.sharepoint.internal.WSSConstants;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class SharePointWebApplicationCompatibilityEntity extends TFSCompatibilityEntity
    implements SharePointWebApplicationEntity, TFSAbsoluteWebSiteEntity {
    protected SharePointWebApplicationCompatibilityEntity(final TFSCompatibilityEntity parent) {
        super(parent);
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.SHARE_POINT_WEB_APPLICATION;
    }

    @Override
    public String getDisplayName() {
        return "WSS"; //$NON-NLS-1$
    }

    @Override
    public TFSEntity getReferencedResource() {
        return null;
    }

    @Override
    public String getAdminURL() {
        return null;
    }

    @Override
    public String getDefaultRelativePath() {
        return null;
    }

    @Override
    public String getRootURL() {
        return getConnection().getRegistrationClient().getServiceInterfaceURL(
            ToolNames.SHAREPOINT,
            WSSConstants.BASE_SITE_URL_SERVICE_NAME);
    }

    @Override
    public String getBaseURL() {
        return getRootURL();
    }
}
