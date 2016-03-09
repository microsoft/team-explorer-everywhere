// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;

import com.microsoft.tfs.client.common.commands.vc.FindChangesetChildrenCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickListener;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.client.common.vc.HistoryManager;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.Platform;

public class HistoryTreeControl extends Composite implements ISelectionProvider, IHistoryControl {
    public static final String HISTORY_TABLE_ID = "HistoryTreeControl.tableTreeViewer"; //$NON-NLS-1$

    private ISelection selection;

    private final HistoryTableTreeViewer tableTreeViewer;

    private final MenuManager contextMenu;

    // TODO Copy action

    public HistoryTreeControl(final Composite parent, final int style) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());

        tableTreeViewer = new HistoryTableTreeViewer(this, style);
        final Table table = tableTreeViewer.getTableTree().getTable();
        AutomationIDHelper.setWidgetID(table, HISTORY_TABLE_ID);
        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        tableLayout.addColumnData(new ColumnWeightData(30, 30, true));
        createTableColumn(table, Messages.getString("HistoryTreeControl.ColumNameChangeset")); //$NON-NLS-1$
        tableLayout.addColumnData(new ColumnWeightData(40, 40, true));
        createTableColumn(table, Messages.getString("HistoryTreeControl.ColumnNameChange")); //$NON-NLS-1$
        tableLayout.addColumnData(new ColumnWeightData(30, 40, true));
        createTableColumn(table, Messages.getString("HistoryTreeControl.ColumnNameUser")); //$NON-NLS-1$
        tableLayout.addColumnData(new ColumnWeightData(60, 60, true));
        createTableColumn(table, Messages.getString("HistoryTreeControl.ColumnNameDate")); //$NON-NLS-1$
        tableLayout.addColumnData(new ColumnWeightData(100, 100, true));
        createTableColumn(table, Messages.getString("HistoryTreeControl.ColumnNamePath")); //$NON-NLS-1$
        tableLayout.addColumnData(new ColumnWeightData(100, 100, true));
        createTableColumn(table, Messages.getString("HistoryTreeControl.ColumnNameComment")); //$NON-NLS-1$

        tableTreeViewer.setContentProvider(
            new ContentProvider(UICommandExecutorFactory.newUICommandExecutor(getShell())));
        tableTreeViewer.setLabelProvider(new LabelProvider(tableTreeViewer));

        contextMenu = new MenuManager("#popup"); //$NON-NLS-1$
        contextMenu.setRemoveAllWhenShown(true);
        tableTreeViewer.getTableTree().setMenu(contextMenu.createContextMenu(table));
        contextMenu.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillMenu(manager);
            }
        });

        tableTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                selection = event.getSelection();
            }
        });

    }

    private void createTableColumn(final Table table, final String text) {
        final TableColumn col = new TableColumn(table, SWT.NONE);
        col.setText(text);
        col.pack();
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        tableTreeViewer.addSelectionChangedListener(listener);
    }

    @Override
    public ISelection getSelection() {
        return selection;
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        tableTreeViewer.removeSelectionChangedListener(listener);
    }

    @Override
    public void setSelection(final ISelection selection) {
        tableTreeViewer.setSelection(selection);
    }

    @Override
    public void addDoubleClickListener(final DoubleClickListener listener) {
        // TODO Check why we deviate from jface double click
        tableTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                listener.doubleClick(
                    new com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickEvent(
                        event.getSource(),
                        event.getSelection()));
            }
        });
    }

    @Override
    public void addMenuListener(final IMenuListener listener) {
        contextMenu.addMenuListener(listener);
    }

    @Override
    public IAction getCopyAction() {
        return null;
    }

    @Override
    public Changeset getSelectedChangeset() {
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection sel = (IStructuredSelection) selection;
            if (sel.getFirstElement() instanceof Changeset) {
                final Changeset c = (Changeset) sel.getFirstElement();
                return c;
            }
        }
        return null;
    }

    @Override
    public void refresh() {
        tableTreeViewer.refresh();
    }

    @Override
    public void setInput(final HistoryInput input) {
        tableTreeViewer.setInput(input);
        selection = tableTreeViewer.getSelection();
    }

    private void fillMenu(final IMenuManager manager) {
        manager.add(new Separator(StandardActionConstants.PRIVATE_CONTRIBUTIONS));
        manager.add(new Separator(StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS));
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        // manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS,
        // copyAction);
    }

    @Override
    public void registerContextMenu(final IWorkbenchPartSite site) {
        site.registerContextMenu(contextMenu, this);
    }
}

class ContentProvider extends TreeContentProvider {
    private final ICommandExecutor executor;

    /**
     * Key is a child Changeset, value is its parent {@link Changeset} (or null)
     * when queried via {@link #getChildren(Object)}.
     */
    private final Map<Changeset, Changeset> changesetChildToParent = new HashMap<Changeset, Changeset>();

    private TFSRepository repository;

