// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingConfigurationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.SharePointWebApplicationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamFoundationServerEntity;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class OrganizationalRootCompatibilityEntity extends TFSCompatibilityEntity implements OrganizationalRootEntity {
    private final Object lock = new Object();
    private TeamFoundationServerCompatibilityEntity teamFoundationServer;
    private ReportingConfigurationCompatibilityEntity reportingConfiguration;
    private ReportingServerCompatibilityEntity reportingServer;
    private SharePointWebApplicationEntity sharePointWebApplication;

    public OrganizationalRootCompatibilityEntity(final TFSTeamProjectCollection connection) {
        super(connection);
    }

    @Override
    public GUID getResourceID() {
        return CatalogResourceTypes.ORGANIZATIONAL_ROOT;
    }

    @Override
    public String getDisplayName() {
        return "Organizational Root"; //$NON-NLS-1$
    }

    @Override
    public String getDescription() {
        return ""; //$NON-NLS-1$
    }

    public String getProperty(final String propertyName) {
        return null;
    }

    public ServiceDefinition getServiceReference(final String serviceName) {
        return null;
    }

    @Override
    public TeamFoundationServerEntity getTeamFoundationServer() {
        synchronized (lock) {
            if (teamFoundationServer == null) {
                teamFoundationServer = new TeamFoundationServerCompatibilityEntity(this);
            }

            return teamFoundationServer;
        }
    }

    @Override
    public ReportingConfigurationEntity getReportingConfiguration() {
        synchronized (lock) {
            if (reportingConfiguration == null) {
                reportingConfiguration = new ReportingConfigurationCompatibilityEntity(this);
            }

            return reportingConfiguration;
        }
    }

    @Override
    public ReportingServerEntity getReportingServer() {
        synchronized (lock) {
            if (reportingServer == null) {
                reportingServer = new ReportingServerCompatibilityEntity(this);
            }

            return reportingServer;
        }
    }

    @Override
    public SharePointWebApplicationEntity getSharePointWebApplication() {
        synchronized (lock) {
            if (sharePointWebApplication == null) {
                sharePointWebApplication = new SharePointWebApplicationCompatibilityEntity(this);
            }

            return sharePointWebApplication;
        }
    }
}
