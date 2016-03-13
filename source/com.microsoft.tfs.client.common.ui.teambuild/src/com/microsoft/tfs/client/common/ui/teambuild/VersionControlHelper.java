// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkingFolderData;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkingFolderDataCollection;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditorInput;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

/**
 * A helper class to perform common version control operations from within the
 * build UI tier.
 */
public class VersionControlHelper {
    private static final Log log = LogFactory.getLog(VersionControlHelper.class);

    // This class is roughly comparable to the Microsoft API class
    // Microsoft.TeamFoundation.Build.Controls.VersionControlHelper

    public static WorkingFolderDataCollection rerootToBuildDirLocalItems(
        final WorkingFolder[] workingFolders,
        final VersionControlClient server) {
        // Split mapped from cloaked working folders.
        final List<WorkingFolder> cloaked = new ArrayList<WorkingFolder>();
        final List<WorkingFolder> mapped = new ArrayList<WorkingFolder>();
        for (int i = 0; i < workingFolders.length; i++) {
            if (workingFolders[i].getType() == WorkingFolderType.CLOAK) {
                cloaked.add(workingFolders[i]);
            } else {
                mapped.add(workingFolders[i]);
            }
        }

        // Sort the mapped working folders into local path order.
        final WorkingFolder[] mappedFolders = mapped.toArray(new WorkingFolder[mapped.size()]);
        sortWorkingFoldersByLocalItem(mappedFolders);

        // Get all the local items - only bothered with mapped ones as cloaked
        // do not have local path.
        final String[] localItems = getLocalItems(mappedFolders);

        if (localItems.length == 1) {
            // We only have 1 local path - make it the SourceDir
            // If the mapping corresponds to a folder, then mapping is just
            // $(SourceDir) otherwise
            // it if $(SourceDir)\file
            final ItemSet itemSet = server.getItems(
                mappedFolders[0].getServerItem(),
                LatestVersionSpec.INSTANCE,
                RecursionType.NONE,
                DeletedState.NON_DELETED,
                ItemType.ANY,
                false);

            final Item[] subItems = (itemSet != null) ? itemSet.getItems() : null;

            if (itemSet != null
                && subItems != null
                && subItems.length == 1
                && ItemType.FILE == subItems[0].getItemType()) {
                final int postfixStart = localItems[0].lastIndexOf(BuildPath.PATH_SEPERATOR_CHAR);
                localItems[0] = prefixWithSourceDir(postfixStart + 1, localItems[0]);
            } else {
                localItems[0] = prefixWithSourceDir(localItems[0].length(), localItems[0]);
            }

        } else if (localItems.length > 1) {
            final int rootEndPos = findCommonRootEndPosition(localItems);

            if (rootEndPos >= 0) // we have a common root
            {
                // Loop through local paths and replace root with $(SourceDir)
                // The root might end in a \. Make sure paths are in format
                // $(SourceDir)\folderA
                // or $(SourceDir). Do not have $(SourceDir)\\folderA

                final int seperator = localItems[0].charAt(rootEndPos) == BuildPath.PATH_SEPERATOR_CHAR ? 0 : 1;
                for (int i = 1; i < localItems.length; i++) {
                    localItems[i] = prefixWithSourceDir(rootEndPos + seperator + 1, localItems[i]);
                }
                localItems[0] = prefixWithSourceDir(rootEndPos + 1, localItems[0]);
            } else {
                // (we do not have a common root)
                // Paths in a sequence like
                // \ProjectA
                // \ProjectA\trunk
                // \ProjectB
                //
                // Should become
                // $(SourceDir)\ProjectA
                // $(SourceDir)\ProjectA\trunk
                // $(SourceDir)\ProjectB
                for (int i = 0; i < localItems.length; i++) {
                    localItems[i] = prefixWithSourceDir(1, localItems[i]);
                }
            }

        }

        // Create a list of WorkingFolderDatas to return
        final WorkingFolderDataCollection returnFolders = new WorkingFolderDataCollection();

        // loop through mapped folders, adding a working folder data for each
        // using new local path
        for (int i = 0; i < mappedFolders.length; i++) {
            final WorkingFolderData folderData = new WorkingFolderData(mappedFolders[i]);
            folderData.setLocalItem(localItems[i]);
            returnFolders.add(folderData);
        }

        // loop through cloaked folders, adding a working folder data for each
        for (final Iterator<WorkingFolder> it = cloaked.iterator(); it.hasNext();) {
            returnFolders.add(new WorkingFolderData(it.next()));
        }

        // return list as typed array.
        return returnFolders;
    }

