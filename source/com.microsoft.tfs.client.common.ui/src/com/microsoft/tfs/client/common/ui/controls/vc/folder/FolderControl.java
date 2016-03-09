// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.folder;

import java.util.Stack;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.ItemPathComparer;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemLabelProvider;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemViewerSorter;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Provides a control that allows browsing of the server tree.
 *
 * Optionally, the control state (expanded and selected elements) can be saved
 * and restored - see saveState() and restoreState().
 */
public class FolderControl extends Composite implements ISelectionProvider {

    // the JFace viewer being used
    private final TreeViewer treeViewer;
    private boolean showDeletedItems;

    public FolderControl(final Composite parent, final int style) {
        this(parent, style, false);
    }

    /**
     * Create a new FolderControl.
     *
     * @param includeFiles
     *        true to include file children in the tree, false to show server
     *        children only
     */
    public FolderControl(final Composite parent, final int style, final boolean includeFiles) {
        this(parent, style, includeFiles, true);
    }

    public FolderControl(
        final Composite parent,
        final int style,
        final boolean includeFiles,
        final boolean greyUnmapped) {
        this(parent, style, includeFiles, greyUnmapped, false);
    }

    public FolderControl(
        final Composite parent,
        final int style,
        final boolean includeFiles,
        final boolean greyUnmapped,
        final boolean showDeletedItems) {
        super(parent, style);

        this.showDeletedItems = showDeletedItems;

        setLayout(new FillLayout());

        final int treeViewerStyle = SWT.BORDER | (style & SWT.MULTI);
        treeViewer = new TreeViewer(this, treeViewerStyle);

        // used to provide comparison between ItemPaths and TFSItems
        treeViewer.setComparer(new ItemPathComparer());

        // fills in the tree content
        treeViewer.setContentProvider(new FolderControlContentProvider(includeFiles, showDeletedItems));

        // provides a label for each node
        if (greyUnmapped) {
            treeViewer.setLabelProvider(new FolderControlLabelProvider());
        } else {
            treeViewer.setLabelProvider(new TFSItemLabelProvider());
        }

        // sorts TFSItems and TFSFolders into categories, then by label within
        // the category
        treeViewer.setSorter(new TFSItemViewerSorter());

        // toggle expand/collapse a folder node on double-clicking
        treeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final TFSItem item = (TFSItem) ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
                if (item instanceof TFSFolder && ((TFSFolder) item).hasChildren()) {
                    final boolean expanded = treeViewer.getExpandedState(item);
                    treeViewer.setExpandedState(item, !expanded);
                }
            }
        });
    }

    public boolean isShowDeletedItems() {
        return showDeletedItems;
    }

    public void setShowDeletedItems(final boolean showDeletedItems) {
        this.showDeletedItems = showDeletedItems;
        ((FolderControlContentProvider) treeViewer.getContentProvider()).setShowDeletedItems(showDeletedItems);
    }

    public void setLabelProvider(final IBaseLabelProvider labelProvider) {
        treeViewer.setLabelProvider(labelProvider);
    }

    public void setExpandedItems(final ServerItemPath[] expandedItems) {
        treeViewer.setExpandedElements(expandedItems);
    }

    public Object[] getExpandedElements() {
        return treeViewer.getExpandedElements();
    }

    /**
     * Sets the selected item in the tree, using an ItemPath.
     *
     * @param selectedPath
     *        and ItemPath representing the path to select in the tree
     */
    public void setSelectedItem(final ServerItemPath selectedPath) {
        if (!treeViewer.getTree().isDisposed()) {
            /*
             * Walk up the parentage, creating a list of elements (root first) -
             * if we're given a path that doesn't exist, we want to reveal the
             * deepest parent path that does exist.
             */
            final Stack<ServerItemPath> pathElements = new Stack<ServerItemPath>();

            for (ServerItemPath parentPath = selectedPath; parentPath != null; parentPath = parentPath.getParent()) {
                pathElements.add(parentPath);
            }

            /* Reveal elements beginning at the root. */
            while (!pathElements.isEmpty()) {
                treeViewer.reveal(pathElements.pop());
            }

            /* Only try to select the actual path given to us. */
            final ISelection selection = new StructuredSelection(selectedPath);
            treeViewer.setSelection(selection);
        }
    }

    /**
     * @return the currently selected TFSItem in the tree, or null if there is
     *         no current selection.
     */
    public TFSItem getSelectedItem() {
        final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
        if (selection != null) {
            return (TFSItem) selection.getFirstElement();
        }
        return null;
    }

    /**
     * @see ISelectionProvider.addSelectionChangedListener(
     *      ISelectionChangedListener )
     */
    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        treeViewer.addSelectionChangedListener(listener);
    }

    /**
     * @see ISelectionProvider.getSelection()
     */
    @Override
    public ISelection getSelection() {
        return treeViewer.getSelection();
    }

    /**
     * @see ISelectionProvider.removeSelectionChangedListener(
     *      ISelectionChangedListener)
     */
    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        treeViewer.removeSelectionChangedListener(listener);
    }

    /**
     * @see ISelectionProvider.setSelection(ISelection)
     */
    @Override
    public void setSelection(final ISelection selection) {
        treeViewer.setSelection(selection);
    }

    /**
     * Populates the FolderControl's tree using the server root ($/) as the root
     * of the tree.
     */
    public FolderControl populateTreeUsingServerRoot() {
        treeViewer.setInput(new Object());
        return this;
    }

    public FolderControl populateTreeUsingServerRoot(final Workspace workspace) {
        treeViewer.setInput(workspace);
        return this;
    }

    /**
     * @return the TreeViewer used by this folder control
     */
    public TreeViewer getTreeViewer() {
        return treeViewer;
    }

    /**
     * Refreshes this folder control.
     */
    public void refresh() {
        treeViewer.refresh();
    }

    public void refresh(final Object element) {
        treeViewer.refresh(element);
    }

    public void expandToLevel(final Object elementOrTreePath, final int level) {
        treeViewer.expandToLevel(elementOrTreePath, level);
    }
}
