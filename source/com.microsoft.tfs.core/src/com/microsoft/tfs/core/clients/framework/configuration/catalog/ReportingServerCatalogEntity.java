// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;
import com.microsoft.tfs.core.clients.framework.location.AccessMapping;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.clients.framework.location.internal.AccessMappingMonikers;

/**
 * @since TEE-SDK-10.1
 */
public class ReportingServerCatalogEntity extends TFSCatalogEntity implements ReportingServerEntity {
    public ReportingServerCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        super(session, catalogNode);
    }

    @Override
    public String getDefaultItemPath() {
        return getProperty("DefaultItemPath"); //$NON-NLS-1$
    }

    @Override
    public ServiceDefinition getReportManagerDefinition() {
        return getCatalogNode().getResource().getServiceReferences().get("ReportManagerUrl"); //$NON-NLS-1$
    }

    @Override
    public String getReportManagerURL() {
        final ServiceDefinition reportManagerService = getReportManagerDefinition();

        final AccessMapping accessMapping =
            ((TFSCatalogEntitySession) getSession()).getConnection().getLocationService().getAccessMapping(
                AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);

        return ((TFSCatalogEntitySession) getSession()).getConnection().getLocationService().locationForAccessMapping(
            reportManagerService,
            accessMapping,
            false);
    }

    @Override
    public ServiceDefinition getReportWebServiceDefinition() {
        return getCatalogNode().getResource().getServiceReferences().get("ReportWebServiceUrl"); //$NON-NLS-1$
    }

    @Override
    public String getReportWebServiceURL() {
        final ServiceDefinition reportingWebService = getReportWebServiceDefinition();

        final AccessMapping accessMapping =
            ((TFSCatalogEntitySession) getSession()).getConnection().getLocationService().getAccessMapping(
                AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);

        return ((TFSCatalogEntitySession) getSession()).getConnection().getLocationService().locationForAccessMapping(
            reportingWebService,
            accessMapping,
            false);
    }
}
