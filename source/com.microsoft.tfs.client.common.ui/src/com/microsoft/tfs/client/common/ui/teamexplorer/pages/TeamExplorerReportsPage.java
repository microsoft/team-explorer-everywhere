// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ReportsHelper;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.reporting.ReportUtils;
import com.microsoft.tfs.util.GUID;

public class TeamExplorerReportsPage extends TeamExplorerBasePage {
    @Override
    public Composite getPageContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 1, true, 0, 5);

        final Hyperlink link =
            toolkit.createHyperlink(
                composite,
                Messages.getString("TeamExplorerReportsSection.GoToReportsSite"), //$NON-NLS-1$
                SWT.WRAP);

        link.setUnderlined(false);
        link.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                final TFSTeamProjectCollection connection = context.getServer().getConnection();
                final GUID projectGUID = new GUID(context.getCurrentProjectInfo().getGUID());

                final String folderPath = ReportUtils.getProjectReportFolder(connection, projectGUID);
                final String reportManagerUrl = ReportUtils.getReportManagerURL(context.getServer().getConnection());
                final String path = ReportUtils.formatReportManagerPath(reportManagerUrl, folderPath);

                ReportsHelper.openReport(composite.getShell(), path);
            }
        });

        return composite;
    }
}
