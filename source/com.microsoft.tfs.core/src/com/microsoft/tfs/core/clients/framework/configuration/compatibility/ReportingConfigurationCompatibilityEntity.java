// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingConfigurationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class ReportingConfigurationCompatibilityEntity extends TFSCompatibilityEntity
    implements ReportingConfigurationEntity {
    private final Object lock = new Object();
    private ReportingServerCompatibilityEntity reportingServer;

    public ReportingConfigurationCompatibilityEntity(final TFSCompatibilityEntity parent) {
        super(parent);
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.REPORTING_CONFIGURATION;
    }

    @Override
    public String getDisplayName() {
        return "Report Configuration"; //$NON-NLS-1$
    }

    @Override
    public ReportingServerEntity getReportingServer() {
        synchronized (lock) {
            if (reportingServer == null) {
                reportingServer = new ReportingServerCompatibilityEntity(this);
            }
        }

        return reportingServer;
    }
}
