// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public class TeamProjectCompatibilityEntityDataProvider extends TFSCompatibilityEntityDataProvider {
    private final ProjectInfo projectInfo;

    public TeamProjectCompatibilityEntityDataProvider(
        final TFSTeamProjectCollection connection,
        final ProjectInfo projectInfo) {
        super(connection);

        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$
        this.projectInfo = projectInfo;
    }

    @Override
    public GUID getResourceTypeID() {
        return CatalogResourceTypes.TEAM_PROJECT;
    }

    @Override
    public String getDisplayName() {
        return projectInfo.getName();
    }

    @Override
    public String getDescription() {
        return projectInfo.getName();
    }

    @Override
    public String getProperty(final String propertyName) {
        if (propertyName.equals("ProjectId")) //$NON-NLS-1$
        {
            return projectInfo.getGUID();
        }

        if (propertyName.equals("ProjectName")) //$NON-NLS-1$
        {
            return projectInfo.getName();
        }

        if (propertyName.equals("ProjectUri")) //$NON-NLS-1$
        {
            return projectInfo.getURI();
        }

        return null;
    }

    @Override
    public ServiceDefinition getServiceReference(final String serviceName) {
        return null;
    }
}
