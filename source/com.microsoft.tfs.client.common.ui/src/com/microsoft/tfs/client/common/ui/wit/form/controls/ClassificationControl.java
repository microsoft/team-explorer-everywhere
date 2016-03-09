// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.wit.controls.ClassificationCombo;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;

public class ClassificationControl extends LabelableControl {
    private String fieldName;

    @Override
    protected void hookInit() {
        fieldName = getControlDescription().getFieldName();
    }

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
         * create the ClassificationCombo widget
         */
        final ClassificationCombo classificationCombo = new ClassificationCombo(parent, SWT.NONE);
        classificationCombo.setProject(getWorkItem().getType().getProject());
        classificationCombo.setTreeType(fieldName);
        classificationCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, columnsToTake, 1));

        /*
         * add a modify listener to the combo control to push changes back into
         * the work item
         */
        final FieldUpdateModifyListener fieldUpdateModifyListener = new FieldUpdateModifyListener(workItemField);
        classificationCombo.addModifyListener(fieldUpdateModifyListener);
        classificationCombo.setData(
            FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY,
            fieldUpdateModifyListener);

        /*
         * field change listener to modify the work item UI when the underlying
         * field changes
         */
        final ClassificationControlFieldChangeListener classificationControlFieldChangeListener =
            new ClassificationControlFieldChangeListener(
                classificationCombo,
                FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY);
        workItemField.addFieldChangeListener(classificationControlFieldChangeListener);

        /*
         * add a dispose listener so that the field change listener is removed
         * from the work item when this UI is disposed. the work item has a
         * lifecycle independent of the UI.
         */
        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                workItemField.removeFieldChangeListener(classificationControlFieldChangeListener);
            }
        });

        /*
         * fire a "fake" field change event to the field change listener this
         * sets up the initial value, decoration, etc
         */
        final FieldChangeEvent fieldChangeEvent = new FieldChangeEvent();
        fieldChangeEvent.field = workItemField;
        classificationControlFieldChangeListener.fieldChanged(fieldChangeEvent);

        /*
         * focus listener for <Required> decorations
         */
        final RequiredDecorationFocusListener classificationControlFocusListener = new RequiredDecorationFocusListener(
            workItemField,
            FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY);
        classificationCombo.addFocusListener(classificationControlFocusListener);

        /*
         * register with the field tracker
         */
        getFieldTracker().addField(workItemField);
        getFieldTracker().setFocusReceiver(workItemField, new FieldTracker.FocusReceiver() {
            @Override
            public boolean setFocus() {
                return classificationCombo.setFocus();
            }
        });
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }
}
