// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorerEditorInput;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;
import com.microsoft.tfs.core.clients.workitem.files.DownloadException;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

public class TeamBuildHelper {
    public static final CodeMarker CODEMARKER_OPEN_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.teambuild.actions.QueueBuildAction#openComplete"); //$NON-NLS-1$

    private static final int DOWNLOAD_BUFFER_SIZE = 2048;

    /**
     * Format a text string for a controller which shows the controller name and
     * status if appropriate.
     *
     * @param buildServer
     *        The build server.
     * @param controller
     *        The build controller.
     * @return A formatted string with the controller name and status if
     *         controller is not available.
     */
    public static String getControllerDisplayString(final IBuildServer buildServer, final IBuildController controller) {
        final boolean isHostedController = controller.getStatus() == ControllerStatus.OFFLINE
            && controller.getServiceHost() != null
            && controller.getServiceHost().isVirtual();

        if (controller.isEnabled() && (controller.getStatus() == ControllerStatus.AVAILABLE || isHostedController)) {
            return controller.getName();
        } else if (controller.isEnabled()) {
            final String status = buildServer.getDisplayText(controller.getStatus());
            final String messageFormat = Messages.getString("TeamBuildHelper.BuildControllerNameFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, controller.getName(), status);
        } else {
            final String messageFormat = Messages.getString("TeamBuildHelper.BuildControllerNameDisabledFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, controller.getName());
        }
    }

    /**
     * Open build explorer in the work bench and scope it to the specified build
     * definition. Set the initial selection to the specified build.
     *
     * @param buildDefinition
     *        The initial build definition filter selection.
     * @param queuedBuild
     *        The initial build selection.
     */
    public static void openBuildExplorer(final IBuildDefinition buildDefinition, final IQueuedBuild queuedBuild) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            final IEditorPart editorPart = page.openEditor(
                new BuildExplorerEditorInput(
                    TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getServer(
                        buildDefinition.getBuildServer().getConnection()),
                    buildDefinition),
                BuildExplorer.ID);

            if (editorPart instanceof BuildExplorer) {
                final BuildExplorer buildExplorer = (BuildExplorer) editorPart;
                buildExplorer.setBuildDefinition(buildDefinition);
                if (!buildDefinition.getBuildServer().getBuildServerVersion().isV1()) {
                    buildExplorer.setSelectedQueuedBuild(queuedBuild);
                }
                CodeMarkerDispatch.dispatch(CODEMARKER_OPEN_COMPLETE);
            }
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }
    }

    public static void download(final URL dropUrl, final File localTarget, final TFSTeamProjectCollection connection)
        throws DownloadException {
        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();
        final HttpClient httpClient = connection.getHTTPClient();
        final GetMethod method = new GetMethod(dropUrl.toExternalForm());
        method.setRequestHeader("Accept", "application/zip"); //$NON-NLS-1$ //$NON-NLS-2$

        boolean cancelled = false;
        OutputStream outputStream = null;

        try {
            final int statusCode = httpClient.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                throw new DownloadException(
                    MessageFormat.format(
                        Messages.getString("TeamBuildHelper.ServerReturnedHTTPStatusFormat"), //$NON-NLS-1$
                        Integer.toString(statusCode)));
            }

            taskMonitor.begin(
                Messages.getString("TeamBuildHelper.Downloading"), //$NON-NLS-1$
                computeTaskSize(method.getResponseContentLength()));

            final InputStream input = method.getResponseBodyAsStream();
            outputStream = new FileOutputStream(localTarget);
            outputStream = new BufferedOutputStream(outputStream);

            final byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            int len;
            long totalBytesDownloaded = 0;

            while ((len = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                totalBytesDownloaded += len;
                taskMonitor.worked(1);
                taskMonitor.setCurrentWorkDescription(
                    MessageFormat.format(
                        Messages.getString("TeamBuildHelper.DownloadedCountBytesFormat"), //$NON-NLS-1$
                        totalBytesDownloaded));
                if (taskMonitor.isCanceled()) {
                    cancelled = true;
                    break;
                }
                Thread.sleep(10);
            }

        } catch (final Exception ex) {
            throw new DownloadException(ex);
        } finally {
            method.releaseConnection();
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (final IOException e) {
                }
            }
        }

        if (cancelled) {
            localTarget.delete();
        }
    }

    private static int computeTaskSize(final long responseContentLength) {
        return (int) Math.ceil(((double) responseContentLength) / DOWNLOAD_BUFFER_SIZE);
    }

}
