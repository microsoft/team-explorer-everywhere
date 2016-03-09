// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchPage;

import com.microsoft.tfs.client.common.commands.vc.AbstractGetToTempLocationCommand;
import com.microsoft.tfs.client.common.commands.vc.GetDownloadURLToTempLocationCommand;
import com.microsoft.tfs.client.common.commands.vc.GetVersionedItemToTempLocationCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromptViewOldVersionDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFile;
import com.microsoft.tfs.client.common.util.ExtensionLoader;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Contains static methods for viewing local folders, local files, server files
 * (possibly historical), or {@link TFSFile}s (which may be latest, or may
 * require a download) in the current product. External tools may be used,
 * depending on preferences and contributions from other plugins.
 * </p>
 *
 * <p>
 * These methods must all be run on the UI thread.
 * </p>
 *
 * @threadsafety thread-compatible
 */
public class ViewFileHelper {
    private static final Log log = LogFactory.getLog(ViewFileHelper.class);

    private static final String SERVER_ITEM_BROWSER_EXTENSION_POINT_ID =
        "com.microsoft.tfs.client.common.ui.serverItemBrowser"; //$NON-NLS-1$

    /**
     * The server item browser loaded on-demand from the extension point.
     */
    private static ServerItemBrowser serverItemBrowser = null;

    /**
     * Views a local file or folder using the {@link FileViewer} available from
     * this plug-in's default instance.
     *
     * @param localPath
     *        the local path to the file or folder being viewed (must not be
     *        <code>null</code> or empty)
     * @param page
     *        the active workbench page (must not be <code>null</code>)
     * @param inModalContext
     *        true if the application is in a modal context and a viewer that
     *        works inside a modal context must be used (for example, the viewer
     *        is internal and opens a new top-level window, or the viewer is
     *        external and opens its own application), false if the context is
     *        non-modal and the viewer may open an editor in the workbench
     *
     * @see TFSCommonUIClientPlugin#getFileViewer()
     */
    public static void viewLocalFileOrFolder(
        final String localPath,
        final IWorkbenchPage page,
        final boolean inModalContext) {
        Check.notNullOrEmpty(localPath, "path"); //$NON-NLS-1$
        Check.notNull(page, "page"); //$NON-NLS-1$

        ViewFileHelper.log.debug(MessageFormat.format(
            "Viewing path {0} (inModalContext is {1})", //$NON-NLS-1$
            localPath,
            inModalContext));

        /*
         * Check the plug-in contributions first.
         */
        if (TFSCommonUIClientPlugin.getDefault().getProductPlugin().getFileViewer().viewFile(
            localPath,
            page,
            inModalContext)) {
            ViewFileHelper.log.debug(MessageFormat.format("Viewer successfully viewed path {0}", localPath)); //$NON-NLS-1$
            return;
        }

        /*
         * Use the basic Launcher class which always opens an OS tool.
         */

        ViewFileHelper.log.debug(
            MessageFormat.format(
                "No viewer successfully viewed path {0}, using fallback {1} class", //$NON-NLS-1$
                localPath,
                Launcher.class.getName()));

        final File file = new File(localPath);
        Launcher.launch(file.getAbsolutePath());
        return;
    }

    /**
     * Like {@link #viewLocalFileOrFolder(String, IWorkbenchPage, boolean)}, but
     * only works for files, and fetches the specified version of the file from
     * history to a temporary directory before launching the viewer. The file is
     * immediately fetched and viewed, the user is not prompted.
     *
     * @param repository
     *        the repository to use to get the historical file from (must not be
     *        <code>null</code>)
     * @param serverFilePath
     *        the server path to the file or folder being viewed (must not be
     *        <code>null</code> or empty)
     * @param queryVersion
     *        the version of the file the given server path identifies (if
     *        <code>null</code>, viewViersion is used as the query version)
     * @param viewVersion
     *        the version of the file to fetch from the server (must not be
     *        <code>null</code>)
     * @param page
     *        the active workbench page (must not be <code>null</code>)
     * @param inModalContext
     *        true if the application is in a modal context and a viewer that
     *        works inside a modal context must be used (for example, the viewer
     *        is internal and opens a new top-level window, or the viewer is
     *        external and opens its own application), false if the context is
     *        non-modal and the viewer may open an editor in the workbench
     */
    public static void viewVersionedFile(
        final TFSRepository repository,
        final String serverFilePath,
        final VersionSpec queryVersion,
        final VersionSpec viewVersion,
        final IWorkbenchPage page,
        final boolean inModalContext) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(serverFilePath, "serverPath"); //$NON-NLS-1$
        Check.notNull(viewVersion, "version"); //$NON-NLS-1$
        Check.notNull(page, "page"); //$NON-NLS-1$

        final GetVersionedItemToTempLocationCommand command = new GetVersionedItemToTempLocationCommand(
            repository,
            serverFilePath,
            viewVersion,
            queryVersion != null ? queryVersion : viewVersion);

