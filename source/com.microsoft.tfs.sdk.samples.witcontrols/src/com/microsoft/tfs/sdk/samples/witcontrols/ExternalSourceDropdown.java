// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.witcontrols;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.controls.generic.AutocompleteCombo;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.client.common.ui.wit.form.controls.LabelableControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.RequiredDecorationFocusListener;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;

/**
 * Auto-complete combo custom control which gets its allowed/suggested values
 * from an external source, rather than from work item rules.
 */
public abstract class ExternalSourceDropdown extends LabelableControl {
    private final static String MODIFYING_COMBO_KEY = "modifying-combo-key"; //$NON-NLS-1$
    private final static String MODIFY_LISTENER_KEY = "modify-listener-key"; //$NON-NLS-1$

    private AutocompleteCombo comboControl;

    /**
     * Method to get the allowed/suggested dropdown items for this control
     *
     * @return List of items
     */
    protected abstract String[] getDropdownItems();

    /**
     * Whether or not the field value is restricted to one of the dropdown items
     * (ALLOWED-mode if TRUE, Suggested-mode if FALSE)
     *
     * @return True if the field's value is invalid if it is not one of the
     *         dropdown items
     */
    protected boolean isValueRestrictedToDropdownItems() {
        return "true".equalsIgnoreCase(getControlDescription().getAttribute("Restricted")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Get the valid/invalid status of the specified field value
     *
     * @param fieldValue
     *        Value of the field to examine
     *
     * @return FieldStatus.VALID if the field is in the dropdown values list.
     *         FieldStatus.INVALID_LIST_VALUE if the field is non-empty and not
     *         in the list. NULL if the field is empty.
     */
    protected FieldStatus getFieldStatus(final String fieldValue) {
        if (fieldValue != null && fieldValue.length() > 0 && isValueRestrictedToDropdownItems()) {
            for (final String item : getDropdownItems()) {
                if (item.equalsIgnoreCase(fieldValue)) {
                    return FieldStatus.VALID;
                }
            }
            return FieldStatus.INVALID_LIST_VALUE;
        } else {
            return null;
        }
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }

    @Override
    protected Field getFieldForToolTipSupport() {
        if (getControlDescription().getFieldName() != null) {
            return getWorkItem().getFields().getField(getControlDescription().getFieldName());
        } else {
            return null;
        }
    }

    @Override
    protected void createControl(final Composite parent, final int numColumns) {
        comboControl = new AutocompleteCombo(parent, SWT.DROP_DOWN);
        comboControl.setEnabled(!isFormReadonly());
        comboControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numColumns, 1));

        if (getControlDescription().getFieldName() == null) {
            return;
        }

        final Field workItemField = getWorkItem().getFields().getField(getControlDescription().getFieldName());

        if (!isFormReadonly()) {
            /*
             * add a modify listener to capture changes made by the user and
             * push them into the work item
             */
            final ModifyListener fieldUpdateModifyListener = new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    try {
                        comboControl.setData(MODIFYING_COMBO_KEY, Boolean.TRUE);
                        workItemField.setValue(comboControl.getText());
                        updateFieldStatus(workItemField, comboControl.getText());
                    } finally {
                        comboControl.setData(MODIFYING_COMBO_KEY, null);
                    }
                }
            };
            comboControl.addModifyListener(fieldUpdateModifyListener);
            comboControl.setData(MODIFY_LISTENER_KEY, fieldUpdateModifyListener);
        }

        /*
         * field change listener to modify the UI when the underlying work item
         * field changes
         */
        final FieldChangeListener fieldChangeListener = new FieldChangeListener() {
            @Override
            public void fieldChanged(final FieldChangeEvent event) {
                UIHelpers.runOnUIThread(comboControl.getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        handleFieldChanged(event);
                    }
                });
            }
        };
        workItemField.addFieldChangeListener(fieldChangeListener);

        /*
         * add a dispose listener so that the field change listener is removed
         * from the work item when this UI is disposed. the work item has a
         * lifecycle independent of the UI.
         */
        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                workItemField.removeFieldChangeListener(fieldChangeListener);
            }
        });

        /*
         * fire a "fake" field change event to the field change listener this
         * sets up the initial picklist, decoration, etc for this field control
         */
        final FieldChangeEvent fieldChangeEvent = new FieldChangeEvent();
        fieldChangeEvent.field = workItemField;
        fieldChangeListener.fieldChanged(fieldChangeEvent);

        /*
         * focus listener for <Required> decorations
         */
        final RequiredDecorationFocusListener fieldControlFocusListener =
            new RequiredDecorationFocusListener(workItemField, MODIFY_LISTENER_KEY);
        comboControl.addFocusListener(fieldControlFocusListener);

        /*
         * register with the field tracker
         */
        getFieldTracker().addField(workItemField);
        getFieldTracker().setFocusReceiver(workItemField, new FieldTracker.FocusReceiver() {
            @Override
            public boolean setFocus() {
                return comboControl.setFocus();
            }
        });
    }

    /**
     * Update the work item field's status based on the new field value
     *
     * @param field
     *        Work item field
     *
     * @param newValue
     *        New value of the field
     */
    private void updateFieldStatus(final Field field, final String newValue) {
        final FieldStatus newStatus = getFieldStatus(newValue);
        if (newStatus != null && newStatus != field.getStatus()) {
            field.overrideStatus(newStatus);
        }
    }

    /**
     * Update the UI when the work item field has changed
     *
     * @param event
     *        Field changed event
     */
    private void handleFieldChanged(final FieldChangeEvent event) {
        final Field field = event.field;
        final String newValue = field.getValue() == null ? "" : field.getValue().toString(); //$NON-NLS-1$
        updateFieldStatus(field, newValue);

        /*
         * remove the modify listener from the combo to avoid triggering it when
         * we call setText()
         */
        final ModifyListener modifyListener = (ModifyListener) comboControl.getData(MODIFY_LISTENER_KEY);
        if (modifyListener != null) {
            comboControl.removeModifyListener(modifyListener);
        }

        /*
         * set the dropdown items
         */
        final String[] items = comboControl.getItems();
        final String[] newItems = getDropdownItems();
        if (!Arrays.equals(items, newItems)) {
            comboControl.setItems(newItems);
            comboControl.setVisibleItemCount(Math.min(newItems.length, 10));
        }

        final String currentValueFromWidget = RequiredDecorationFocusListener.hasDecoration(comboControl) ? "" //$NON-NLS-1$
            : comboControl.getText().trim();

        if (!newValue.equals(currentValueFromWidget)) {
            /*
             * the combo control holds a value that is not equal to the value in
             * the work item field
             */
            if (comboControl.getData(MODIFYING_COMBO_KEY) == null) {
                if (RequiredDecorationFocusListener.hasDecoration(comboControl)) {
                    RequiredDecorationFocusListener.setHasDecoration(comboControl, false);
                }
                comboControl.setText(newValue);
            }
        }

        /*
         * set the background color of the combo depending on whether or not the
         * work item field is currently valid
         *
         * note that changing the BG color does not require a layout
         */
        if (field.getStatus() != FieldStatus.VALID) {
            comboControl.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
            if (!comboControl.isFocusControl() && comboControl.getData(MODIFYING_COMBO_KEY) == null) {
                RequiredDecorationFocusListener.addDecoration(field, comboControl, null);
            }
        } else {
            comboControl.setBackground(null);
            if (!comboControl.isFocusControl() && comboControl.getData(MODIFYING_COMBO_KEY) == null) {
                RequiredDecorationFocusListener.removeDecoration(comboControl, null);
            }
        }

        /*
         * re-add the modify listener we temporarily removed
         */
        if (modifyListener != null) {
            comboControl.addModifyListener(modifyListener);
        }

        /*
         * Check for readonly fields
         */
        comboControl.setEnabled(!isFormReadonly() && field.isEditable());
    }
}
