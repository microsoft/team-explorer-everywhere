// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.tooltip.IToolTipProvider;
import com.microsoft.tfs.client.common.ui.framework.tooltip.TableToolTipSupport;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.util.Check;

public abstract class BaseWITComponentControl extends Composite {
    protected static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private final TFSServer server;

    private TableViewer tableViewer;
    private final WorkItem workItem;

    protected abstract IToolTipProvider getToolTipProvider();

    protected abstract Image getImageForColumn(Object element, int columnIndex);

    protected abstract String getTextForColumn(Object element, int columnIndex);

    protected abstract void handleSelectionChanged(Object[] selectedItems);

    protected abstract void handleItemDoubleClick(Object selectedItem);

    protected abstract String[] getTableColumnNames();

    protected abstract Object[] getItemsFromWorkItem(WorkItem workItem);

    protected abstract int getNumberOfButtons();

    protected abstract void createButtons(Composite parent);

    protected abstract void fillMenuBeforeShow(IMenuManager manager);

    public BaseWITComponentControl(
        final Composite parent,
        final int style,
        final TFSServer server,
        final WorkItem workItem) {
        super(parent, style);
        Check.notNull(server, "server"); //$NON-NLS-1$

        this.server = server;
        this.workItem = workItem;
    }

    public final void init() {
        final GridLayout layout = new GridLayout(2, false);
        setLayout(layout);

        final int tableStyle = SWT.BORDER | SWT.FULL_SELECTION | (getStyle() & SWT.MULTI);

        tableViewer = new TableViewer(this, tableStyle);
        final Table table = tableViewer.getTable();

        final GridData tableLayoutData = new GridData();
        table.setLayoutData(tableLayoutData);
        tableLayoutData.verticalSpan = getNumberOfButtons() + 1;
        tableLayoutData.horizontalAlignment = SWT.FILL;
        tableLayoutData.verticalAlignment = SWT.FILL;
        tableLayoutData.grabExcessHorizontalSpace = true;
        tableLayoutData.grabExcessVerticalSpace = true;

        /*
         * create the buttons
         */
        createButtons(this);

        /*
         * create the table columns
         */
        createTableColumns(table);

        /*
         * add a content provider to the table
         */
        tableViewer.setContentProvider(new ContentProviderAdapter() {
            @Override
            public Object[] getElements(final Object inputElement) {
                return getItemsFromWorkItem((WorkItem) inputElement);
            }
        });

        /*
         * add a label provider to the table
         */
        tableViewer.setLabelProvider(new LabelProvider());

        /*
         * set up sorting
         */
        addSorting(tableViewer);

        /*
         * create context menu
         */
        final MenuManager menu = new MenuManager("#PopUp"); //$NON-NLS-1$
        menu.setRemoveAllWhenShown(true);
        menu.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillMenuBeforeShow(manager);
            }
        });
        tableViewer.getControl().setMenu(menu.createContextMenu(tableViewer.getControl()));

        /*
         * handle table double-clicks
         */
        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                handleItemDoubleClick(getSelectedItem());
            }
        });

        /*
         * add a selection listener to the table to hook for enablement
         */
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                handleSelectionChanged(getSelectedItems());
            }
        });

        /*
         * add tool tip support to the table
         */
        TableToolTipSupport.addToolTipSupport(table, getShell(), getToolTipProvider());

        hookInit();
    }

    protected void hookInit() {

    }

    protected TFSServer getServer() {
        return server;
    }

    private void createTableColumns(final Table table) {
        table.setLinesVisible(false);
        table.setHeaderVisible(true);

        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);

        final String[] columnNames = getTableColumnNames();
        for (int i = 0; i < columnNames.length; i++) {
            tableLayout.addColumnData(new ColumnWeightData(1, true));
            final TableColumn column1 = new TableColumn(table, SWT.NONE);
            column1.setText(columnNames[i]);
            column1.setResizable(true);
        }
    }

    protected Object getSelectedItem() {
        return ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
    }

    protected Object[] getSelectedItems() {
        return ((IStructuredSelection) tableViewer.getSelection()).toArray();
    }

    protected void bindWorkItemToTable() {
        /*
         * handle initial validation / enablement
         */
        handleSelectionChanged(new Object[] {});

        /*
         * finally, set the work item as the table viewer's input
         */
        tableViewer.setInput(workItem);
    }

    protected Button createButton(
        final Composite parent,
        final String text,
        final SelectionListener selectionListener) {
        final Button button = new Button(parent, SWT.NONE);
        button.setText(text);
        button.addSelectionListener(selectionListener);

        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        button.setLayoutData(gd);

        return button;
    }

    protected void addSorting(final TableViewer tableViewer) {
        final TableViewerSorter sorter = new TableViewerSorter(tableViewer);
        tableViewer.setSorter(sorter);
    }

    protected void refresh() {
        if (!tableViewer.getTable().isDisposed()) {
            tableViewer.refresh();
        }
    }

    protected WorkItem getWorkItem() {
        return workItem;
    }

    private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
        implements ITableLabelProvider, IFontProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            return getImageForColumn(element, columnIndex);
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final String columnText = getTextForColumn(element, columnIndex);

            if (columnText != null) {
                return columnText;
            }

            return ""; //$NON-NLS-1$
        }

        @Override
        public Font getFont(final Object element) {
            // the following commented block should be added in once the work
            // item state management
            // APIs are more defined - in particular, once support is added for
            // revert / refresh,
            // it should be clearer what we need to hook in order to
            // successfully implement the
            // following
            /*
             * WITComponent component = (WITComponent) element; if
             * (component.isNewlyCreated() || component.isDirty()) { return
             * JFaceResources
             * .getFontRegistry().getBold(JFaceResources.DEFAULT_FONT); }
             */
            return null;
        }
    }
}
