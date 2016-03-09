// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.tasks.vc.AbstractGetTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.SetWorkingFolderTask;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFile;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PathTranslation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public abstract class GetAction extends TeamViewerAction {
    protected TFSItem[] items;

    @Override
    public void doRun(final IAction action) {
        final TFSRepository repository = getCurrentRepository();

        if (items == null || items.length == 0) {
            return;
        }

        /*
         * This working folder check logic is not in GetTask because it's Source
         * Control Explorer-specific (choosing whether a folder needs mapped
         * based on the selection of TFSFolders and TFSFiles, types which aren't
         * used for Team provider support).
         */

        // Collect folders and files separately

        final List<TypedServerItem> folders = new ArrayList<TypedServerItem>();
        final List<TypedServerItem> files = new ArrayList<TypedServerItem>();

        for (final TFSItem item : items) {
            if (item instanceof TFSFolder) {
                // TypedServerItem constructure detects if folder is ROOT and
                // uses that type
                folders.add(new TypedServerItem(item.getFullPath(), ServerItemType.FOLDER));
            } else if (item instanceof TFSFile) {
                files.add(new TypedServerItem(item.getFullPath(), ServerItemType.FILE));
            }
        }

        /*
         * Need to make sure they are all mapped. If we find an item that is not
         * mapped, we will look children that are mapped. If don't find any,
         * then the item will be marked as not mapped. At the end, if one is not
         * mapped prompt to map team project.
         */
        final List<String> knownMappedFolders = new ArrayList<String>();
        String folderToMap = null;

        for (int i = folders.size() - 1; i >= 0; i--) {
            /*
             * We skip the root folder because it is handled differently when
             * performing a get from it.
             */
            final TypedServerItem folder = folders.get(i);
            if (folder.getType() != ServerItemType.ROOT) {
                final String folderServerPath = folder.getServerPath();

                /*
                 * Note that CLOAKED items will pass this check because there is
                 * a working folder mapping (translation), which will have
                 * isCloaked() set to true.
                 */
                if (repository.getWorkspace().translateServerPathToLocalPath(folderServerPath) == null) {
                    /*
                     * Get the mapped child folders of this unmapped parent. If
                     * we find some, then they will be added to the list of
                     * mapped folders. Otherwise, we will flag this folder as an
                     * unmappped folder.
                     */
                    if (getMappedChildServerPaths(repository.getWorkspace(), folderServerPath, knownMappedFolders)) {
                        /*
                         * We have found mapped child folders, so we need to
                         * remove the parent from the list of folders to get. We
                         * also avoid incrementing the index counter to keep out
                         * position in the list.
                         */
                        folders.remove(i);
                    } else {
                        folderToMap = ServerPath.getCommonParent(folderToMap, folderServerPath);
                    }
                }
            }
        }

        for (final TypedServerItem file : files) {
            final String fileServerPath = file.getServerPath();

            final PathTranslation fileTranslation =
                repository.getWorkspace().translateServerPathToLocalPath(fileServerPath);

            if (fileTranslation == null || fileTranslation.isCloaked()) {
                final String parentFolder = ServerPath.getParent(fileServerPath);

                if (repository.getWorkspace().translateServerPathToLocalPath(parentFolder) == null) {
                    folderToMap = ServerPath.getCommonParent(folderToMap, parentFolder);
                } else if (fileTranslation == null) {
                    // In case the file is not mapped and its parent is mapped
                    // then it is one level mapping of the grand parent
                    folderToMap = ServerPath.getCommonParent(folderToMap, fileServerPath);
                } else {
                    // File is cloaked -- parent folder is not

                    /*
                     * TEE is intentionally different than Visual Studio in this
                     * case. VS saves off this item path (which is an explicitly
                     * cloaked FILE with a non-cloaked parent) to be "mapped",
                     * but it later prompts the user to REMOVE the mapping for.
                     * This must be intended as a short cut to to get the file
                     * on disk, but it's inconsistent with every other case
                     * where "Get" is invoked on files or folders (including
                     * cloaked folders). This behavior also goes bad when
                     * multiple items are selected which include > 1 cloaked
                     * files in this state: VS prompts to remove the mapping on
                     * the common PARENT of them all then fails badly during the
                     * get.
                     *
                     * TEE treats the file like any other mapped (cloak or
                     * otherwise) file; no prompt at all.
                     */

                    // VS behavior:
                    // folderToMap = ServerPath.getCommonParent(folderToMap,
                    // fileServerPath);

                }
            }
        }

        if (folderToMap != null) {
            final IStatus workingFolderStatus =
                new SetWorkingFolderTask(getShell(), repository, folderToMap, false).run();

            if (!workingFolderStatus.isOK()) {
                return;
            }
        }

        // We can safely add the known mapped folders that were found
        if (knownMappedFolders.size() > 0) {
            folders.addAll(
                Arrays.asList(
                    TypedServerItem.getTypedServerItemFromServerPaths(
                        knownMappedFolders.toArray(new String[knownMappedFolders.size()]),
                        ServerItemType.FOLDER)));
        }

        /*
         * Check for the root folder. If the root folder is in the list, we
         * issue only one get request, for the root item.
         */

        boolean foundRoot = false;
        for (final TypedServerItem folder : folders) {
            if (folder.getType() == ServerItemType.ROOT) {
                foundRoot = true;
                break;
            }
        }

        // Items remains null for whole-workspace get (root selected)
        TypedServerItem[] typedServerItemsToGet = null;

        if (foundRoot) {
            /*
             * (This comment tagged "Orcas bug 303203" in VS Dev10 source) Check
             * to see if there are any mappings in the workspace before doing a
             * workspace-level get on nothing.
             */
            if (0 == repository.getWorkspace().getFolders().length) {
                // Prompt the user to map $/ to some location before the
                // workspace-level get.
                final IStatus workingFolderStatus =
                    new SetWorkingFolderTask(getShell(), repository, ServerPath.ROOT, false).run();

                if (!workingFolderStatus.isOK()) {
                    return;
                }
            }

            // typedServerItemsToGet stays null
        } else {
            /*
             * Get remaining files and folders. Folders may have been deleted or
             * added above to optimize the request for cloaks.
             */
            typedServerItemsToGet = new TypedServerItem[files.size() + folders.size()];

            int i = 0;

            for (int j = 0; j < files.size(); j++) {
                typedServerItemsToGet[i++] = files.get(j);
            }

            for (int j = 0; j < folders.size(); j++) {
                typedServerItemsToGet[i++] = folders.get(j);
            }
        }

        final AbstractGetTask getTask = getGetTask(getShell(), repository, typedServerItemsToGet);
        getTask.run();
    }

    /**
     * Appends the mapped children of the given server path to the passed list.
     *
     * @param workspace
     *        the workspace to search for mappings (must not be
     *        <code>null</code>)
     * @param parentPath
     *        the folder name to search for in the mappings (must not be
     *        <code>null</code>)
     * @param childFolders
     *        the known mapped folders list to append to (must not be
     *        <code>null</code>)
     * @return <code>true</code> if any children were found
     */
    private boolean getMappedChildServerPaths(
        final Workspace workspace,
        final String parentPath,
        final List<String> childFolders) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(parentPath, "folderName"); //$NON-NLS-1$
        Check.notNull(childFolders, "knownMappedFolders"); //$NON-NLS-1$

        boolean found = false;
        for (final WorkingFolder workingFolder : workspace.getFolders()) {
            if (workingFolder.isCloaked()) {
                continue;
            }

            if (ServerPath.isChild(parentPath, workingFolder.getServerItem())
                && !ServerPath.equals(workingFolder.getServerItem(), parentPath)) {
                childFolders.add(workingFolder.getServerItem());
                found = true;
            }
        }

        return found;
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        items = (TFSItem[]) adaptSelectionToArray(TFSItem.class);

        final TFSRepository repository = getCurrentRepository();

        action.setEnabled(repository != null && items.length > 0);
    }

    protected abstract AbstractGetTask getGetTask(Shell shell, TFSRepository repository, TypedServerItem[] serverItems);
}
