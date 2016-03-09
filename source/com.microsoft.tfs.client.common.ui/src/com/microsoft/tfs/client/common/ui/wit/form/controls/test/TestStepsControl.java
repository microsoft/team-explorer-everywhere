// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test;

import java.util.HashSet;

import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.wit.form.controls.LabelableControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.providers.ParamDataContentProvider;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.providers.ParamDataLabelProvider;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.providers.TestStepContentProvider;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.providers.TestStepLabelProvider;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;

public class TestStepsControl extends LabelableControl {
    private static final String FIELD_DATA_SOURCE = "Microsoft.VSTS.TCM.LocalDataSource"; //$NON-NLS-1$
    public static final String VALIDATE_STEP = "ValidateStep"; //$NON-NLS-1$
    public static final String ACTION_STEP = "ActionStep"; //$NON-NLS-1$

    private WorkItemStateListener workItemStateListener;
    private TableViewer stepsTable;
    private TableViewer dataTable;

    private SashForm sash;
    private Composite bottomSashComposite;

    @Override
    protected int getControlColumns() {
        return 1;
    }

    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        final WorkItem workItem = getWorkItem();

        // Create a sash that fills the entire control. The sash will be two
        // panels. The top panel always displays the test steps and the bottom
        // panel shows parameters, if any. The bottom panel is hidden if there
        // are no parameters.
        SWTUtil.gridLayout(parent, 1, false, 0, 0);
        sash = new SashForm(parent, SWT.VERTICAL);
        SWTUtil.gridLayout(sash, 1, false, 0, 0);
        sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        // Create a table viewer to fill the top portion of the sash.
        stepsTable = createStepsTable(sash, workItem);

