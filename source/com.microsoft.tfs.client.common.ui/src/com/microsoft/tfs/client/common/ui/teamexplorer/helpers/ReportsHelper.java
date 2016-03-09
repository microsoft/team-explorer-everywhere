// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.core.clients.reporting.Report;
import com.microsoft.tfs.core.clients.reporting.ReportFolder;
import com.microsoft.tfs.core.clients.reporting.ReportNode;
import com.microsoft.tfs.core.clients.reporting.ReportUtils;
import com.microsoft.tfs.util.Check;

public class ReportsHelper {
    public static void openReport(final Shell shell, final String path) {
        openReport(shell, null, path);
    }

    public static void openReport(final Shell shell, final TFSServer server, final ReportNode reportNode) {
        if (reportNode instanceof Report) {
            final Report report = (Report) reportNode;
            final String reportViewerUrl = ReportUtils.getReportServiceURL(server.getConnection());
            final String path = ReportUtils.formatReportViewerPath(reportViewerUrl, report.getPath(), true);

            ReportsHelper.openReport(shell, report, path);
        } else if (reportNode instanceof ReportFolder) {
            final ReportFolder reportFolder = (ReportFolder) reportNode;
            final String reportManagerUrl = ReportUtils.getReportManagerURL(server.getConnection());
            final String path = ReportUtils.formatReportManagerPath(reportManagerUrl, reportFolder.getPath());

            ReportsHelper.openReport(shell, reportFolder, path);
        }
    }

    private static void openReport(final Shell shell, final ReportNode reportNode, final String path) {
        URI uri;

        try {
            uri = new URI(path);
        } catch (final IllegalArgumentException e) {
            final String title = Messages.getString("OpenReportAction.UrlErrorDialogTitle"); //$NON-NLS-1$
            final String message = Messages.getString("OpenReportAction.UrlErrorDialogText"); //$NON-NLS-1$
            final IStatus status = new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, null, e);

            ErrorDialog.openError(shell, title, message, status);
            return;
        } catch (final Exception e) {
            final String title = Messages.getString("OpenReportAction.ExplorerErrorDialogTitle"); //$NON-NLS-1$
            final String message = Messages.getString("OpenReportAction.ExplorerErrorDialogText"); //$NON-NLS-1$
            final IStatus status = new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, null, e);

            ErrorDialog.openError(shell, title, message, status);
            return;
        }

        Check.notNull(uri, "uri"); //$NON-NLS-1$

        String title = null;
        String tooltip = null;

        if (reportNode != null) {
            title = reportNode.getLabel();
            tooltip = reportNode.getDescription();
        }

        if (title == null || title.length() == 0) {
            title = uri.toString();
        }

        if (tooltip == null || tooltip.length() == 0) {
            tooltip = uri.toString();
        }

        // Always open external as Reports viewer has a big area at top for
        // paramaters and means that embedded version doesn't have enough real
        // estate to show report without scrolling. VS2005 used to display
        // inside the IDE. VS 2012 displays in an external browser.
        BrowserFacade.launchURL(uri, title, tooltip, uri.toString(), LaunchMode.EXTERNAL);
    }
}
