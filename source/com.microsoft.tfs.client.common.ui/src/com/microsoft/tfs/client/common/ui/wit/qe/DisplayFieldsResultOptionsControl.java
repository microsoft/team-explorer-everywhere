// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldUsages;
import com.microsoft.tfs.core.clients.workitem.query.qe.DisplayField;
import com.microsoft.tfs.core.clients.workitem.query.qe.ResultOptions;

public class DisplayFieldsResultOptionsControl extends BaseResultOptionsControl {
    private Label widthLabel;
    private Text widthText;
    private DisplayField selectedDisplayField;
    private final Map nameToWidth = new HashMap();

    public DisplayFieldsResultOptionsControl(
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
        column.setText(Messages.getString("DisplayFieldsResultOptionsControl.ColumnNameName")); //$NON-NLS-1$
        tableLayout.addColumnData(new ColumnWeightData(3, 100, true));

        column = new TableColumn(table, SWT.NONE);
        column.setText(Messages.getString("DisplayFieldsResultOptionsControl.ColumnNameWidth")); //$NON-NLS-1$
        tableLayout.addColumnData(new ColumnWeightData(1, 40, true));
    }

    @Override
    protected Object[] getItemsForSelectedColumns(final ResultOptions resultOptions) {
        return resultOptions.getDisplayFields().toArray();
    }

    @Override
    protected String getTextForSelectedColumnItem(final Object selectedColumnItem, final int index) {
        final DisplayField displayField = (DisplayField) selectedColumnItem;
        if (index == 0) {
            return displayField.getFieldName();
        } else if (index == 1) {
            return String.valueOf(displayField.getWidth());
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
        if (!fieldDefinition.isQueryable()) {
            return false;
        }
        return !resultOptions.getDisplayFields().contains(fieldDefinition.getName());
    }

    @Override
    protected Object[] select(final FieldDefinition[] fieldDefinitionArray, final ResultOptions resultOptions) {
        final DisplayField[] fields = new DisplayField[fieldDefinitionArray.length];

        for (int i = 0; i < fieldDefinitionArray.length; i++) {
            final String name = fieldDefinitionArray[i].getName();
            int width;
            if (nameToWidth.containsKey(name)) {
                width = ((Integer) nameToWidth.get(name)).intValue();
            } else {
                width = ResultOptions.getDefaultColumnWidth(fieldDefinitionArray[i]);
            }

            fields[i] = new DisplayField(name, width);

            resultOptions.getDisplayFields().add(fields[i]);
        }

        return fields;
    }

    @Override
    protected String[] deselect(final Object[] selectedColumnItems, final ResultOptions resultOptions) {
        final String[] fieldNames = new String[selectedColumnItems.length];

        for (int i = 0; i < selectedColumnItems.length; i++) {
            final DisplayField displayField = (DisplayField) selectedColumnItems[i];
            resultOptions.getDisplayFields().remove(displayField);
            fieldNames[i] = displayField.getFieldName();
            nameToWidth.put(displayField.getFieldName(), new Integer(displayField.getWidth()));
        }

        return fieldNames;
    }

    @Override
    protected void moveUp(final Object selectedColumnItem, final ResultOptions resultOptions) {
        final DisplayField displayField = (DisplayField) selectedColumnItem;

        final int index = resultOptions.getDisplayFields().indexOf(displayField);
        resultOptions.getDisplayFields().removeAt(index);
        resultOptions.getDisplayFields().insert(index - 1, displayField);
    }

    @Override
    protected void moveDown(final Object selectedColumnItem, final ResultOptions resultOptions) {
        final DisplayField displayField = (DisplayField) selectedColumnItem;

        final int index = resultOptions.getDisplayFields().indexOf(displayField);
        resultOptions.getDisplayFields().removeAt(index);
        resultOptions.getDisplayFields().insert(index + 1, displayField);
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
            selectedDisplayField = null;
            widthLabel.setEnabled(false);
            widthText.setEnabled(false);
            widthText.setText(""); //$NON-NLS-1$
        } else {
            selectedDisplayField = (DisplayField) selectedColumnsSelection.getFirstElement();
            widthLabel.setEnabled(true);
            widthText.setEnabled(true);
            widthText.setText(String.valueOf(selectedDisplayField.getWidth()));
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
        innerComposite.setLayout(new RowLayout(SWT.VERTICAL));

        widthLabel = new Label(innerComposite, SWT.NONE);
        widthLabel.setText(Messages.getString("DisplayFieldsResultOptionsControl.WidthLabelText")); //$NON-NLS-1$

        widthText = new Text(innerComposite, SWT.BORDER);

        ControlSize.setCharWidthHint(widthText, 5);

        widthText.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(final VerifyEvent e) {
                final String text = e.text;
                for (int i = 0; i < text.length(); i++) {
                    if (!Character.isDigit(text.charAt(i))) {
                        e.doit = false;
                        break;
                    }
                }
            }
        });
        widthText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                if (selectedDisplayField == null) {
                    return;
                }

                final String text = widthText.getText();
                if (text.trim().length() == 0) {
                    selectedDisplayField.setWidth(0);
                } else {
                    try {
                        selectedDisplayField.setWidth(Integer.parseInt(text));
                    } catch (final NumberFormatException ex) {
                        selectedDisplayField.setWidth(0);
                    }
                }
                updateSelectedItem(selectedDisplayField);
            }
        });
        widthText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                final String text = widthText.getText();
                boolean valid = true;
                if (text.trim().length() == 0) {
                    valid = false;
                } else {
                    try {
                        final int x = Integer.parseInt(text.trim());
                        valid = (x >= 1) && (x <= 32767);
                    } catch (final NumberFormatException ex) {
                        valid = false;
                    }
                }

                if (!valid) {
                    MessageBoxHelpers.errorMessageBox(
                        getShell(),
                        Messages.getString("DisplayFieldsResultOptionsControl.InvalidWidthDialogTitle"), //$NON-NLS-1$
                        Messages.getString("DisplayFieldsResultOptionsControl.InvalidWidthDialogText")); //$NON-NLS-1$
                    widthText.setFocus();
                }
            }
        });

        return outerComposite;
    }
}
