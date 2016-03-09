// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.ui;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.Adapters;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderActionInfo;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderWrapper;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;

public class DataProviderOutputControl extends Composite {
    private static final Log log = LogFactory.getLog(DataProviderOutputControl.class);

    private final StackLayout stackLayout;
    private final TableViewer tableViewer;
    private final Text text;
    private DataProviderWrapper dataProvider;

    public DataProviderOutputControl(final Composite parent, final int style) {
        super(parent, style);

        stackLayout = new StackLayout();
        setLayout(stackLayout);

        tableViewer = new TableViewer(this, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
        text = new Text(this, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);

        tableViewer.setContentProvider(new ContentProvider());
        tableViewer.setLabelProvider(new LabelProvider());

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);

        addActions();

        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final DataProviderActionInfo action = dataProvider.getDataProviderInfo().getDefaultAction();
                if (action != null) {
                    final Row row = (Row) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
                    action.getAction().run(getShell(), row.getTag());
                }
            }
        });
    }

    private void addActions() {
        final MenuManager menuMgr = new MenuManager("#PopUp"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new MenuListener());

        tableViewer.getTable().setMenu(menuMgr.createContextMenu(tableViewer.getTable()));
    }

    private class MenuListener implements IMenuListener {
        @Override
        public void menuAboutToShow(final IMenuManager manager) {
            final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            final boolean enableCopyAction = (selection.size() > 0);

            if (enableCopyAction) {
                final IAction copyAction = new Action() {
                    @Override
                    public void run() {
                        copyToClipboard();
                    }
                };
                copyAction.setText(Messages.getString("DataProviderOutputControl.CopyClipboardActionText")); //$NON-NLS-1$
                manager.add(copyAction);

                DataProviderActionInfo[] actions = null;
                if (dataProvider.getDataProviderInfo().hasActions()) {
                    final boolean enableExtraActions = ((IStructuredSelection) tableViewer.getSelection()).size() == 1;
                    if (enableExtraActions) {
                        actions = dataProvider.getDataProviderInfo().getActions();

                        manager.add(new Separator());

                        for (int i = 0; i < actions.length; i++) {
                            final IAction extraAction = new ActionAdapter(actions[i]);
                            manager.add(extraAction);
                        }
                    }
                }
            }
        }
    }

    private class ActionAdapter extends Action {
        private final DataProviderActionInfo dataProviderAction;

        public ActionAdapter(final DataProviderActionInfo dataProviderActionInfo) {
            dataProviderAction = dataProviderActionInfo;
            setText(dataProviderActionInfo.getLabel());
        }

        @Override
        public void run() {
            final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            final Row row = (Row) selection.getFirstElement();
            dataProviderAction.getAction().run(getShell(), row.getTag());
        }
    }

    private void copyToClipboard() {
        final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        final List rows = selection.toList();
        final TabularData viewTable = (TabularData) tableViewer.getInput();
        final TabularData selectionTable = new TabularData(viewTable, (Row[]) rows.toArray(new Row[rows.size()]));
        selectionTable.setSorted(true);
        final String value = (String) Adapters.get(selectionTable, String.class);
        UIHelpers.copyToClipboard(value);
    }

    public void setData(final DataProviderWrapper dataProvider) {
        this.dataProvider = dataProvider;

        if (dataProvider == null) {
            populateText(""); //$NON-NLS-1$
        } else {
            final TabularData tabularData = (TabularData) Adapters.get(dataProvider.getData(), TabularData.class);
            if (tabularData != null) {
                populateTable(tabularData);
            } else {
                final String string = (String) Adapters.get(dataProvider.getData(), String.class);
                if (string != null) {
                    populateText(string);
                } else {
                    final String messageFormat = "dataProvider [{0}] did not produce valid ouput for the UI"; //$NON-NLS-1$
                    final String message =
                        MessageFormat.format(messageFormat, dataProvider.getDataProviderInfo().getID());

                    log.warn(message);
                    populateText(""); //$NON-NLS-1$
                }
            }
        }
    }

    private void populateTable(final TabularData tabularData) {
        final Table table = tableViewer.getTable();

        table.setRedraw(false);

        tableViewer.setSorter(null);

        TableColumn[] columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
            columns[i].dispose();
        }

        final String[] columnValues = tabularData.getColumns();
        for (int i = 0; i < columnValues.length; i++) {
            final TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(columnValues[i]);
        }

        tableViewer.setInput(tabularData);

        final TableViewerSorter sorter = new TableViewerSorter(tableViewer);
        tableViewer.setSorter(sorter);

        columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
            columns[i].pack();
        }

        table.setRedraw(true);

        stackLayout.topControl = table;
        layout();
    }

    private void populateText(final String value) {
        text.setText(value);
        stackLayout.topControl = text;
        layout();
    }

    private static class ContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            return ((TabularData) inputElement).getRows();
        }
    }

    private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final Row row = (Row) element;
            final Object value = row.getValues()[columnIndex];
            String text = (String) Adapters.get(value, String.class);
            if (text == null) {
                text = ""; //$NON-NLS-1$
            }
            return text;
        }
    }
}
