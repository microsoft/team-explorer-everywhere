// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.query.qe.ResultOptions;

public class BaseResultOptionsControl extends BaseControl {
    private static final int BUTTON_EXTRA_SIZE = 5;

    private final FieldDefinitionCollection fieldDefinitions;
    private final ResultOptions resultOptions;

    private final ListViewer availableColumnsList;
    private final TableViewer selectedColumnsTable;
    private Button selectButton;
    private Button deselectButton;
    private Button moveUpButton;
    private Button moveDownButton;

    public BaseResultOptionsControl(
        final Composite parent,
        final int style,
        final FieldDefinitionCollection fieldDefinitions,
        final ResultOptions resultOptions) {
        super(parent, style);

        this.fieldDefinitions = fieldDefinitions;
        this.resultOptions = resultOptions;

        setLayout(new GridLayout(4, false));

        Label label = new Label(this, SWT.NONE);
        label.setText(Messages.getString("BaseResultOptionsControl.AvailableColumnsLabelText")); //$NON-NLS-1$
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

        label = new Label(this, SWT.NONE);
        label.setText(Messages.getString("BaseResultOptionsControl.SelectedColumnsLabelText")); //$NON-NLS-1$
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

        availableColumnsList = new ListViewer(this);
        availableColumnsList.setLabelProvider(new AvailableColumnsLabelProvider());
        availableColumnsList.setContentProvider(new AvailableColumnsContentProvider());
        availableColumnsList.addFilter(new AvailableColumnsFilter());
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd.widthHint = 1;
        gd.heightHint = 1;
        availableColumnsList.getControl().setLayoutData(gd);
        availableColumnsList.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                onSelectionChanged();
            }
        });
        availableColumnsList.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                select();
            }
        });

        Composite composite = createSelectDeselectComposite(this);
        composite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        selectedColumnsTable =
            new TableViewer(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
        selectedColumnsTable.setContentProvider(new SelectedColumnsContentProvider());
        selectedColumnsTable.setLabelProvider(new SelectedColumnsLabelProvider());
        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd.widthHint = 1;
        gd.heightHint = 1;
        selectedColumnsTable.getControl().setLayoutData(gd);
        selectedColumnsTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                onSelectionChanged();
            }
        });
        selectedColumnsTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                deselect();
            }
        });
        setupTable(selectedColumnsTable.getTable());

        composite = createUpDownComposite(this);
        composite.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));

        availableColumnsList.setInput(fieldDefinitions);
        selectedColumnsTable.setInput(new Object());

        if (selectedColumnsTable.getTable().getItemCount() > 0) {
            final Object firstSelectedItem = selectedColumnsTable.getElementAt(0);
            selectedColumnsTable.setSelection(new StructuredSelection(firstSelectedItem));
        } else {
            onSelectionChanged();
        }
    }

    private void refreshAvailableColumns() {
        availableColumnsList.refresh();
    }

    private void refreshSelectedColumns() {
        selectedColumnsTable.refresh();
    }

    private void onSelectionChanged() {
        setEnablement(
            (IStructuredSelection) availableColumnsList.getSelection(),
            (IStructuredSelection) selectedColumnsTable.getSelection(),
            selectedColumnsTable.getTable().getSelectionIndices(),
            selectedColumnsTable.getTable().getItemCount());
    }

    protected void setEnablement(
        final IStructuredSelection availableColumnsSelection,
        final IStructuredSelection selectedColumnsSelection,
        final int[] selectedColumnsSelectionIndices,
        final int selectedColumnsSize) {
        selectButton.setEnabled(availableColumnsSelection.size() > 0);
        deselectButton.setEnabled(selectedColumnsSelection.size() > 0);

        if (selectedColumnsSelectionIndices.length != 1) {
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
        } else {
            moveUpButton.setEnabled(selectedColumnsSelectionIndices[0] != 0);
            moveDownButton.setEnabled(selectedColumnsSelectionIndices[0] != (selectedColumnsSize - 1));
        }
    }

    protected Object[] getItemsForSelectedColumns(final ResultOptions resultOptions) {
        return new Object[] {};
    }

    protected String getTextForSelectedColumnItem(final Object selectedColumnItem, final int index) {
        return ""; //$NON-NLS-1$
    }

    protected void setupTable(final Table table) {
        table.setHeaderVisible(true);
    }

    protected boolean isFieldDefinitionAvailable(
        final FieldDefinition fieldDefinition,
        final ResultOptions resultOptions) {
        return true;
    }

    protected Object[] select(final FieldDefinition[] fieldDefinitionArray, final ResultOptions resultOptions) {
        return new Object[] {};
    }

    protected String[] deselect(final Object[] selectedColumnItems, final ResultOptions resultOptions) {
        return new String[] {};
    }

    protected void moveUp(final Object selectedColumnItem, final ResultOptions resultOptions) {

    }

    protected void moveDown(final Object selectedColumnItem, final ResultOptions resultOptions) {

    }

    protected final void updateSelectedItem(final Object item) {
        selectedColumnsTable.update(item, null);
    }

    private Composite createSelectDeselectComposite(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        final RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.spacing = getSpacing();
        composite.setLayout(layout);

        /*
         * Feature in Mac OS: arrow buttons don't work properly, left buttons
         * don't work at all (display right arrow.)
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            selectButton = new Button(composite, SWT.PUSH);
            selectButton.setText(Messages.getString("BaseResultOptionsControl.MoveRightLabelText")); //$NON-NLS-1$
        } else {
            selectButton = new Button(composite, SWT.ARROW | SWT.RIGHT);

            final Point defaultButtonSize = selectButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            selectButton.setLayoutData(
                new RowData(defaultButtonSize.x + BUTTON_EXTRA_SIZE, defaultButtonSize.y + BUTTON_EXTRA_SIZE));
        }

        selectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                select();
            }
        });

        /*
         * Feature in Mac OS: arrow buttons don't work properly, left buttons
         * don't work at all (display right arrow.)
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            deselectButton = new Button(composite, SWT.PUSH);
            deselectButton.setText(Messages.getString("BaseResultOptionsControl.MoveLeftLabelText")); //$NON-NLS-1$
        } else {
            deselectButton = new Button(composite, SWT.ARROW | SWT.LEFT);

            final Point defaultButtonSize = selectButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            deselectButton.setLayoutData(
                new RowData(defaultButtonSize.x + BUTTON_EXTRA_SIZE, defaultButtonSize.y + BUTTON_EXTRA_SIZE));
        }

        deselectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                deselect();
            }
        });

        return composite;
    }

    private void select() {
        final IStructuredSelection selection = (IStructuredSelection) availableColumnsList.getSelection();
        final Object[] objArray = selection.toArray();
        final FieldDefinition[] fieldDefinitionArray = new FieldDefinition[objArray.length];
        for (int i = 0; i < objArray.length; i++) {
            fieldDefinitionArray[i] = (FieldDefinition) objArray[i];
        }

        final Object[] selectedObjects = select(fieldDefinitionArray, resultOptions);

        refreshAvailableColumns();
        refreshSelectedColumns();

        selectedColumnsTable.setSelection(new StructuredSelection(selectedObjects));
    }

    private void deselect() {
        final IStructuredSelection selection = (IStructuredSelection) selectedColumnsTable.getSelection();
        final Object[] objArray = selection.toArray();

        final String[] fieldNames = deselect(objArray, resultOptions);

        refreshAvailableColumns();
        refreshSelectedColumns();

        final FieldDefinition[] fieldDefinitionArray = new FieldDefinition[fieldNames.length];

        for (int i = 0; i < fieldNames.length; i++) {
            fieldDefinitionArray[i] = fieldDefinitions.get(fieldNames[i]);
        }

        availableColumnsList.setSelection(new StructuredSelection(fieldDefinitionArray));
    }

    protected Composite createUpDownComposite(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        final RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.spacing = getSpacing();
        composite.setLayout(layout);

        /*
         * Feature in Mac OS: arrow buttons don't work properly, left buttons
         * don't work at all (display right arrow.)
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            moveUpButton = new Button(composite, SWT.PUSH);
            moveUpButton.setText(Messages.getString("BaseResultOptionsControl.MoveUpLabelText")); //$NON-NLS-1$
        } else {
            moveUpButton = new Button(composite, SWT.ARROW | SWT.UP);

            final Point defaultButtonSize = selectButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            moveUpButton.setLayoutData(
                new RowData(defaultButtonSize.x + BUTTON_EXTRA_SIZE, defaultButtonSize.y + BUTTON_EXTRA_SIZE));
        }

        moveUpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final Object selectedItem =
                    ((IStructuredSelection) selectedColumnsTable.getSelection()).getFirstElement();

                moveUp(selectedItem, resultOptions);

                refreshSelectedColumns();

                selectedColumnsTable.setSelection(new StructuredSelection(selectedItem));
            }
        });

        /*
         * Feature in Mac OS: arrow buttons don't work properly, left buttons
         * don't work at all (display right arrow.)
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            moveDownButton = new Button(composite, SWT.PUSH);
            moveDownButton.setText(Messages.getString("BaseResultOptionsControl.MoveDownLabelText")); //$NON-NLS-1$
        } else {
            moveDownButton = new Button(composite, SWT.ARROW | SWT.DOWN);

            final Point defaultButtonSize = selectButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            moveDownButton.setLayoutData(
                new RowData(defaultButtonSize.x + BUTTON_EXTRA_SIZE, defaultButtonSize.y + BUTTON_EXTRA_SIZE));
        }

        moveDownButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final Object selectedItem =
                    ((IStructuredSelection) selectedColumnsTable.getSelection()).getFirstElement();

                moveDown(selectedItem, resultOptions);

                refreshSelectedColumns();

                selectedColumnsTable.setSelection(new StructuredSelection(selectedItem));
            }
        });

        return composite;
    }

    private class AvailableColumnsFilter extends ViewerFilter {
        @Override
        public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
            final FieldDefinition fieldDefinition = (FieldDefinition) element;
            return isFieldDefinitionAvailable(fieldDefinition, resultOptions);
        }
    }

    private static class AvailableColumnsContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            final FieldDefinitionCollection collection = (FieldDefinitionCollection) inputElement;
            return collection.getFieldDefinitions();
        }
    }

    private static class AvailableColumnsLabelProvider extends LabelProvider {
        @Override
        public String getText(final Object element) {
            return ((FieldDefinition) element).getName();
        }
    }

    private class SelectedColumnsContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            return getItemsForSelectedColumns(resultOptions);
        }
    }

    private class SelectedColumnsLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            return getTextForSelectedColumnItem(element, columnIndex);
        }
    }
}