    public ContentProvider(final ICommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof Changeset) {
            final Changeset parentChangeset = (Changeset) parentElement;
            final FindChangesetChildrenCommand cmd = new FindChangesetChildrenCommand(repository, parentChangeset);
            final IStatus status = executor.execute(cmd);

            if (status != null && status.isOK()) {
                final Changeset[] children = cmd.getChildrenChangesets();

                for (int i = 0; i < children.length; i++) {
                    changesetChildToParent.put(children[i], parentChangeset);
                }

                return children;
            }
        }
        return null;
    }

    @Override
    public boolean hasChildren(final Object element) {
        if (element instanceof Changeset) {
            final Changeset original = (Changeset) element;

            if (HistoryManager.mayHaveChildren(repository, original)) {
                /*
                 * Detect (and prevent showing children for) cyclic renames by
                 * walking up the ancestry to find a duplicate.
                 *
                 * Because our control model for history items is very lacking,
                 * we use a map of children to parent to find old elements.
                 */

                final String originalServerItem = (original.getChanges() != null && original.getChanges().length > 0)
                    ? original.getChanges()[0].getItem().getServerItem() : null;

                Changeset current = original;
                while (current != null) {
                    final Changeset ancestor = changesetChildToParent.get(current);

                    /*
                     * Test for same changeset ID and same server path.
                     * Different paths mean not a full cycle has been detected.
                     */
                    if (ancestor != null && ancestor.getChangesetID() == original.getChangesetID()) {
                        final String ancestorServerItem =
                            (ancestor.getChanges() != null && ancestor.getChanges().length > 0)
                                ? ancestor.getChanges()[0].getItem().getServerItem() : null;

                        if (originalServerItem == ancestorServerItem
                            || (originalServerItem != null
                                && ancestorServerItem != null
                                && ServerPath.equals(originalServerItem, ancestorServerItem))) {
                            return false;
                        }
                    }

                    current = ancestor;
                }

                // Has legitimate children.
                return true;
            }
        }

        return false;
    }

    @Override
    public Object[] getElements(final Object inputElement) {
        if (inputElement instanceof HistoryInput) {
            final HistoryInput historyInput = (HistoryInput) inputElement;
            final Iterator iter = historyInput.queryHistory();
            final ArrayList changesets = new ArrayList();
            while (iter.hasNext()) {
                HistoryManager.addChild(changesets, (Changeset) iter.next(), historyInput.isSlotMode());
            }
            return changesets.toArray();
        }
        return null;
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        super.inputChanged(viewer, oldInput, newInput);
        if (newInput instanceof HistoryInput) {
            final HistoryInput in = (HistoryInput) newInput;
            repository = in.getRepository();
        }
    }

}

class LabelProvider implements ITableLabelProvider {
    private static final int MAC_PADDING_SPACES = 7;
    private static final int GTK_PADDING_SPACES = 7;

    private final HistoryTableTreeViewer viewer;

    public LabelProvider(final HistoryTableTreeViewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        return null;
    }

    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        if (element instanceof Changeset) {
            final Changeset c = (Changeset) element;
            switch (columnIndex) {
                case 0:
                    String idString = ChangesetDisplayFormatter.getIDString(c);

                    /*
                     * Mac OS X doesn't leave enough room for the expansion
                     * button, so we have to calculate manual padding.
                     */
                    if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
                        idString = getMacPadding(c) + idString;
                    } else if (WindowSystem.isCurrentWindowSystem(WindowSystem.GTK)) {
                        idString = getGTKPadding(c) + idString;
                    }

                    return idString;

                case 1:
                    return ChangesetDisplayFormatter.getChangeString(c);

                case 2:
                    final HistoryInput input = (HistoryInput) viewer.getInput();
                    return ChangesetDisplayFormatter.getUserString(input.getRepository(), c);

                case 3:
                    return DateHelper.getDefaultDateTimeFormat().format(c.getDate().getTime());

                case 4:
                    return getPath(c);

                case 5:
                    return ChangesetDisplayFormatter.getCommentString(c);

                default:
                    break;
            }
        }
        return null;
    }

    private String getPath(final Changeset c) {
        if (c.getChanges() == null || c.getChanges().length <= 0) {
            return ""; //$NON-NLS-1$
        }
        return c.getChanges()[0].getItem().getServerItem();
    }

    @Override
    public void addListener(final ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(final Object element, final String property) {
        return false;
    }

    @Override
    public void removeListener(final ILabelProviderListener listener) {
    }

    private String getMacPadding(final Changeset c) {
        final HistoryInput input = (HistoryInput) viewer.getInput();

        int depth = viewer.getLevel(c);
        if (depth >= 1 && HistoryManager.mayHaveChildren(input.getRepository(), c)) {
            depth--;
        }

        final int paddingSpaces = depth * MAC_PADDING_SPACES;
        final StringBuffer padding = new StringBuffer(paddingSpaces);
        for (int i = 0; i < paddingSpaces; i++) {
            padding.append(' ');
        }
        return padding.toString();
    }

    private String getGTKPadding(final Changeset c) {
        viewer.getInput();

        final int depth = (viewer.getLevel(c) - 1);

        final int paddingSpaces = depth * GTK_PADDING_SPACES;
        final StringBuffer padding = new StringBuffer(paddingSpaces);
        for (int i = 0; i < paddingSpaces; i++) {
            padding.append(' ');
        }
        return padding.toString();
    }

}

class HistoryTableTreeViewer extends TableTreeViewer {

    public HistoryTableTreeViewer(final Composite parent, final int style) {
        super(parent, style);
    }

    public int getLevel(final Changeset c) {
        int level = 1;
        TableTreeItem item = (TableTreeItem) findItem(c);
        while (item.getParentItem() != null) {
            item = item.getParentItem();
            level++;
        }
        return level;
    }

}
