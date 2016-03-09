// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.text.DateFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import com.microsoft.tfs.client.common.ui.controls.generic.DatepickerCombo;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;

public class DateTimeControl extends LabelableControl {
    @Override
    protected Field getFieldForToolTipSupport() {
        return getWorkItem().getFields().getField(getControlDescription().getFieldName());
    }

    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        /*
         * get the work item field this field control is for
         */
        final Field workItemField = getWorkItem().getFields().getField(getControlDescription().getFieldName());

        /*
         * create the DatepickerCombo that forms the UI for this DateTimeControl
         */
        final DatepickerCombo datepickerCombo =
            new DatepickerCombo(parent, SWT.BORDER, DateFormat.getDateInstance(DateFormat.SHORT));
        datepickerCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, columnsToTake, 1));

        if (!isFormReadonly()) {
            /*
             * add a modify listener to capture changes made by the user and
             * push them into the work item
             */
            final DateTimeControlUpdateModifyListener modifyListener =
                new DateTimeControlUpdateModifyListener(workItemField, datepickerCombo);
            datepickerCombo.addModifyListener(modifyListener);
            datepickerCombo.setData(
                DateTimeControlUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY,
                modifyListener);
            datepickerCombo.getTextControl().setData(
                DateTimeControlUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY,
                modifyListener);
        } else {
            /*
             * this DateTimeControl is set readonly on the form, so set the
             * datepicker combo to disabled
             */
            datepickerCombo.setEnabled(false);
        }

        /*
         * field change listener to modify the UI when the underlying work item
         * field changes
         */
        final DateTimeControlFieldChangeListener dateTimeControlFieldChangeListener =
            new DateTimeControlFieldChangeListener(
                datepickerCombo,
                DateTimeControlUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY);
        workItemField.addFieldChangeListener(dateTimeControlFieldChangeListener);

        /*
         * add a dispose listener so that the field change listener is removed
         * from the work item when this UI is disposed. the work item has a
         * lifecycle independent of the UI.
         */
        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                workItemField.removeFieldChangeListener(dateTimeControlFieldChangeListener);
            }
        });

        /*
         * fire a "fake" field change event to the field change listener this
         * sets up the initial picklist, decoration, etc for this field control
         */
        final FieldChangeEvent fieldChangeEvent = new FieldChangeEvent();
        fieldChangeEvent.field = workItemField;
        dateTimeControlFieldChangeListener.fieldChanged(fieldChangeEvent);

        /*
         * focus listener for <Required> decorations
         */
        final RequiredDecorationFocusListener fieldControlFocusListener = new RequiredDecorationFocusListener(
            workItemField,
            DateTimeControlUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY);
        datepickerCombo.getTextControl().addFocusListener(fieldControlFocusListener);

        /*
         * fire a "fake" focus lost event to the focus listener this sets up the
         * initial decoration
         */
        final Event e = new Event();
        e.widget = datepickerCombo.getTextControl();
        fieldControlFocusListener.focusLost(new FocusEvent(e));

        /*
         * register with the field tracker
         */
        getFieldTracker().addField(workItemField);
        getFieldTracker().setFocusReceiver(workItemField, new FieldTracker.FocusReceiver() {
            @Override
            public boolean setFocus() {
                return datepickerCombo.setFocus();
            }
        });
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }
}
