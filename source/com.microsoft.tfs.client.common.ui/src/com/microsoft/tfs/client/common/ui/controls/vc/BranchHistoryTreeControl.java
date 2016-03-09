// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.SystemColor;
import com.microsoft.tfs.client.common.ui.helpers.TableTreeHelper;
import com.microsoft.tfs.client.common.ui.helpers.TableTreeHelper.TableTreeHelperFlags;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistoryTreeItem;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistoryUtil;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.util.Hierarchical;

/**
 * Control for displaying a branch history tree.
 */
public class BranchHistoryTreeControl extends Composite implements IPostSelectionProvider {

    private final TableTreeViewer tableTreeViewer;
    private final Table table;
    private final TableTree tableTree;

    /**
     * SWT table styles that will always be used.
     */
    private static int TREE_STYLES = SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL;

    /**
     * SWT table styles that will only be used if the client passes them in.
     */
    private static int OPTIONAL_TREE_STYLES = SWT.MULTI | SWT.SINGLE | SWT.READ_ONLY;

    private volatile boolean displayLatestVersion = true;

    public BranchHistoryTreeControl(final Composite parent, final int style) {
        this(parent, style, true);
    }

    /**
     *
     *
     * @param parent
     * @param style
     */
    public BranchHistoryTreeControl(final Composite parent, final int style, final boolean displayLatestVersion) {
        super(parent, style);

        this.displayLatestVersion = displayLatestVersion;

        final FillLayout layout = new FillLayout();
        setLayout(layout);

        tableTreeViewer = new TableTreeViewer(this, TREE_STYLES | (style & OPTIONAL_TREE_STYLES));
        tableTree = tableTreeViewer.getTableTree();
        table = tableTree.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        createTableColumns(table);

        tableTreeViewer.setContentProvider(new BranchHistoryContentProvider());
        tableTreeViewer.setLabelProvider(new TableLabelProvider());

        final BranchHistoryTreeItem dummyItem = new BranchHistoryDummyItem(""); //$NON-NLS-1$

        tableTreeViewer.setInput(dummyItem);
    }

    private void createTableColumns(final Table table) {
        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);

        tableLayout.addColumnData(new ColumnWeightData(5, true));
        final TableColumn column1 = new TableColumn(table, SWT.NONE);
        column1.setText(Messages.getString("BranchHistoryTreeControl.ColumnHeaderFileName")); //$NON-NLS-1$
        column1.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column2 = new TableColumn(table, SWT.NONE);
        column2.setText(Messages.getString("BranchHistoryTreeControl.ColumnHeaderBranchedFromVersion")); //$NON-NLS-1$
        column2.setResizable(true);