        // Add a composite to fill the bottom portion of the sash.
        bottomSashComposite = new Composite(sash, SWT.NONE);
        bottomSashComposite.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, true, 1, 1));
        final GridLayout gridLayout = SWTUtil.gridLayout(bottomSashComposite, 1, false, 0, 0);
        gridLayout.verticalSpacing = 2;

        // Set a proportional split between the top and bottom panes.
        final int[] weights = new int[2];
        weights[0] = 66;
        weights[1] = 34;
        sash.setWeights(weights);

        dataTable = createDataTable(bottomSashComposite, workItem);

        // Create a refresh listener for this work item that will update the
        // steps and paramters tables when triggered.
        workItemStateListener = new WorkItemStateAdapter() {
            @Override
            public void synchedToLatest(final WorkItem workItem) {
                UIHelpers.runOnUIThread(sash.getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        stepsTable.refresh();

                        final ParamDataTable[] paramDataTables = getParamDataTables();
                        if (paramDataTables.length > 0) {
                            sash.setMaximizedControl(null);
                            updateDataTableColumns(dataTable, paramDataTables[0].getColumnNames());
                            dataTable.refresh();
                        } else {
                            sash.setMaximizedControl(stepsTable.getTable());
                        }
                    }
                });
            }
        };

        workItem.addWorkItemStateListener(workItemStateListener);
        sash.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                workItem.removeWorkItemStateListener(workItemStateListener);
            }
        });
    }

    /**
     * Get the parameter data tables from the test case.
     *
     * @return the parameter data
     */
    private ParamDataTable[] getParamDataTables() {
        final String data = getFieldDataAsString(FIELD_DATA_SOURCE);
        final ParamDataTable[] paramDataTables = TestStepUtil.extractParamData(data);
        return paramDataTables;
    }

    /**
     * Create the steps table.
     *
     * @param composite
     *        the parent layout composite
     * @param workItem
     *        the test case work item
     *
     * @return the allocated table viewer
     */
    private TableViewer createStepsTable(final Composite composite, final WorkItem workItem) {
        final TableViewer viewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
        GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).span(1, 1).minHeight(75).applyTo(
            viewer.getTable());

        final Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);

        tableLayout.addColumnData(new ColumnPixelData(60, true));
        final TableColumn column1 = new TableColumn(table, SWT.NONE);
        column1.setImage(TestStepUtil.imageHelper.getImage("images/common/attachment.gif")); //$NON-NLS-1$
        column1.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(10, true));
        final TableColumn column2 = new TableColumn(table, SWT.NONE);
        column2.setText(Messages.getString("TestStepsControl.ColumnNameAction")); //$NON-NLS-1$
        column2.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(8, true));
        final TableColumn column3 = new TableColumn(table, SWT.NONE);
        column3.setText(Messages.getString("TestStepsControl.ColumnNameExpectedResult")); //$NON-NLS-1$
        column3.setResizable(true);

        viewer.setContentProvider(new TestStepContentProvider());
        viewer.setLabelProvider(new TestStepLabelProvider());
        viewer.setInput(workItem);

        return viewer;
    }

    /**
     * Create the parameter data table. The table is collapsed if there are no
     * columns defined in the work item.
     *
     * @param parent
     *        The parent layout composite.
     * @param workItem
     *        The test case work item.
     *
     * @return The allocated table viewer
     */
    private TableViewer createDataTable(final Composite parent, final WorkItem workItem) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText(Messages.getString("TestStepsControl.ParameterValuesLabelText")); //$NON-NLS-1$

        final TableViewer dataViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
        final Table dataTable = dataViewer.getTable();
        GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).span(1, 1).minHeight(75).applyTo(
            dataTable);

        dataTable.setHeaderVisible(true);
        dataTable.setLinesVisible(true);
        final TableLayout layout = new TableLayout();
        dataTable.setLayout(layout);

        final ParamDataTable[] paramDataTables = getParamDataTables();
        if (paramDataTables.length > 0) {
            sash.setMaximizedControl(null);
            updateDataTableColumns(dataViewer, paramDataTables[0].getColumnNames());
        } else {
            sash.setMaximizedControl(stepsTable.getTable());
        }

        dataViewer.setContentProvider(new ParamDataContentProvider());
        dataViewer.setLabelProvider(new ParamDataLabelProvider());
        dataViewer.setInput(workItem);

        return dataViewer;
    }

    /**
     * Compare the existing set of columns with the specified defined set of
     * column names. Do nothing if they are the same. If they differ, delete any
     * existing columns from the parameter table and allocate new columns.
     *
     * @param tableViewer
     *        the parameter data table viewer.
     * @param definedColumnNames
     *        the current set of column names contained in the work item.
     */
    private void updateDataTableColumns(final TableViewer tableViewer, final String[] definedColumnNames) {
        // Retrieve the names of the existing columns.
        final HashSet<String> existingColumnNames = new HashSet<String>();
        for (final TableColumn column : tableViewer.getTable().getColumns()) {
            existingColumnNames.add(column.getText());
        }

        // Check for a new column in the specified defined columns.
        boolean hasNewColumn = false;
        for (final String columnName : definedColumnNames) {
            if (existingColumnNames.contains(columnName)) {
                existingColumnNames.remove(columnName);
            } else {
                hasNewColumn = true;
                break;
            }
        }

        // Don't create new columns if the column set did not change.
        if (hasNewColumn || existingColumnNames.size() > 0) {
            // Dispose of the old columns.
            while (tableViewer.getTable().getColumns().length > 0) {
                tableViewer.getTable().getColumns()[0].dispose();
            }

            // Allocate a new column layout.
            final TableLayout layout = new TableLayout();
            tableViewer.getTable().setLayout(layout);

            // Allocate the new columns.
            for (final String columnName : definedColumnNames) {
                final TableColumn column = new TableColumn(tableViewer.getTable(), SWT.None);
                column.setText(columnName);
                layout.addColumnData(new ColumnWeightData(5, true));
            }

            // Size the new column layout.
            for (final TableColumn column : tableViewer.getTable().getColumns()) {
                column.pack();
            }

            tableViewer.refresh();
        }
    }
}
