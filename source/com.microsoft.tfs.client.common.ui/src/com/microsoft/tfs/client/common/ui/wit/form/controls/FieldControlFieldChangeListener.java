// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.core.clients.workitem.WorkItemUtils;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;

/**
 * A work item FieldChangeListener that's used to update the form UI for field
 * controls when a work item field changes.
 */
public class FieldControlFieldChangeListener implements FieldChangeListener {
    private static final Log log = LogFactory.getLog(FieldControlFieldChangeListener.class);

    /*
     * field controls use a stack layout in which a text control and a combo
     * control are stacked
     */
    private final StackLayout stackLayout;

    /*
     * the text control - used when there is no picklist or the field is
     * readonly
     */
    private final Text textControl;

    /*
     * the combo control - used when a picklist exists and the field is editable
     */
    private final Combo comboControl;

    /*
     * the key of a modify listener that is attached to the text and combo
     * control by some other class. this key is used to remove and re-add the
     * modify listener to ensure that this class doesn't trigger it
     */
    private final String modifyListenerWidgetDataKey;

    /*
     * Is the field control marked as readonly in the form definition?
     */
    private final boolean formReadOnly;

    /*
     * The DateFormat used to format field values that are dates.
     *
     * I18N: need to use a specified Locale instead of the default Locale
     */
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    public FieldControlFieldChangeListener(
        final StackLayout stackLayout,
        final Text textControl,
        final Combo comboControl,
        final String modifyListenerWidgetDataKey,
        final boolean formReadOnly) {
        this.stackLayout = stackLayout;
        this.textControl = textControl;
        this.comboControl = comboControl;
        this.modifyListenerWidgetDataKey = modifyListenerWidgetDataKey;
        this.formReadOnly = formReadOnly;
    }

