// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.reporting.ReportUtils;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class ReportingServerCompatibilityEntity extends TFSCompatibilityEntity implements ReportingServerEntity {
    public ReportingServerCompatibilityEntity(final TFSCompatibilityEntity parent) {
        super(parent);
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.REPORTING_SERVER;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getDefaultItemPath() {
        return null;
    }

    @Override
    public ServiceDefinition getReportManagerDefinition() {
        /* TODO: backcompat service definitions a la Visual Studio */
        throw new NotSupportedException("Catalog backcompatibility does not support ServiceDefinitions"); //$NON-NLS-1$
    }

    @Override
    public String getReportManagerURL() {
        return getConnection().getRegistrationClient().getServiceInterfaceURL(
            ToolNames.WAREHOUSE,
            ServiceInterfaceNames.REPORTING_MANAGER_URL);
    }

    @Override
    public ServiceDefinition getReportWebServiceDefinition() {
        /* TODO: backcompat service definitions a la Visual Studio */
        throw new NotSupportedException("Catalog backcompatibility does not support ServiceDefinitions"); //$NON-NLS-1$
    }

    @Override
    public String getReportWebServiceURL() {
        String reportWebService = getConnection().getRegistrationClient().getServiceInterfaceURL(
            ToolNames.WAREHOUSE,
            ServiceInterfaceNames.REPORTING);

        if (reportWebService == null) {
            // Probably a post rosario server - look if server has reports
            // installer
            reportWebService = getConnection().getRegistrationClient().getServiceInterfaceURL(
                ToolNames.WAREHOUSE,
                ServiceInterfaceNames.REPORTING_WEB_SERVICE_URL);

            if (reportWebService == null || reportWebService.length() == 0) {
                // no reports web service installed on server
                return null;
            }
        }

        reportWebService = ReportUtils.removeKnownWebServerPaths(reportWebService);

        return reportWebService;
    }
}