        viewFromDownloadCommand(command, page, inModalContext);
    }

    public static void viewFromDownloadURL(
        final TFSRepository repository,
        final String downloadUrl,
        final IWorkbenchPage page,
        final boolean inModalContext) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(downloadUrl, "downloadUrl"); //$NON-NLS-1$
        Check.notNull(page, "page"); //$NON-NLS-1$

        final GetDownloadURLToTempLocationCommand command =
            new GetDownloadURLToTempLocationCommand(repository, downloadUrl);

        viewFromDownloadCommand(command, page, inModalContext);
    }

    private static void viewFromDownloadCommand(
        final AbstractGetToTempLocationCommand command,
        final IWorkbenchPage page,
        final boolean inModalContext) {
        Check.notNull(command, "command"); //$NON-NLS-1$
        Check.notNull(page, "page"); //$NON-NLS-1$

        final ICommandExecutor executor =
            UICommandExecutorFactory.newUICommandExecutor(page.getWorkbenchWindow().getShell());
        final IStatus status = executor.execute(command);

        if (status == Status.OK_STATUS) {
            final String localPath = command.getTempLocation();

            final String messageFormat = "downloaded item [{0}] to [{1}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, command.getFileDescription(), localPath);
            ViewFileHelper.log.info(message);

            ViewFileHelper.viewLocalFileOrFolder(localPath, page, inModalContext);
        }
    }

    /**
     * Views a {@link TFSFile}, which describes an item which may already be at
     * its latest version in a working folder on disk. If the file is at its
     * latest version, the file is simply viewed with
     * {@link #viewLocalFileOrFolder(String, IWorkbenchPage, boolean)}. If the
     * file is not at its latest, the user is prompted whether to view the
     * existing local version or view the latest version from the server
     * (choosing the latter invokes
     * {@link #viewHistoricalFile(TFSRepository, String, VersionSpec, IWorkbenchPage, boolean)}
     * ). If there is no file on disk at all, the user is prompted whether to
     * download and view the given item.
     *
     * @param repository
     *        the repository to use to get the historical file from (must not be
     *        <code>null</code>)
     * @param file
     *        the {@link TFSFile} to view (must not be <code>null</code>)
     * @param page
     *        the active workbench page (must not be <code>null</code>)
     * @param inModalContext
     *        true if the application is in a modal context and a viewer that
     *        works inside a modal context must be used (for example, the viewer
     *        is internal and opens a new top-level window, or the viewer is
     *        external and opens its own application), false if the context is
     *        non-modal and the viewer may open an editor in the workbench
     *
     * @warning this method must be called on the UI thread, because it may
     *          raise dialogs
     */
    public static void viewTFSFile(
        final TFSRepository repository,
        final TFSFile file,
        final IWorkbenchPage page,
        final boolean inModalContext) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(file, "file"); //$NON-NLS-1$
        Check.notNull(page, "page"); //$NON-NLS-1$

        /*
         * Does the file exist locally?
         */
        if (file.getExtendedItem() != null && file.getExtendedItem().getLocalItem() != null) {
            if (!file.isLatest()) {
                /*
                 * If the file isn't at the latest version, prompt the user for
                 * what they want to do.
                 */
                final PromptViewOldVersionDialog dlg =
                    new PromptViewOldVersionDialog(page.getWorkbenchWindow().getShell());

                if (dlg.open() == IDialogConstants.CANCEL_ID) {
                    /*
                     * Cancelled the prompt - abort the view file operation.
                     */
                    return;
                }

                if (dlg.getSelectedOption() == PromptViewOldVersionDialog.SERVER_OPTION) {
                    /*
                     * User selected to view the latest (server) version. Query
                     * on the server path at the local version.
                     */
                    ViewFileHelper.viewVersionedFile(
                        repository,
                        file.getSourceServerPath(),
                        new ChangesetVersionSpec(file.getLocalVersion()),
                        LatestVersionSpec.INSTANCE,
                        page,
                        inModalContext);
                    return;
                }
            }

            /*
             * Either file is at latest version, or user selected to view the
             * old (workspace) version.
             */
            if (file.getLocalPath() != null) {
                ViewFileHelper.viewLocalFileOrFolder(file.getLocalPath(), page, inModalContext);
            }
        } else {
            /*
             * File is not local.
             */

            if (MessageBoxHelpers.dialogConfirmPrompt(
                page.getWorkbenchWindow().getShell(),
                Messages.getString("ViewFileHelper.ConfirmViewDialogTitle"), //$NON-NLS-1$
                Messages.getString("ViewFileHelper.ConfirmViewDialogText"))) //$NON-NLS-1$
            {
                /*
                 * Query on the server path and remote version, since there is
                 * no local version.
                 */
                ViewFileHelper.viewVersionedFile(
                    repository,
                    file.getSourceServerPath(),
                    new ChangesetVersionSpec(file.getRemoteVersion()),
                    LatestVersionSpec.INSTANCE,
                    page,
                    inModalContext);
            }
        }
    }

    /**
     * Browses to a server item, using extension points to find a provider who
     * can handle this (hopefully Version Control Explorer.)
     *
     * @param serverItem
     *        The server item (file or folder) to browse to (not
     *        <code>null</code>)
     */
    public static void viewServerFolder(final String serverItem) {
        Check.notNull(serverItem, "serverItem"); //$NON-NLS-1$

        if (getServerItemBrowser() != null) {
            getServerItemBrowser().browse(serverItem);
        }
    }

    private static ServerItemBrowser getServerItemBrowser() {
        if (serverItemBrowser == null) {
            try {
                serverItemBrowser = (ServerItemBrowser) ExtensionLoader.loadSingleExtensionClass(
                    SERVER_ITEM_BROWSER_EXTENSION_POINT_ID);
            } catch (final Exception e) {
                log.error("Could not load server item browser via extension point", e); //$NON-NLS-1$
                serverItemBrowser = null;
            }
        }

        return serverItemBrowser;
    }
}
