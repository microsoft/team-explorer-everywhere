// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks;

import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.webaccessintegration.editors.WebAccessBuildReportEditor;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.linking.LinkingClient;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;

public class ViewBuildReportTask extends BaseTask {
    private final IBuildServer buildServer;
    private final String buildUri;
    private final String buildNumber;
    private final LaunchMode launchMode;

    public static final CodeMarker CODEMARKER_REPORT_OPENED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.teambuild.tasks.ViewBuildReportTask#opened"); //$NON-NLS-1$

    public ViewBuildReportTask(
        final Shell shell,
        final IBuildServer buildServer,
        final String buildUri,
        final String buildNumber) {
        this(shell, buildServer, buildUri, buildNumber, LaunchMode.USER_PREFERENCE);
    }

    public ViewBuildReportTask(
        final Shell shell,
        final IBuildServer buildServer,
        final String buildUri,
        final String buildNumber,
        final LaunchMode launchMode) {
        super(shell);

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNull(buildUri, "buildUri"); //$NON-NLS-1$
        Check.notNull(buildNumber, "buildNumber"); //$NON-NLS-1$

        this.buildServer = buildServer;
        this.buildUri = buildUri;
        this.buildNumber = buildNumber;
        this.launchMode = launchMode;
    }

    @Override
    public IStatus run() {
        URI uri = null;

        final boolean displayInEmbeddedBrowser =
            launchMode != LaunchMode.EXTERNAL && WebAccessBuildReportEditor.isSupported(buildServer.getConnection());

        try {
            final TSWAHyperlinkBuilder builder =
                new TSWAHyperlinkBuilder(buildServer.getConnection(), displayInEmbeddedBrowser);

            final String build;
            final int index = buildUri.indexOf('?');

            if (index != -1) {
                build = buildUri.substring(0, index);
            } else {
                build = buildUri;
            }

            uri = builder.getViewBuildDetailsURL(build);
        } catch (final Exception e) {
            /*
             * Ignore exception raised by doing it the new way - fall back to
             * old mechanism.
             */
            uri = null;
        }

        if (uri == null) {
            final LinkingClient linkingClient =
                (LinkingClient) buildServer.getConnection().getClient(LinkingClient.class);

            try {
                uri = URIUtils.newURI(linkingClient.getArtifactURLExternal(buildUri));
            } catch (final IllegalArgumentException e) {
                MessageDialog.openError(
                    getShell(),
                    Messages.getString("ViewBuildReportTask.InvalidUrlTitle"), //$NON-NLS-1$
                    Messages.getString("ViewBuildReportTask.InvalidUrlMessage")); //$NON-NLS-1$

                return new Status(
                    IStatus.ERROR,
                    TFSCommonUIClientPlugin.PLUGIN_ID,
                    0,
                    Messages.getString("ViewBuildReportTask.InvalidUrlMessage"), //$NON-NLS-1$
                    e);
            }
        }

        if (displayInEmbeddedBrowser) {
            final ServerManager manager = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager();
            WebAccessBuildReportEditor.openEditor(manager.getDefaultServer(), uri.toString(), buildNumber);
        } else {
            BrowserFacade.launchURL(uri, buildNumber, buildNumber, uri.toString(), launchMode);
        }

        CodeMarkerDispatch.dispatch(ViewBuildReportTask.CODEMARKER_REPORT_OPENED);

        return Status.OK_STATUS;
    }
}
