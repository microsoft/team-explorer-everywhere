// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsByIDCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.client.common.ui.tasks.ViewBuildReportTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewChangesetDetailsTask;
import com.microsoft.tfs.client.common.ui.wit.form.link.VersionedItemLinkParser;
import com.microsoft.tfs.client.common.ui.wit.form.link.VersionedItemLinkParser.VersionedItemLinkData;
import com.microsoft.tfs.core.ServerCapabilities;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.workitem.link.VersionedItemLinkTypeNames;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class ArtifactLinkHelpers {
    private static final Log log = LogFactory.getLog(ArtifactLinkHelpers.class);
    private static final String CHANGESET = "changeset"; //$NON-NLS-1$
    private static final String LATEST_VERSION = "latestitemversion"; //$NON-NLS-1$
    private static final String VERSIONED_ITEM = "versioneditem"; //$NON-NLS-1$
    private static final String STORYBOARD = "storyboard"; //$NON-NLS-1$
    private static final String BUILD = "build"; //$NON-NLS-1$
    private static final String COMMIT = "commit"; //$NON-NLS-1$

    public static boolean openArtifact(final Shell shell, final ArtifactID artifactID) {
        if (artifactID.getArtifactType().equalsIgnoreCase(CHANGESET)) {
            viewChangesetArtifact(artifactID);
        } else if (artifactID.getArtifactType().equalsIgnoreCase(LATEST_VERSION)) {
            viewVersionedItemArtifact(shell, artifactID);
        } else if (artifactID.getArtifactType().equalsIgnoreCase(VERSIONED_ITEM)) {
            viewVersionedItemArtifact(shell, artifactID);
        } else if (artifactID.getArtifactType().equalsIgnoreCase(STORYBOARD)) {
            viewStoryboardArtifact(artifactID);
        } else if (artifactID.getArtifactType().equalsIgnoreCase(BUILD)) {
            viewBuildArtifact(artifactID);
        } else if (artifactID.getArtifactType().equalsIgnoreCase(COMMIT)) {
            openCommitLink(artifactID);
        } else {
            return false;
        }
        return true;
    }

    public static void openHyperlinkLink(final Shell shell, String location) {
        /*
         * A Hyperlink can hold any string value up to 256 characters - not just
         * well-formed URLs. Try to detect any scheme, and if none is present,
         * use http. Visual Studio's IVsWebBrowsingService.Navigate provides
         * similar functionality.
         */

        URI uri;
        try {
            uri = URIUtils.newURI(location);
            if (StringUtil.isNullOrEmpty(uri.getScheme())) {
                location = "http://" + location; //$NON-NLS-1$
                uri = URIUtils.newURI(location);
            }
        } catch (final IllegalArgumentException e) {
            final String messageFormat = Messages.getString("WorkItemLinksControl.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, location);
            MessageBoxHelpers.errorMessageBox(shell, null, message);
            return;
        }

        BrowserFacade.launchURL(uri, location);
    }

    private static void viewChangesetArtifact(final ArtifactID artifactID) {
        final int changesetId = Integer.parseInt(artifactID.getToolSpecificID());

        final ViewChangesetDetailsTask task = new ViewChangesetDetailsTask(
            ShellUtils.getWorkbenchShell(),
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository(),
            changesetId);

        task.run();
    }

    private static void viewBuildArtifact(final ArtifactID artifactID) {
        final ViewBuildReportTask task = new ViewBuildReportTask(
            ShellUtils.getWorkbenchShell(),
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer().getConnection().getBuildServer(),
            artifactID.encodeURI(),
            artifactID.getToolSpecificID());

        task.run();
    }

    private static void viewStoryboardArtifact(final ArtifactID artifactID) {
        Launcher.launch(artifactID.getToolSpecificID());
    }

    private static void openCommitLink(final ArtifactID artifact) {
        final String[] parts = artifact.getToolSpecificID().split("/"); //$NON-NLS-1$

        if (parts.length != 3) {
            return;
        }

        final String projectID = parts[0];
        final String repositoryID = parts[1];
        final String commitID = parts[2];

        final TFSServer server =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer();
        final TFSTeamProjectCollection collection = server.getConnection();

        final ProjectInfo[] projects = server.getProjectCache().getTeamProjects();

        String projectName = null;
        for (final ProjectInfo proj : projects) {
            if (proj.getGUID().equals(projectID)) {
                projectName = proj.getName();
                break;
            }
        }

        if (projectName != null) {
            final boolean isHosted = collection.getServerCapabilities().contains(ServerCapabilities.HOSTED);
            final TSWAHyperlinkBuilder builder = new TSWAHyperlinkBuilder(collection, isHosted);
            final URI uri = builder.getGitCommitURL(projectName, repositoryID, commitID);
            BrowserFacade.launchURL(uri, uri.toString());
        }
    }

    private static void viewVersionedItemArtifact(final Shell shell, final ArtifactID artifactID) {
        Check.notNull(artifactID, "artifactID"); //$NON-NLS-1$

        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        Item item = null;

        if (VersionedItemLinkTypeNames.VERSIONED_ITEM.equals(artifactID.getArtifactType())) {
            final VersionedItemLinkData linkData = VersionedItemLinkParser.parse(artifactID);
            final VersionSpec version = new ChangesetVersionSpec(linkData.getChangesetVersion());
            final DeletedState deletedState =
                (linkData.getDeletionID() != 0) ? DeletedState.DELETED : DeletedState.NON_DELETED;

            final QueryItemsCommand queryCommand = new QueryItemsCommand(repository, new ItemSpec[] {
                new ItemSpec(linkData.getItemPath(), RecursionType.NONE)
            }, version, deletedState, ItemType.ANY, GetItemsOptions.DOWNLOAD);

            final IStatus status = UICommandExecutorFactory.newUICommandExecutor(shell).execute(queryCommand);

            if (!status.isOK()) {
                return;
            }

            final Item[] items = queryCommand.getItemSets()[0].getItems();

            if (items.length != 1 || items[0] == null) {
                MessageDialog.openError(
                    shell,
                    Messages.getString("WorkItemLinksControl.InvalidVersionedItemLinkTitle"), //$NON-NLS-1$
                    MessageFormat.format(
                        Messages.getString("WorkItemLinksControl.VersionedItemNotFoundFormat"), //$NON-NLS-1$
                        linkData.getItemPath(),
                        linkData.getChangesetVersion()));
                return;
            }

            item = items[0];
        } else if (VersionedItemLinkTypeNames.LATEST_VERSION.equals(artifactID.getArtifactType())) {
            int itemId;

            try {
                itemId = Integer.parseInt(artifactID.getToolSpecificID());
            } catch (final NumberFormatException e) {
                MessageDialog.openError(
                    shell,
                    Messages.getString("WorkItemLinksControl.InvalidVersionedItemLinkTitle"), //$NON-NLS-1$
                    MessageFormat.format(
                        Messages.getString("WorkItemLinksControl.ItemIdInvalidFormat"), //$NON-NLS-1$
                        artifactID.getToolSpecificID()));
                return;
            }

            final QueryItemsByIDCommand queryCommand = new QueryItemsByIDCommand(repository, new int[] {
                itemId
            }, Changeset.MAX, GetItemsOptions.DOWNLOAD);
            final IStatus status = UICommandExecutorFactory.newUICommandExecutor(shell).execute(queryCommand);

            if (!status.isOK()) {
                return;
            }

            final Item[] items = queryCommand.getItems();

            if (items.length != 1 || items[0] == null) {
                MessageDialog.openError(
                    shell,
                    Messages.getString("WorkItemLinksControl.InvalidVersionedItemLinkTitle"), //$NON-NLS-1$
                    MessageFormat.format(
                        Messages.getString("WorkItemLinksControl.LatestVersionNotFoundFormat"), //$NON-NLS-1$
                        artifactID.getToolSpecificID()));
                return;
            }

            item = items[0];
        } else {
            log.error(
                MessageFormat.format("Could not parse versioned artifact type: {0}", artifactID.getArtifactType())); //$NON-NLS-1$

            MessageDialog.openError(
                shell,
                Messages.getString("WorkItemLinksControl.InvalidVersionedItemLinkTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("WorkItemLinksControl.UnknownVersionedArtifactTypeFormat"), //$NON-NLS-1$
                    artifactID.getArtifactType()));

            return;
        }

        if (item.getItemType() == ItemType.FOLDER) {
            ViewFileHelper.viewServerFolder(item.getServerItem());
        } else {
            ViewFileHelper.viewFromDownloadURL(repository, item.getDownloadURL(), page, false);
        }
    }
}
