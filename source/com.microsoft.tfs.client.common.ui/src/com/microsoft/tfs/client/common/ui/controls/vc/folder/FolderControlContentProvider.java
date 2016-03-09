// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.folder;

import java.util.Collection;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemFactory;

/**
 * A content provider for use by FolderControl.
 */
public class FolderControlContentProvider extends TreeContentProvider {
    // keep track of whether or not we include file children -
    // the default is false, or show folder children only
    private boolean includeFiles = false;

    private boolean showDeletedItems;

    /**
     * Creates a new FolderControlContentProvider
     *
     * @param includeFiles
     *        true to show file children, false to only show folder children
     */
    public FolderControlContentProvider(final boolean includeFiles, final boolean showDeletedItems) {
        this.includeFiles = includeFiles;
        this.showDeletedItems = showDeletedItems;
    }

    public void setShowDeletedItems(final boolean showDeletedItems) {
        this.showDeletedItems = showDeletedItems;
    }

    @Override
    public Object[] getElements(final Object parent) {
        // get the root of the tree

        if (parent instanceof Object[]) {
            return (Object[]) parent;
        } else if (parent instanceof Collection) {
            return ((Collection) parent).toArray();
        } else if (parent instanceof TFSRepository) {
            return new Object[] {
                TFSItemFactory.getRoot((TFSRepository) parent)
            };
        } else if (TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer() != null) {
            return new Object[] {
                TFSItemFactory.getRoot()
            };
        } else {
            // otherwise, return empty array. the tree will be empty
            return new Object[] {};
        }
    }

    @Override
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof TFSFolder) {
            final TFSFolder parent = (TFSFolder) parentElement;
            // if the includeFiles flag is set, return all children,
            // otherwise return only folder children
            return includeFiles ? parent.getChildren(showDeletedItems).toArray()
                : parent.getFolderChildren(showDeletedItems).toArray();
        }
        return null;
    }

    @Override
    public boolean hasChildren(final Object element) {
        return (element instanceof TFSFolder);

        // commented out for now - this generates multiple server round trips
        // for each node expansion
        // return (element instanceof TFSFolder && ((TFSFolder)
        // element).hasFolderChildren());
    }

    @Override
    public Object getParent(final Object element) {
        if (element instanceof ServerItemPath) {
            final ServerItemPath ip = (ServerItemPath) element;
            return ip.getParent();
        }
        return null;
    }
}
