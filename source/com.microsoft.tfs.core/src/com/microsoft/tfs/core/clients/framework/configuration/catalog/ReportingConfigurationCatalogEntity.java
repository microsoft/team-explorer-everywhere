// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingConfigurationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;

/**
 * @since TEE-SDK-10.1
 */
public class ReportingConfigurationCatalogEntity extends TFSCatalogEntity implements ReportingConfigurationEntity {
    public ReportingConfigurationCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    @Override
    public ReportingServerEntity getReportingServer() {
        final TFSEntity reportingServer = getSingletonDependency("ReportServer"); //$NON-NLS-1$

        if (reportingServer != null && reportingServer instanceof ReportingServerEntity) {
            return (ReportingServerEntity) reportingServer;
        }

        return null;
    }
}
