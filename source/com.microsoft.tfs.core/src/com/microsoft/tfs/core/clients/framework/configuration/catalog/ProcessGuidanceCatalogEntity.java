// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSWebSiteEntity.TFSRelativeWebSiteEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProcessGuidanceEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSWebSiteEntityUtils;

/**
 * @since TEE-SDK-10.1
 */
public class ProcessGuidanceCatalogEntity extends TFSCatalogEntity
    implements ProcessGuidanceEntity, TFSRelativeWebSiteEntity {
    public ProcessGuidanceCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    @Override
    public String getResourceSubType() {
        return getProperty("ResourceSubType"); //$NON-NLS-1$
    }

    @Override
    public String getASCIIName() {
        return getProperty("AsciiName"); //$NON-NLS-1$
    }

    @Override
    public String getRelativePath() {
        return getProperty("RelativePath"); //$NON-NLS-1$
    }

    @Override
    public TFSEntity getReferencedResource() {
        return getSingletonDependency("ReferencedResource"); //$NON-NLS-1$
    }

    @Override
    public String getFullyQualifiedURL() {
        /* Resolve WSS-based URLs using the configuration tree */
        if ("WssDocumentLibrary".equals(getResourceSubType())) //$NON-NLS-1$
        {
            return TFSWebSiteEntityUtils.getFullItemPath(this);
        }
        /* Explicitly fully-qualified URL */
        else if ("WebSite".equals(getResourceSubType())) //$NON-NLS-1$
        {
            return getProperty("FullyQualifiedUrl"); //$NON-NLS-1$
        } else {
            return null;
        }
    }
}
