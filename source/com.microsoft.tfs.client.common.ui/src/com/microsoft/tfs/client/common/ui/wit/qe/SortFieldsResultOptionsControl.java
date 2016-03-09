// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldUsages;
import com.microsoft.tfs.core.clients.workitem.query.qe.ResultOptions;
import com.microsoft.tfs.core.clients.workitem.query.qe.SortField;

public class SortFieldsResultOptionsControl extends BaseResultOptionsControl {
    private Button ascendingButton;
    private Button descendingButton;
    private SortField selectedSortField;

    public SortFieldsResultOptionsControl(
        final Composite parent,
        final int style,
        final FieldDefinitionCollection fieldDefinitions,
        final ResultOptions resultOptions) {
        super(parent, style, fieldDefinitions, resultOptions);
    }

    @Override
    protected void setupTable(final Table table) {
        super.setupTable(table);

        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);

        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(Messages.getString("SortFieldsResultOptionsControl.ColumnNameName")); //$NON-NLS-1$
        tableLayout.addColumnData(new ColumnWeightData(3, 100, true));

        column = new TableColumn(table, SWT.NONE);
        column.setText(Messages.getString("SortFieldsResultOptionsControl.ColumnNameSort")); //$NON-NLS-1$
        tableLayout.addColumnData(new ColumnWeightData(1, 40, true));
    }

    @Override
    protected Object[] getItemsForSelectedColumns(final ResultOptions resultOptions) {
        return resultOptions.getSortFields().toArray();
    }

    @Override
    protected String getTextForSelectedColumnItem(final Object selectedColumnItem, final int index) {
        final SortField sortField = (SortField) selectedColumnItem;
        if (index == 0) {
            return sortField.getFieldName();
        } else if (index == 1) {
            return sortField.isAscending() ? Messages.getString("SortFieldsResultOptionsControl.SortAscending") //$NON-NLS-1$
                : Messages.getString("SortFieldsResultOptionsControl.SortDescending"); //$NON-NLS-1$
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    @Override
    protected boolean isFieldDefinitionAvailable(
        final FieldDefinition fieldDefinition,
        final ResultOptions resultOptions) {
        if (fieldDefinition.getUsage() == FieldUsages.WORK_ITEM_LINK && !resultOptions.isLinkQuery()) {
            return false;
        }
        if (!fieldDefinition.isSortable()) {
            return false;
        }
        return !resultOptions.getSortFields().contains(fieldDefinition.getName());
    }

    @Override
    protected Object[] select(final FieldDefinition[] fieldDefinitionArray, final ResultOptions resultOptions) {
        final SortField[] fields = new SortField[fieldDefinitionArray.length];

        for (int i = 0; i < fieldDefinitionArray.length; i++) {
            fields[i] = new SortField(fieldDefinitionArray[i].getName(), true);

            resultOptions.getSortFields().add(fields[i]);
        }

        return fields;
    }

    @Override
    protected String[] deselect(final Object[] selectedColumnItems, final ResultOptions resultOptions) {
        final String[] fieldNames = new String[selectedColumnItems.length];

        for (int i = 0; i < selectedColumnItems.length; i++) {
            final SortField sortField = (SortField) selectedColumnItems[i];
            resultOptions.getSortFields().remove(sortField);
            fieldNames[i] = sortField.getFieldName();
        }

        return fieldNames;
    }

    @Override
    protected void moveUp(final Object selectedColumnItem, final ResultOptions resultOptions) {
        final SortField sortField = (SortField) selectedColumnItem;

        final int index = resultOptions.getSortFields().indexOf(sortField);
        resultOptions.getSortFields().removeAt(index);
        resultOptions.getSortFields().insert(index - 1, sortField);
    }

    @Override
    protected void moveDown(final Object selectedColumnItem, final ResultOptions resultOptions) {
        final SortField sortField = (SortField) selectedColumnItem;

        final int index = resultOptions.getSortFields().indexOf(sortField);
        resultOptions.getSortFields().removeAt(index);
        resultOptions.getSortFields().insert(index + 1, sortField);
    }

    @Override
    protected void setEnablement(
        final IStructuredSelection availableColumnsSelection,
        final IStructuredSelection selectedColumnsSelection,
        final int[] selectedColumnsSelectionIndices,
        final int selectedColumnsSize) {
        super.setEnablement(
            availableColumnsSelection,
            selectedColumnsSelection,
            selectedColumnsSelectionIndices,
            selectedColumnsSize);

        if (selectedColumnsSelection.size() != 1) {
            ascendingButton.setEnabled(false);
            descendingButton.setEnabled(false);
        } else {
            selectedSortField = (SortField) selectedColumnsSelection.getFirstElement();
            ascendingButton.setEnabled(!selectedSortField.isAscending());
            descendingButton.setEnabled(selectedSortField.isAscending());
        }
    }

    @Override
    protected Composite createUpDownComposite(final Composite parent) {
        final Composite outerComposite = new Composite(parent, SWT.NONE);

        final RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.spacing = 20;
        outerComposite.setLayout(layout);

        super.createUpDownComposite(outerComposite);

        final Composite innerComposite = new Composite(outerComposite, SWT.NONE);
        final FillLayout innerLayout = new FillLayout(SWT.VERTICAL);
        innerLayout.spacing = 5;
        innerComposite.setLayout(innerLayout);

        ascendingButton = new Button(innerComposite, SWT.NONE);
        ascendingButton.setText(Messages.getString("SortFieldsResultOptionsControl.AscendingButtonText")); //$NON-NLS-1$
        ascendingButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedSortField.setAscending(true);
                updateSelectedItem(selectedSortField);
                ascendingButton.setEnabled(false);
                descendingButton.setEnabled(true);
            }
        });

        descendingButton = new Button(innerComposite, SWT.NONE);
        descendingButton.setText(Messages.getString("SortFieldsResultOptionsControl.DescendingButtonText")); //$NON-NLS-1$
        descendingButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedSortField.setAscending(false);
                updateSelectedItem(selectedSortField);
                ascendingButton.setEnabled(true);
                descendingButton.setEnabled(false);
            }
        });

        return outerComposite;
    }
}