    @Override
    public void fieldChanged(final FieldChangeEvent event) {
        /*
         * Ignore the field changed event if the field was change was caused by
         * this control.
         */
        if (event.source == textControl || event.source == comboControl) {
            return;
        }

        /*
         * Check if we're the UI thread and handle the event directly. This is
         * to support the case when we're being called during WIT form setup by
         * the UI thread and other code expects us to execute synchronously.
         */
        final Display display = textControl.getDisplay();
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

        String messageFormat = "FieldControlFieldChangeListener called for field change on [{0}]"; //$NON-NLS-1$
        String message = MessageFormat.format(messageFormat, Integer.toString(field.getID()));
        log.trace(message);

        /*
         * keep track of whether we need to call layout() on the parent of the
         * text/combo control
         */
        boolean needToLayout = false;

        /*
         * the value currently held in the work item field - hereafter called
         * "new" value to distinguish from the (possibly old) value held in the
         * GUI
         */
        final String newValue = getStringValueFromFieldValue(field.getValue());

        if (!formReadOnly && field.getAllowedValues().size() > 0 && field.isEditable()) {
            /*
             * there is a picklist and the field is editable - therefore we must
             * display the combo box as the top control
             */

            /*
             * remove the modify listener from the combo to avoid triggering it
             * when we call setText()
             */
            final ModifyListener modifyListener = (ModifyListener) comboControl.getData(modifyListenerWidgetDataKey);
            if (modifyListener != null) {
                comboControl.removeModifyListener(modifyListener);
            }

            if (stackLayout.topControl != comboControl) {
                /*
                 * the combo control isn't currently the top control - set it to
                 * the top and record that we need to layout
                 */
                stackLayout.topControl = comboControl;
                needToLayout = true;
            }

            final String[] items = comboControl.getItems();
            final String[] newItems = field.getAllowedValues().getValues();
            if (!Arrays.equals(items, newItems)) {
                /*
                 * the combo control doesn't hold the latest picklist for the
                 * field - set the combo's items to the latest picklist and
                 * record that we need to layout
                 */
                comboControl.setItems(newItems);
                needToLayout = true;
            }

            /*
             * set the visible items in the combo to be either the maximum
             * visible item count, or the total picklist size, whichever is
             * smaller
             */
            ComboHelper.setVisibleItemCount(comboControl);

            final String currentValueFromWidget = RequiredDecorationFocusListener.hasDecoration(comboControl) ? "" //$NON-NLS-1$
                : comboControl.getText().trim();

            if (!newValue.equals(currentValueFromWidget)) {
                /*
                 * the combo control holds a value that is not equal to the
                 * value in the work item field
                 */
                if (comboControl.getData(FieldUpdateModifyListener.MODIFICATION_KEY) == null) {
                    /*
                     * Call setText() with the new value and record that we need
                     * to layout. We only do this when the combo control doesn't
                     * have focus. If the combo control does have focus, it's
                     * likely that the FieldUpdateModifyListener has
                     * auto-corrected the text typed into the combo box to be
                     * the text in one of the items (eg when the typed in case
                     * is different than the case in the list).
                     */
                    if (RequiredDecorationFocusListener.hasDecoration(comboControl)) {
                        RequiredDecorationFocusListener.setHasDecoration(comboControl, false);
                    }
                    comboControl.setText(newValue);
                    needToLayout = true;
                }
            }

            /*
             * set the background color of the combo depending on whether or not
             * the work item field is currently valid
             *
             * note that changing the BG color does not require a layout
             */
            if (field.getStatus() != FieldStatus.VALID) {
                comboControl.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                if (!comboControl.isFocusControl()
                    && comboControl.getData(FieldUpdateModifyListener.MODIFICATION_KEY) == null) {
                    RequiredDecorationFocusListener.addDecoration(field, comboControl, null);
                }
            } else {
                comboControl.setBackground(null);
                if (!comboControl.isFocusControl()
                    && comboControl.getData(FieldUpdateModifyListener.MODIFICATION_KEY) == null) {
                    RequiredDecorationFocusListener.removeDecoration(comboControl, null);
                }
            }

            /*
             * re-add the modify listener we temporarily removed
             */
            if (modifyListener != null) {
                comboControl.addModifyListener(modifyListener);
            }
        } else {
            /*
             * either there is no picklist or the work item field is readonly
             * (either natively or is marked readonly in the form definition) -
             * so we show the text control as the top control
             */

            /*
             * remove the modify listener from the combo to avoid triggering it
             * when we call setText()
             */
            final ModifyListener modifyListener = (ModifyListener) textControl.getData(modifyListenerWidgetDataKey);
            if (modifyListener != null) {
                textControl.removeModifyListener(modifyListener);
            }

            if (stackLayout.topControl != textControl) {
                /*
                 * the text control isn't currently the top control - set it to
                 * the top and record that we need to layout
                 */
                stackLayout.topControl = textControl;
                needToLayout = true;
            }

            if (formReadOnly) {
                if (textControl.getEditable()) {
                    /*
                     * the field control is marked as readonly but the text
                     * control is editable - set the text control's editable
                     * state and record that we need to layout
                     */
                    textControl.setEditable(false);
                    needToLayout = true;
                }
            } else {
                if (field.isEditable() != textControl.getEditable()) {
                    /*
                     * the work item field's editable state and the text
                     * control's editable state don't agree - set the text
                     * control's editable state and record that we need to
                     * layout
                     */
                    textControl.setEditable(field.isEditable());
                    needToLayout = true;
                }
            }

            if (!textControl.isFocusControl()) {
                final String currentValueFromWidget = RequiredDecorationFocusListener.hasDecoration(textControl) ? "" //$NON-NLS-1$
                    : textControl.getText().trim();
                if (!newValue.equals(currentValueFromWidget)) {
                    /*
                     * the text control holds an old value for the field - call
                     * setText() with the new value and record that we need to
                     * layout
                     */
                    if (RequiredDecorationFocusListener.hasDecoration(textControl)) {
                        RequiredDecorationFocusListener.setHasDecoration(textControl, false);
                    }
                    textControl.setText(newValue);
                    needToLayout = true;
                }
            }

            /*
             * set the background color of the text control depending on whether
             * or not the work item field is currently valid
             *
             * note that changing the BG color does not require a layout
             */
            if (field.getStatus() != FieldStatus.VALID) {
                textControl.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                if (!textControl.isFocusControl()
                    && textControl.getData(FieldUpdateModifyListener.MODIFICATION_KEY) == null) {
                    RequiredDecorationFocusListener.addDecoration(field, textControl, null);
                }
            } else {
                textControl.setBackground(null);
                if (!textControl.isFocusControl()
                    && textControl.getData(FieldUpdateModifyListener.MODIFICATION_KEY) == null) {
                    RequiredDecorationFocusListener.removeDecoration(textControl, null);
                }
            }

            /*
             * re-add the modify listener we temporarily removed
             */
            if (modifyListener != null) {
                textControl.addModifyListener(modifyListener);
            }
        }

        if (needToLayout) {
            stackLayout.topControl.pack();
            stackLayout.topControl.getParent().layout(true);

            messageFormat = "FieldControlFieldChangeListener laying out composite for field change [{0}]"; //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, Integer.toString(field.getID()));
            log.trace(message);
        }
    }

    private String getStringValueFromFieldValue(final Object value) {
        if (value == null) {
            return ""; //$NON-NLS-1$
        }

        if (value instanceof Date) {
            if (((Date) value).getTime() == 0) {
                return ""; //$NON-NLS-1$
            }

            return dateFormat.format((Date) value);
        }

        return WorkItemUtils.objectToString(value);
    }
}