    /**
     * Loop through the paths and work what the maximum common root is
     *
     * i.e. for \project\trunk, \project\trunk\folderA, \project\trunk\folderB
     *
     * the common root would be \project\trunk
     *
     * @return the position of the end of the common root string.
     */
    private static int findCommonRootEndPosition(final String[] localItems) {
        int index = localItems[0].indexOf(BuildPath.PATH_SEPERATOR_CHAR);
        int rootEndPos = -1;
        final int firstItemEndPos = localItems[0].length() - 1;
        while (index >= 0) {
            final String prefix = localItems[0].substring(0, index + 1);
            boolean hasPrefix = true;
            for (int i = 1; i < localItems.length; i++) {
                if (!isPrefixedWithFolderPath(localItems[i], prefix)) {
                    hasPrefix = false;
                    break;
                }
            }
            if (!hasPrefix) {
                break;
            }
            rootEndPos = index;
            if (index >= firstItemEndPos) {
                break;
            }
            index = localItems[0].indexOf(BuildPath.PATH_SEPERATOR_CHAR, index + 1);
            if (index < 0) {
                index = firstItemEndPos;
            }
        }

        return rootEndPos;
    }

    private static String prefixWithSourceDir(final int postfixStartPosition, final String originalString) {
        if (postfixStartPosition >= originalString.length()) {
            return BuildConstants.SOURCE_DIR_ENVIRONMENT_VARIABLE;
        }

        return BuildConstants.SOURCE_DIR_ENVIRONMENT_VARIABLE
            + LocalPath.TFS_PREFERRED_LOCAL_PATH_SEPARATOR
            + originalString.substring(postfixStartPosition);
    }

    private static String[] getLocalItems(final WorkingFolder[] workingFolders) {
        final String[] items = new String[workingFolders.length];
        for (int i = 0; i < items.length; i++) {
            // We want to work with TFS (Windows) style paths, i.e. U:\a\b
            // So bypass the TFS to native translation in core.
            String path = workingFolders[i].getLocalItemRaw();
            path = path.replace('/', BuildPath.PATH_SEPERATOR_CHAR);
            path = path.replace('\\', BuildPath.PATH_SEPERATOR_CHAR);
            final int drivePos = path.indexOf(':');
            if (drivePos >= 0) {
                path = path.substring(drivePos + 1);
            }
            items[i] = path;
        }
        return items;
    }

    private static WorkingFolder[] sortWorkingFoldersByLocalItem(final WorkingFolder[] workingFolders) {
        Arrays.sort(workingFolders, new Comparator<WorkingFolder>() {

            @Override
            public int compare(final WorkingFolder x, final WorkingFolder y) {
                // Perform the comparison the same as
                // Microsoft.TeamFoundation.VersionControl.Client.WorkingFolderComparer
                // for local items.
                final WorkingFolder folder = x;
                final WorkingFolder folder2 = y;

                if (!folder.isCloaked() || !folder2.isCloaked()) {
                    if (folder.isCloaked()) {
                        return 1;
                    }
                    if (folder2.isCloaked()) {
                        return -1;
                    }
                    return LocalPath.compareTopDown(folder.getLocalItem(), folder2.getLocalItem());
                }
                return ServerPath.compareTopDown(folder.getServerItem(), folder2.getServerItem());

            }
        });
        return workingFolders;
    }

