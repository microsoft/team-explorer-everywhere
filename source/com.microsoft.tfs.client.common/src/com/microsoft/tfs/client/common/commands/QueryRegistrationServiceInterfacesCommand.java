// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.registration.ServiceInterface;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.reporting.ReportingClient;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Get the registration service interfaces for a particular toolId.
 */
public class QueryRegistrationServiceInterfacesCommand extends TFSConnectedCommand {
    public static final String SHAREPOINT = ToolNames.SHAREPOINT;
    public static final String VSTFS = ToolNames.CORE_SERVICES;
    public static final String REPORTS = ToolNames.WAREHOUSE;

    private final TFSTeamProjectCollection connection;
    private final String toolId;

    Properties properties = new Properties();

    public QueryRegistrationServiceInterfacesCommand(final TFSTeamProjectCollection connection, final String toolId) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(toolId, "toolId"); //$NON-NLS-1$

        this.connection = connection;
        this.toolId = toolId;

        setConnection(connection);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryRegistrationServiceInterfacesCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryRegistrationServiceInterfacesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryRegistrationServiceInterfacesCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        properties = new Properties();

        final ServiceInterface[] serviceInterfaces = connection.getRegistrationClient().getServiceInterfaces(toolId);

        if (serviceInterfaces != null) {
            for (int i = 0; i < serviceInterfaces.length; i++) {
                final String key = serviceInterfaces[i].getName();
                String value = serviceInterfaces[i].getURL();

                /* Fix-up common URL's passed for reporting and sharepoint. */
                if (REPORTS.equals(toolId) && ("BaseReportsUrl".equals(key) || "ReportsService".equals(key))) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    final ReportingClient reportingClient =
                        (ReportingClient) connection.getClient(ReportingClient.class);
                    value = reportingClient.getFixedURI(value);
                }

                properties.setProperty(key, value);
            }
        }

        return Status.OK_STATUS;
    }

    /**
     * @return Returns the service interface properties.
     */
    public Properties getProperties() {
        return properties;
    }
}