        if (displayLatestVersion) {
            tableLayout.addColumnData(new ColumnWeightData(1, true));
            final TableColumn column3 = new TableColumn(table, SWT.NONE);
            column3.setText(Messages.getString("BranchHistoryTreeControl.ColumnHeaderLatestVersion")); //$NON-NLS-1$
            column3.setResizable(true);
        }
    }

    @Override
    public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
        tableTreeViewer.addPostSelectionChangedListener(listener);
    }

    @Override
    public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
        tableTreeViewer.removePostSelectionChangedListener(listener);
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        tableTreeViewer.addSelectionChangedListener(listener);
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        tableTreeViewer.removeSelectionChangedListener(listener);
    }

    @Override
    public ISelection getSelection() {
        return tableTreeViewer.getSelection();
    }

    @Override
    public void setSelection(final ISelection selection) {
        tableTreeViewer.setSelection(selection);
    }

    public void addDoubleClickListener(final IDoubleClickListener listener) {
        tableTreeViewer.addDoubleClickListener(listener);
    }

    public void removeDoubleClickListener(final IDoubleClickListener listener) {
        tableTreeViewer.removeDoubleClickListener(listener);
    }

    public BranchHistoryTreeItem getSelectedBranchHistoryTreeItem() {
        final ISelection selection = tableTreeViewer.getSelection();

        if (selection == null || !(selection instanceof IStructuredSelection)) {
            return null;
        }

        final BranchHistoryTreeItem item = (BranchHistoryTreeItem) ((IStructuredSelection) selection).getFirstElement();

        /* Don't leak BranchHistoryDummyItem objects */
        if (item == null || item instanceof BranchHistoryDummyItem) {
            return null;
        }

        return item;
    }

    /**
     * Set the item that you want to query branch history for.
     */
    public void setSourceItem(final Workspace workspace, final TypedServerItem item) {
        // Reset tree to show that we are querying branches.
        final BranchHistoryTreeItem parent = (BranchHistoryTreeItem) tableTreeViewer.getInput();
        parent.getChildrenAsList().clear();

        final BranchHistoryTreeItem queryItem = new BranchHistoryInProgressDummyItem(
            Messages.getString("BranchHistoryTreeControl.InProgressDummyTreeItemText")); //$NON-NLS-1$
        parent.addChild(queryItem);
        tableTreeViewer.refresh();

        if (item == null) {
            return;
        }

        /* Kick off a background thread to do the query */
        final Runnable queryRunnable = new Runnable() {
            @Override
            public void run() {
                BranchHistory history = null;
                Exception exception = null;

                try {
                    // create a new itemSpec so that we don't recurse
                    final ItemSpec branchSpec = new ItemSpec(item.getServerPath(), RecursionType.NONE);
                    history = workspace.getBranchHistory(branchSpec, LatestVersionSpec.INSTANCE);

                    if (displayLatestVersion) {
                        BranchHistoryUtil.updateServerItems(workspace, LatestVersionSpec.INSTANCE, history);
                    }
                } catch (final Exception e) {
                    history = null;
                    exception = e;
                }

                final BranchHistory branchHistory = history;
                final IStatus status = (exception == null) ? Status.OK_STATUS
                    : new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, exception.getMessage(), null);

                /* Post the results to the UI thread */
                UIHelpers.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (isDisposed()) {
                            return;
                        }

                        if (status.isOK()) {
                            if (branchHistory == null || !branchHistory.hasChildren()) {
                                final BranchHistoryTreeItem parent = (BranchHistoryTreeItem) tableTreeViewer.getInput();
                                parent.getChildrenAsList().clear();

                                final BranchHistoryTreeItem queryItem = new BranchHistoryDummyItem(
                                    Messages.getString("BranchHistoryTreeControl.TreeItemNoBranchesFound")); //$NON-NLS-1$
                                parent.addChild(queryItem);
                                tableTreeViewer.refresh();
                            } else {
                                tableTreeViewer.setInput(branchHistory);
                                tableTreeViewer.expandAll();
                                tableTreeViewer.setSelection(new StructuredSelection(branchHistory.getRequestedItem()));
                            }
                        } else {
                            ErrorDialog.openError(getShell(), "Could Not Query Branch History", null, status); //$NON-NLS-1$
                        }
                    }
                });
            }
        };

        new Thread(queryRunnable).start();
    }

    private class TableLabelProvider extends LabelProvider
        implements ITableLabelProvider, IFontProvider, IColorProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final BranchHistoryTreeItem historyItem = (BranchHistoryTreeItem) element;
            switch (columnIndex) {
                case 0: // Server Item
                    final Item item = historyItem.getItem();

                    final String serverPath = (item != null) ? item.getServerItem()
                        : Messages.getString("BranchHistoryTreeControl.ColumnTextNoPermissionToReadThisBranchItem"); //$NON-NLS-1$

                    /*
                     * Most window systems don't deal well with TableTreeControl
                     * and we need to pad out the levels with spaces to present
                     * hierarchy. Unfortunate, but true.
                     */
                    return getPadding(historyItem) + serverPath;

                case 1: // change type
                    if (historyItem.getParent() == null || historyItem instanceof BranchHistoryDummyItem) {
                        return ""; //$NON-NLS-1$
                    }
                    return "" + historyItem.getFromItemChangesetID(); //$NON-NLS-1$

                case 2: // latest version
                    if (displayLatestVersion == false
                        || historyItem.getItem() == null
                        || historyItem instanceof BranchHistoryDummyItem) {
                        return ""; //$NON-NLS-1$
                    }

                    if (historyItem.getItem().getDeletionID() != 0) {
                        return Messages.getString("BranchHistoryTreeControl.LatestVersionDeleted"); //$NON-NLS-1$
                    } else {
                        return "" + historyItem.getItem().getChangeSetID(); //$NON-NLS-1$
                    }
            }
            return ""; //$NON-NLS-1$
        }

        /*
         * Feature in Mac OS: TableTree objects are not properly hierarchical,
         * everything looks flat. Fake it with some spaces. (Meh.)
         */
        private String getPadding(final BranchHistoryTreeItem item) {
            /* Dummy items don't get padded, they're not part of a hierarchy. */
            if (item instanceof BranchHistoryDummyItem) {
                return ""; //$NON-NLS-1$
            }

            TableTreeHelperFlags flags = TableTreeHelperFlags.NONE;

            if (item.isRequested()) {
                flags = flags.combine(TableTreeHelperFlags.FONT_BOLD);
            }

            return TableTreeHelper.getPadding(item.getLevel(), item.hasChildren(), flags);
        }

        @Override
        public Font getFont(final Object element) {
            // Make tree item bold if it is the one that we requested branch
            // history on.
            final BranchHistoryTreeItem item = (BranchHistoryTreeItem) element;

            if (item.isRequested()) {
                return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
            } else if (item instanceof BranchHistoryInProgressDummyItem) {
                return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
            }

            return null;
        }

        @Override
        public Color getForeground(final Object element) {
            final BranchHistoryTreeItem item = (BranchHistoryTreeItem) element;

            /* Dim deleted items */
            if (displayLatestVersion) {
                if (item.getItem().getDeletionID() != 0) {
                    return SystemColor.getDimmedWidgetForegroundColor(getShell().getDisplay());
                }
            }

            return null;
        }

        @Override
        public Color getBackground(final Object element) {
            return null;
        }
    }

    private class BranchHistoryContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getChildren(final Object element) {
            return ((Hierarchical) element).getChildren();
        }

        @Override
        public Object getParent(final Object element) {
            return ((Hierarchical) element).getParent();
        }

        @Override
        public boolean hasChildren(final Object element) {
            return ((Hierarchical) element).hasChildren();
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            return getChildren(inputElement);
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        }
    }

    private static class BranchHistoryDummyItem extends BranchHistoryTreeItem {
        public BranchHistoryDummyItem(final String text) {
            super(text, 0);
        }
    }

    private static class BranchHistoryInProgressDummyItem extends BranchHistoryDummyItem {
        public BranchHistoryInProgressDummyItem(final String text) {
            super(text);
        }
    }
}
