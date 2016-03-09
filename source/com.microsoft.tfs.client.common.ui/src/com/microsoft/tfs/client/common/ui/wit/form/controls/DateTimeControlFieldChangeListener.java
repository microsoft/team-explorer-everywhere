// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.text.MessageFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.controls.generic.DatepickerCombo;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;

/**
 * A work item FieldChangeListener that's used to update the form UI for
 * DateTimeControls when a work item field changes.
 */
public class DateTimeControlFieldChangeListener implements FieldChangeListener {
    private static final Log log = LogFactory.getLog(DateTimeControlFieldChangeListener.class);

    /*
     * the DatepickerCombo we will modify in response to field changes
     */
    private final DatepickerCombo datepickerCombo;

    /*
     * the key of a modify listener to temporarily remove when making changes
     */
    private final String modifyListenerWidgetDataKey;

    public DateTimeControlFieldChangeListener(
        final DatepickerCombo datepickerCombo,
        final String modifyListenerWidgetDataKey) {
        this.datepickerCombo = datepickerCombo;
        this.modifyListenerWidgetDataKey = modifyListenerWidgetDataKey;
    }

    @Override
    public void fieldChanged(final FieldChangeEvent event) {
        /*
         * Ignore the field changed event if the field was change was caused by
         * this control.
         */
        if (event.source == datepickerCombo) {
            return;
        }

        /*
         * Check if we're the UI thread and handle the event directly. This is
         * to support the case when we're being called during WIT form setup by
         * the UI thread and other code expects us to execute synchronously.
         */
        final Display display = datepickerCombo.getDisplay();
        if (display.getThread() == Thread.currentThread()) {
            fieldChangedSafe(event);
            return;
        }

        UIHelpers.runOnUIThread(display, true, new Runnable() {
            @Override
            public void run() {
                fieldChangedSafe(event);
            }
        });
    }

    public void fieldChangedSafe(final FieldChangeEvent event) {
        /*
         * the work item field that has changed
         */
        final Field field = event.field;

        final String messageFormat = "DateTimeControlFieldChangeListener called for field change on [{0}]"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Integer.toString(field.getID()));
        log.trace(message);

        /*
         * remove the modify listener from the combo to avoid triggering it when
         * we call modify the UI
         */
        final ModifyListener modifyListener = (ModifyListener) datepickerCombo.getData(modifyListenerWidgetDataKey);
        if (modifyListener != null) {
            datepickerCombo.removeModifyListener(modifyListener);
        }

        if (!datepickerCombo.getTextControl().isFocusControl()) {
            if (field.getStatus() != FieldStatus.INVALID_TYPE && field.getStatus() != FieldStatus.INVALID_DATE) {
                final Date newValue = (Date) field.getValue();

                boolean needToSet = true;

                if (datepickerCombo.isValid()) {
                    final Date currentValue = datepickerCombo.getDate();
                    if (newValue == null) {
                        needToSet = (currentValue != null);
                    } else {
                        needToSet = !(newValue.equals(currentValue));
                    }
                }

                if (needToSet) {
                    datepickerCombo.setDate(newValue);
                }
            }
        }

        /*
         * set the background color of the text control depending on whether or
         * not the work item field is currently valid
         */
        if (field.getStatus() != FieldStatus.VALID) {
            datepickerCombo.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
            if (!datepickerCombo.getTextControl().isFocusControl()) {
                RequiredDecorationFocusListener.addDecoration(field, datepickerCombo.getTextControl(), null);
            }
        } else {
            datepickerCombo.setBackground(null);
            if (!datepickerCombo.getTextControl().isFocusControl()) {
                RequiredDecorationFocusListener.removeDecoration(datepickerCombo.getTextControl(), null);
            }
        }

        /*
         * re-add the modify listener we temporarily removed
         */
        if (modifyListener != null) {
            datepickerCombo.addModifyListener(modifyListener);
        }
    }
}
