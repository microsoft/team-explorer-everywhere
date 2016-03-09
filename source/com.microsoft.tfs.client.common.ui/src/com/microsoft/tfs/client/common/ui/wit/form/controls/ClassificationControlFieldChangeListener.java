// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.wit.controls.ClassificationCombo;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;

/**
 * A work item FieldChangeListener that's used to update the form UI for
 * classification controls when a work item field changes.
 */
public class ClassificationControlFieldChangeListener implements FieldChangeListener {
    private static final Log log = LogFactory.getLog(FieldControlFieldChangeListener.class);

    /*
     * the classificationcombo we will modify in response to field changes
     */
    private final ClassificationCombo classificationCombo;

    /*
     * the key of a modify listener to temporarily remove when making changes
     */
    private final String modifyListenerWidgetDataKey;

    public ClassificationControlFieldChangeListener(
        final ClassificationCombo classificationCombo,
        final String modifyListenerWidgetDataKey) {
        this.classificationCombo = classificationCombo;
        this.modifyListenerWidgetDataKey = modifyListenerWidgetDataKey;
    }

    @Override
    public void fieldChanged(final FieldChangeEvent event) {
        /*
         * Check if we're the UI thread and handle the event directly. This is
         * to support the case when we're being called during WIT form setup by
         * the UI thread and other code expects us to execute synchronously.
         */
        final Display display = classificationCombo.getDisplay();
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

    private void fieldChangedSafe(final FieldChangeEvent event) {
        /*
         * the work item field that has changed
         */
        final Field field = event.field;

        final String messageFormat = "ClassificationControlFieldChangeListener called for field change on [{0}]"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Integer.toString(field.getID()));
        log.trace(message);

        /*
         * remove the modify listener from the combo to avoid triggering it when
         * we call setText()
         */
        final ModifyListener modifyListener = (ModifyListener) classificationCombo.getData(modifyListenerWidgetDataKey);
        if (modifyListener != null) {
            classificationCombo.removeModifyListener(modifyListener);
        }

        /*
         * the value currently held in the work item field - hereafter called
         * "new" value to distinguish from the (possibly old) value held in the
         * GUI
         */
        final String newValue = (field.getValue() != null ? field.getValue().toString() : ""); //$NON-NLS-1$

        /*
         * if the classification combo doesn't hold the latest value, set it
         */
        String currentValue = classificationCombo.getText().trim();
        // remove trailing path separators
        while (currentValue.endsWith("\\")) //$NON-NLS-1$
        {
            currentValue = currentValue.substring(0, currentValue.length() - 1);
        }
        if (!newValue.equals(currentValue)) {
            /*
             * the only time we don't want to set the control's text from the
             * work item is when the field's status is INVALID_PATH. This
             * indicates that we previously attempted to set the path to a bad
             * value. In this case, the value in the work item field is not
             * changed, only the status is modified. In this special case, we
             * leave the text in the control as-is (otherwise the invalid path
             * the user typed into the control would be wiped out).
             */
            if (field.getStatus() != FieldStatus.INVALID_PATH) {
                classificationCombo.setText(newValue);
            }
        }

        /*
         * set the background color of the text control depending on whether or
         * not the work item field is currently valid
         */
        if (field.getStatus() != FieldStatus.VALID) {
            classificationCombo.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        } else {
            classificationCombo.setBackground(null);
        }

        /*
         * re-add the modify listener we temporarily removed
         */
        if (modifyListener != null) {
            classificationCombo.addModifyListener(modifyListener);
        }
    }
}
