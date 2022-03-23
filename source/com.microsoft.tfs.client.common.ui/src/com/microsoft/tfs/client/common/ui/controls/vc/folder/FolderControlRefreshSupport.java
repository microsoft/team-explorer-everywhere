// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.folder;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsExtendedCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemFactory;
import com.microsoft.tfs.client.common.util.ConnectionHelper;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public class FolderControlRefreshSupport {
    private static final Log log = LogFactory.getLog(FolderControlRefreshSupport.class);

    private final FolderControl folderControl;
    private final TFSRepository repository;

    private final List<Object> items = new ArrayList<Object>();

    public FolderControlRefreshSupport(final FolderControl folderControl, final TFSRepository repository) {
        Check.notNull(folderControl, "folderControl"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.folderControl = folderControl;
        this.repository = repository;
    }

    public void refresh() {
        final TFSConnection connection = repository.getConnection();
        if (connection == null || !ConnectionHelper.isConnected(connection)) {
            items.clear();
            folderControl.getTreeViewer().setInput(new Object[] {});
            return;
        }

        items.clear();
        collectItems();
        final Object[] expandedElements = folderControl.getTreeViewer().getExpandedElements();
        final ISelection selection = folderControl.getTreeViewer().getSelection();
        final TFSFolder root = getRoot();

        /*
         * Root can come back null if the server/data was unavailable. Don't
         * update the tree control in that case. The logs will have more
         * information.
         */
        if (root == null) {
            TFSCommonUIClientPlugin.getDefault().getConsole().printErrorMessage(
                Messages.getString("FolderControlRefreshSupport.FolderCouldNotBeRefreshed")); //$NON-NLS-1$
        } else {
            folderControl.getTreeViewer().setInput(new Object[] {
                root
            });
            folderControl.getTreeViewer().setExpandedElements(expandedElements);
            if (selection != null) {
                folderControl.getTreeViewer().setSelection(selection);
            }
        }
    }

    private void collectItems() {
        final Object[] expandedElements = folderControl.getTreeViewer().getExpandedElements();
        items.addAll(Arrays.asList(expandedElements));
        final ISelection selection = folderControl.getTreeViewer().getSelection();
        if (selection instanceof IStructuredSelection) {
            for (final Iterator it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
                final Object next = it.next();
                if (!items.contains(next)) {
                    items.add(next);
                }
            }
        }
    }

    private TFSFolder getRoot() {
        ItemSpec[] itemSpecs;

        if (items.size() > 0) {
            itemSpecs = new ItemSpec[items.size()];
            int ix = 0;
            for (final Iterator<Object> it = items.iterator(); it.hasNext();) {
                itemSpecs[ix++] = new ItemSpec(((TFSItem) it.next()).getFullPath(), RecursionType.ONE_LEVEL);
            }
        } else {
            itemSpecs = new ItemSpec[] {
                new ItemSpec("$/", RecursionType.ONE_LEVEL) //$NON-NLS-1$
            };
        }

        ExtendedItem[][] queryResult;

        final DeletedState deletedState =
            folderControl.isShowDeletedItems() ? DeletedState.ANY : DeletedState.NON_DELETED;

        /*
         * Make sure to include branch information in this query.
         */
        GetItemsOptions options = GetItemsOptions.INCLUDE_BRANCH_INFO;

        if (folderControl.isShowDeletedItems()) {
            options = options.combine(GetItemsOptions.INCLUDE_SOURCE_RENAMES);
        }

        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
        final QueryItemsExtendedCommand queryCommand =
            new QueryItemsExtendedCommand(repository, itemSpecs, deletedState, ItemType.ANY, options);

        final IStatus status = new CommandExecutor().execute(queryCommand);

        if (!status.isOK()) {
            log.error(MessageFormat.format(
                "Could not query extended items for folder control refresh: {0}", //$NON-NLS-1$
                status.getMessage()), status.getException());
            return null;
        }

        queryResult = queryCommand.getItems();

        final List<TFSItem> resultItems = new ArrayList<TFSItem>();
        for (int i = 0; i < queryResult.length; i++) {
            if (queryResult[i] != null && queryResult[i].length > 0) {
                if (i == 0) {
                    resultItems.add(TFSItemFactory.createRootWithChildren(queryResult[i], true));
                } else {
                    /*
                     * This method is called for an ExtendedItem[]. We get an
                     * ExtendedItem[] for an extended item query on a single
                     * path. Typically, this means that we queried a folder: the
                     * folder will be ExtendedItem[0], and its first level
                     * children will be ExtendedItem[1 .. n]. However, there may
                     * not actually be a folder in TFS. In this case, we will
                     * only get back children as ExtendedItem[0 .. n]. (Imagine
                     * multiple adds in a folder, when the folder itself does
                     * not have a pending add and does not yet exist in TFS.)
                     * Thus, in this case, we need create nodes for all these
                     * children.
                     */
                    if (queryResult[i][0].getItemType() == ItemType.FOLDER) {
                        resultItems.add(TFSItemFactory.createWithChildren(queryResult[i], true));
                    } else {
                        resultItems.add(TFSItemFactory.createImplicitWithChildren(queryResult[i], true));
                    }
                }
            }
        }

        for (int i = 1; i < resultItems.size(); i++) {
            addInto((TFSFolder) resultItems.get(0), resultItems.get(i));
        }

        /*
         * We need to look at pending changes cache. There may be folders that
         * are implicitly being added to source control beneath this folder
         * (because there is a pending add for a child beneath that folder.)
         */
        if (resultItems.size() >= 1) {
            final TFSFolder parent = (TFSFolder) resultItems.get(0);

            final TFSItem[] implicitChildren = TFSItemFactory.getImplicitAdds(repository, parent);

            for (int i = 0; i < implicitChildren.length; i++) {
                addInto(parent, implicitChildren[i]);
            }
        }

        return resultItems.isEmpty() ? null : (TFSFolder) resultItems.get(0);
    }

    private void addInto(final TFSFolder folder, final TFSItem child) {
        if (child.getItemPath().getParent().equals(folder.getItemPath())) {
            folder.addChild(child);
        } else {
            final String nextAncestor =
                child.getItemPath().getHierarchy()[folder.getItemPath().getFolderDepth() + 1].getFullPath();
            final TFSItem item = folder.getChildByFullPath(nextAncestor);
            if (item == null) {
                final String messageFormat = "unable to locate [{0}] in [{1}]"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, nextAncestor, folder.getFullPath());
                log.warn(message);
            } else if (!(item instanceof TFSFolder)) {
                final String messageFormat = "path [{0}] in [{1}] is not a folder"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, nextAncestor, folder.getFullPath());
                log.warn(message);
            } else {
                addInto((TFSFolder) item, child);
            }
        }
    }
}
