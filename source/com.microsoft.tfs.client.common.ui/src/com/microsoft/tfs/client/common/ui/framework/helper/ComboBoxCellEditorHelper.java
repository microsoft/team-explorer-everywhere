// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Item;

import com.microsoft.tfs.client.common.ui.framework.celleditor.SafeComboBoxCellEditor;

public abstract class ComboBoxCellEditorHelper {
    private final TableViewer viewer;
    private SafeComboBoxCellEditor cellEditor;
    private final int columnIndexToEdit;
    private String propertyIdentifier;

    protected abstract boolean shouldAllowEdit(Object element);

    protected abstract String[] getAvailableOptions(Object element);

    protected abstract String getSelectedOption(Object element);

    protected abstract void setSelectedOption(Object element, String option);

    public ComboBoxCellEditorHelper(final TableViewer viewer, final int columnIndexToEdit) {
        this.viewer = viewer;
        this.columnIndexToEdit = columnIndexToEdit;
        setupViewer();
    }

    private void setupViewer() {
        final int columnCount = viewer.getTable().getColumnCount();

        final String[] columnProperties = (String[]) viewer.getColumnProperties();
        final CellEditor[] cellEditors = new CellEditor[columnCount];

        if (columnProperties[columnIndexToEdit] == null) {
            columnProperties[columnIndexToEdit] = "celleditor" + columnIndexToEdit; //$NON-NLS-1$
        }

        propertyIdentifier = columnProperties[columnIndexToEdit];

        cellEditor = new SafeComboBoxCellEditor(viewer.getTable(), new String[] {}, SWT.READ_ONLY);
        cellEditors[columnIndexToEdit] = cellEditor;

        viewer.setColumnProperties(columnProperties);
        viewer.setCellEditors(cellEditors);
        viewer.setCellModifier(new CellModifier());
    }

    private class CellModifier implements ICellModifier {
        @Override
        public boolean canModify(final Object element, final String property) {
            return propertyIdentifier.equals(property) && shouldAllowEdit(element);
        }

        @Override
        public Object getValue(final Object element, final String property) {
            final String[] options = getAvailableOptions(element);
            final String selectedOption = getSelectedOption(element);

            cellEditor.setItems(getAvailableOptions(element));
            int foundIx = -1;
            for (int i = 0; i < options.length && foundIx < 0; i++) {
                if (options[i].equals(selectedOption)) {
                    foundIx = i;
                }
            }

            if (foundIx >= 0) {
                return new Integer(foundIx);
            } else {
                return new Integer(0);
            }
        }

        @Override
        public void modify(Object element, final String property, final Object value) {
            if (element instanceof Item) {
                element = ((Item) element).getData();
            }
            final String[] options = getAvailableOptions(element);
            final int selectedIx = ((Integer) value).intValue();
            if (selectedIx != -1) {
                setSelectedOption(element, options[selectedIx]);
                viewer.refresh();
            }
        }
    }
}
