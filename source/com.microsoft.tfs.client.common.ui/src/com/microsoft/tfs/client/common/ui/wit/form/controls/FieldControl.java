// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.controls.generic.AutocompleteCombo;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;

public class FieldControl extends LabelableControl {
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
        /*
         * create a new composite with a stack layout to hold the text and combo
         * controls
         */
        final Composite fieldControlComposite = new Composite(parent, SWT.NONE);
        getDebuggingContext().debug(fieldControlComposite, getFormElement());
        final StackLayout stackLayout = new StackLayout();
        getDebuggingContext().setupStackLayout(stackLayout);
        fieldControlComposite.setLayout(stackLayout);

        /*
         * create the text and combo controls
         */
        final Text textControl = new Text(fieldControlComposite, SWT.BORDER);
        final AutocompleteCombo comboControl = new AutocompleteCombo(fieldControlComposite, SWT.DROP_DOWN);

        /*
         * set layout data on field control composite
         */
        fieldControlComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numColumns, 1));

        /*
         * nothing else to do if there is no field associated with this field
         * control
         */
        if (getControlDescription().getFieldName() == null) {
            stackLayout.topControl = textControl;
            AutomationIDHelper.setWidgetID(textControl, "#fieldControl.text"); //$NON-NLS-1$
            AutomationIDHelper.setWidgetID(comboControl, "#fieldControl.combo"); //$NON-NLS-1$
            return;
        }

        /*
         * get the work item field this field control is for
         */
        final Field workItemField = getWorkItem().getFields().getField(getControlDescription().getFieldName());

        final String fieldRefName = workItemField.getReferenceName().toLowerCase(Locale.ENGLISH);
        AutomationIDHelper.setWidgetID(textControl, fieldRefName + "#fieldControl.text"); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(comboControl, fieldRefName + "#fieldControl.combo"); //$NON-NLS-1$

        if (!isFormReadonly()) {
            /*
             * add a modify listener to capture changes made by the user and
             * push them into the work item
             */
            final FieldUpdateModifyListener fieldUpdateModifyListener = new FieldUpdateModifyListener(workItemField);
            textControl.addModifyListener(fieldUpdateModifyListener);
            comboControl.addModifyListener(fieldUpdateModifyListener);
            textControl.setData(FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY, fieldUpdateModifyListener);
            comboControl.setData(FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY, fieldUpdateModifyListener);
        }

        /*
         * field change listener to modify the UI when the underlying work item
         * field changes
         */
        final FieldControlFieldChangeListener fieldControlFieldChangeListener = new FieldControlFieldChangeListener(
            stackLayout,
            textControl,
            comboControl,
            FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY,
            isFormReadonly());
        workItemField.addFieldChangeListener(fieldControlFieldChangeListener);

        /*
         * add a dispose listener so that the field change listener is removed
         * from the work item when this UI is disposed. the work item has a
         * lifecycle independent of the UI.
         */
        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                workItemField.removeFieldChangeListener(fieldControlFieldChangeListener);
            }
        });

        /*
         * fire a "fake" field change event to the field change listener this
         * sets up the initial picklist, decoration, etc for this field control
         */
        final FieldChangeEvent fieldChangeEvent = new FieldChangeEvent();
        fieldChangeEvent.field = workItemField;
        fieldControlFieldChangeListener.fieldChanged(fieldChangeEvent);

        /*
         * focus listener for <Required> decorations
         */
        final RequiredDecorationFocusListener fieldControlFocusListener = new RequiredDecorationFocusListener(
            workItemField,
            FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY);
        textControl.addFocusListener(fieldControlFocusListener);
        comboControl.addFocusListener(fieldControlFocusListener);

        /*
         * register with the field tracker
         */
        getFieldTracker().addField(workItemField);
        getFieldTracker().setFocusReceiver(workItemField, new FieldTracker.FocusReceiver() {
            @Override
            public boolean setFocus() {
                return stackLayout.topControl.setFocus();
            }
        });

        /*
         * OSX has a bug such that StackLayouts must be explicitly layed out at
         * the first paint. Otherwise, the text in combos is not drawn
         * appropriately. See bug 1388.
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            fieldControlComposite.addPaintListener(new PaintListener() {
                @Override
                public void paintControl(final PaintEvent e) {
                    stackLayout.topControl.pack();
                    fieldControlComposite.layout(true);

                    /* We can remove this listener after the first time */
                    fieldControlComposite.removePaintListener(this);
                }
            });
        }
    }
}
