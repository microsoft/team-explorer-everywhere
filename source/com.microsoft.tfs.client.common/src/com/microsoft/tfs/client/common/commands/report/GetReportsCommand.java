// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.report;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.reporting.ReportNode;
import com.microsoft.tfs.core.clients.reporting.ReportingClient;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetReportsCommand extends TFSConnectedCommand {
    private final TFSServer server;
    private final ProjectInfo projectInfo;
    private final boolean refresh;

    private ReportNode[] reports = new ReportNode[0];

    public GetReportsCommand(final TFSServer server, final ProjectInfo projectInfo, final boolean refresh) {
        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$

        this.server = server;
        this.projectInfo = projectInfo;
        this.refresh = refresh;

        setConnection(server.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("GetReportsCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, projectInfo);
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("GetReportsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("GetReportsCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, projectInfo);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final ReportingClient reportClient = (ReportingClient) server.getConnection().getClient(ReportingClient.class);

        final List<ReportNode> reportList = reportClient.getReports(projectInfo, refresh);
        reports = reportList.toArray(new ReportNode[reportList.size()]);

        return Status.OK_STATUS;
    }

    public ReportNode[] getReports() {
        return reports;
    }
}
