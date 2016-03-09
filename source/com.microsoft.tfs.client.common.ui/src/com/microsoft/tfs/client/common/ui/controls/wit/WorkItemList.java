// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.wit;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.CheckedElements;
import com.microsoft.tfs.client.common.ui.framework.helper.ComboBoxCellEditorHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.ElementIntComparator;
import com.microsoft.tfs.client.common.ui.framework.helper.GenericElementsContentProvider;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.table.EqualSizeTableLayout;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.util.Check;

/**
 * @deprecated use {@link WorkItemCheckinTable}
 */
@Deprecated
public class WorkItemList extends Composite {
    private final TFSServer server;

    private CheckboxTableViewer tableViewer;

    private CheckedElements selectedWorkItems;
    private boolean forHistoryDetails = false;

    public WorkItemList(
        final Composite parent,
        final int style,
        final TFSServer server,
        final boolean forHistoryDetails) {
        super(parent, style);

        Check.notNull(server, "server"); //$NON-NLS-1$

        this.server = server;
        this.forHistoryDetails = forHistoryDetails;

        setLayout(new FillLayout());
        int tableStyle = SWT.FULL_SELECTION | SWT.BORDER;
        if (forHistoryDetails == false) {
            tableStyle |= SWT.CHECK;
        }
        final Table table = new Table(this, tableStyle);
        table.setLayout(new EqualSizeTableLayout());
        table.setHeaderVisible(true);
        createTableColumns(table);
        tableViewer = new CheckboxTableViewer(table);
        tableViewer.setContentProvider(new GenericElementsContentProvider());
        tableViewer.setLabelProvider(new WorkItemListLabelProvider());
        tableViewer.setInput(new Object());

        selectedWorkItems = new CheckedElements(tableViewer) {
            @Override
            protected void onCheck(final Object element) {
                ((WorkItemCheckinInfo) element).setActionToDefault();
                tableViewer.refresh();
            }

            @Override
            protected void onUncheck(final Object element) {
                ((WorkItemCheckinInfo) element).clearAction();
                tableViewer.refresh();
            }
        };
        if (forHistoryDetails == false) {
            new ComboBoxCellEditorHelper(tableViewer, 4) {
                @Override
                protected boolean shouldAllowEdit(final Object element) {
                    return selectedWorkItems.contains(element);
                }

                @Override
                protected String[] getAvailableOptions(final Object element) {
                    return ((WorkItemCheckinInfo) element).getAvailableActionStrings();
                }

                @Override
                protected String getSelectedOption(final Object element) {
                    return ((WorkItemCheckinInfo) element).getActionString();
                }

                @Override
                protected void setSelectedOption(final Object element, final String option) {
                    ((WorkItemCheckinInfo) element).setActionFromString(option);
                }
            };
        }

        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                workItemDoubleClicked((WorkItemCheckinInfo) selection.getFirstElement());
            }
        });

        final TableViewerSorter sorter = new TableViewerSorter(tableViewer);
        sorter.setComparator(1, new ElementIntComparator() {
            @Override
            protected int getInt(final Object element) {
                return ((WorkItemCheckinInfo) element).getWorkItem().getFields().getID();
            }
        });
        tableViewer.setSorter(sorter);

        addContextMenu();
    }

    public CheckboxTableViewer getViewer() {
        return tableViewer;
    }

    public void resetLayout() {
        final Table table = tableViewer.getTable();
        ((EqualSizeTableLayout) table.getLayout()).reset();
        table.layout();
    }

    private void addContextMenu() {
        final MenuManager menuMgr = new MenuManager("#PopUp"); //$NON-NLS-1$
        final IAction detailsAction = new Action() {
            @Override
            public void run() {
                final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                workItemDoubleClicked((WorkItemCheckinInfo) selection.getFirstElement());
            }
        };
        detailsAction.setText(Messages.getString("WorkItemList.DetailsActionText")); //$NON-NLS-1$
        detailsAction.setEnabled(false);
        menuMgr.add(detailsAction);

        tableViewer.getControl().setMenu(menuMgr.createContextMenu(tableViewer.getControl()));

        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                final boolean selectedItem = selection.size() > 0;
                detailsAction.setEnabled(selectedItem);
            }
        });
    }

    public List getSelectedWorkItems() {
        return selectedWorkItems.getElements();
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    private void createTableColumns(final Table table) {
        final TableColumn column1 = new TableColumn(table, SWT.NONE);
        column1.setText(Messages.getString("WorkItemList.ColumnNameType")); //$NON-NLS-1$
        column1.setResizable(true);

        final TableColumn column2 = new TableColumn(table, SWT.NONE);
        column2.setText(Messages.getString("WorkItemList.ColumnNameId")); //$NON-NLS-1$
        column2.setResizable(true);

        final TableColumn column3 = new TableColumn(table, SWT.NONE);
        column3.setText(Messages.getString("WorkItemList.ColumnNameTitle")); //$NON-NLS-1$
        column3.setResizable(true);

        final TableColumn column4 = new TableColumn(table, SWT.NONE);
        column4.setText(Messages.getString("WorkItemList.ColumnNameState")); //$NON-NLS-1$
        column4.setResizable(true);
        if (forHistoryDetails == false) {
            final TableColumn column5 = new TableColumn(table, SWT.NONE);
            column5.setText(Messages.getString("WorkItemList.ColumnNameAction")); //$NON-NLS-1$
            column5.setResizable(true);
        }
    }

    public void setWorkItemCheckinInfos(final List workItemCheckinInfos) {
        /*
         * MAC BUG: setInput() does not clear check selection if the number of
         * new table items is the same as the number of old table items. Clear
         * check selection explicitly. See Eclipse bug 212957.
         */
        tableViewer.setCheckedElements(new Object[0]);
        tableViewer.setInput(workItemCheckinInfos);
        selectedWorkItems.clearElements();
    }

    private void workItemDoubleClicked(final WorkItemCheckinInfo clickedItem) {
        /*
         * WorkItem[] workItems = new WorkItem[workItemCheckinInfos.size()]; int
         * ix = 0; int clickedIndex = 0; for (Iterator it =
         * workItemCheckinInfos.iterator(); it.hasNext(); ) {
         * WorkItemCheckinInfo wii = (WorkItemCheckinInfo) it.next(); if
         * (clickedItem.getWorkItem().getId() == wii.getWorkItem().getId()) {
         * clickedIndex = ix; } workItems[ix++] = wii.getWorkItem(); }
         *
         * DlgWorkItemStaticDisplay dlg = new
         * DlgWorkItemStaticDisplay(getShell(), workItems, clickedIndex);
         *
         * dlg.open();
         */
        WorkItemEditorHelper.openEditor(server, clickedItem.getWorkItem());
    }
}
