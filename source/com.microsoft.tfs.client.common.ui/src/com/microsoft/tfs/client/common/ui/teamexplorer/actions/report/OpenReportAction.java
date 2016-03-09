// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.report;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ReportsHelper;
import com.microsoft.tfs.core.clients.reporting.ReportNode;
import com.microsoft.tfs.util.Check;

public class OpenReportAction extends TeamExplorerBaseAction {
    @Override
    public void doRun(final IAction action) {
        final ReportNode reportNode = (ReportNode) getStructuredSelection().getFirstElement();
        Check.notNull(reportNode, "reportNode"); //$NON-NLS-1$

        ReportsHelper.openReport(getShell(), getContext().getServer(), reportNode);
    }
}