    public static void checkinTemporaryBuildConfigFolder(
        final TFSTeamProjectCollection connection,
        final String tempFolderPath,
        final String serverFolder,
        final boolean deleteTempFolder) throws IOException {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        final File tempDir = new File(tempFolderPath);
        if (!tempDir.exists()) {
            throw new IllegalArgumentException("The passed local folder + " + tempFolderPath + " does not exist."); //$NON-NLS-1$ //$NON-NLS-2$
        }

        final VersionControlClient server = connection.getVersionControlClient();

        final WorkingFolder[] mappings = new WorkingFolder[] {
            new WorkingFolder(serverFolder, tempDir.getCanonicalPath(), WorkingFolderType.MAP)
        };

        Workspace workspace = null;
        try {
            String workspaceName =
                System.currentTimeMillis() + "_" + "TFSBuildTemporaryWorkspace" + LocalHost.getShortName(); //$NON-NLS-1$ //$NON-NLS-2$
            if (workspaceName.length() > 64) {
                workspaceName = workspaceName.substring(0, 64);
            }

            workspace = server.createWorkspace(
                mappings,
                workspaceName,
                Messages.getString("VersionControlHelper.TemporaryWorkspaceDescription"), //$NON-NLS-1$
                WorkspaceLocation.SERVER,
                null);

            final File[] files = tempDir.listFiles();
            final List<String> filePaths = new ArrayList<String>();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && files[i].canRead()) {
                    filePaths.add(files[i].getCanonicalPath());
                }
            }
            final int numPended = workspace.pendAdd(
                filePaths.toArray(new String[filePaths.size()]),
                false,
                FileEncoding.UTF_8,
                LockLevel.UNCHANGED,
                GetOptions.NONE,
                PendChangesOptions.NONE);

            if (numPended > 0) {
                workspace.checkIn(
                    workspace.getPendingChanges().getPendingChanges(),
                    Messages.getString("VersionControlHelper.BuildCheckinComment")); //$NON-NLS-1$
            } else if (numPended == 0) {
                throw new IllegalArgumentException(Messages.getString("VersionControlHelper.InvalidServerPathText")); //$NON-NLS-1$
            }
        } finally {
            // delete workspace;
            if (workspace != null) {
                server.deleteWorkspace(workspace);
            }
        }

    }

    public static String calculateDefaultBuildFileLocation(final String teamProject, final String buildDefinitionName) {
        if (StringUtil.isNullOrEmpty(buildDefinitionName) || StringUtil.isNullOrEmpty(teamProject)) {
            return ""; //$NON-NLS-1$
        }

        return ServerPath.ROOT
            + teamProject
            + ServerPath.PREFERRED_SEPARATOR_CHARACTER
            + BuildConstants.BUILD_TYPE_FOLDER_NAME
            + ServerPath.PREFERRED_SEPARATOR_CHARACTER
            + buildDefinitionName;
    }

    public static boolean isPrefixedWithFolderPath(final String pathString, final String prefix) {
        if (prefix.length() > pathString.length()) {
            return false;
        }
        if (prefix.endsWith(BuildPath.PATH_SEPERATOR) || prefix.length() == pathString.length()) {
            return pathString.toLowerCase().startsWith(prefix.toLowerCase());
        }
        return pathString.toLowerCase().startsWith(prefix.toLowerCase())
            && (pathString.charAt(prefix.length()) == BuildPath.PATH_SEPERATOR_CHAR);
    }

    /**
     * Remove any trailing "\" from the end of a windows style local path.
     */
    public static String normalizeLocalPath(final String localPath) {
        if (localPath.endsWith("\\") && localPath.length() > 3) //$NON-NLS-1$
        {
            return localPath.substring(0, localPath.length() - 1);
        }
        return localPath;
    }

    public static void openSourceControlExplorer(final String path) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.openEditor(new VersionControlEditorInput(), VersionControlEditor.ID);
            if (VersionControlEditor.getCurrent() != null) {
                VersionControlEditor.getCurrent().setSelectedFolder(new ServerItemPath(path));
            }
        } catch (final PartInitException e) {
            log.warn("Could not open version control editor", e); //$NON-NLS-1$
        }
    }
}
