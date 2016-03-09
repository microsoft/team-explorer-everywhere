// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.text.MessageFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;

import com.microsoft.tfs.client.common.ui.controls.generic.DatepickerCombo;
import com.microsoft.tfs.core.clients.workitem.fields.Field;

public class DateTimeControlUpdateModifyListener implements ModifyListener {
    private static final Log log = LogFactory.getLog(DateTimeControlUpdateModifyListener.class);

    public static final String MODIFY_LISTENER_WIDGET_DATA_KEY = "wit-modify-listener"; //$NON-NLS-1$

    private final Field workItemField;
    private final DatepickerCombo datepickerCombo;

    public DateTimeControlUpdateModifyListener(final Field workItemField, final DatepickerCombo datepickerCombo) {
        this.workItemField = workItemField;
        this.datepickerCombo = datepickerCombo;
    }

    @Override
    public void modifyText(final ModifyEvent e) {
        if (datepickerCombo.isValid()) {
            /*
             * Bit of a hack here. The DatepickerCombo sets the text control
             * internally when a date is selected, which leaves the
             * RequiredDecorationFocusListener's flag on for that field. If we
             * don't clear that flag here, the valid date text will get
             * overwritten by the empty string when code in
             * DateTimeControlFieldChangeListener removes the required
             * decoration.
             */
            if (RequiredDecorationFocusListener.hasDecoration(datepickerCombo.getTextControl())) {
                RequiredDecorationFocusListener.setHasDecoration(datepickerCombo.getTextControl(), false);
            }

            final Date date = datepickerCombo.getDate();

            if (log.isDebugEnabled()) {
                final String messageFormat = "DateTimeControl modify listener, date: [{0}]"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, date);
                log.debug(message);
            }

            workItemField.setValue(datepickerCombo, date);
        } else {
            final String text = datepickerCombo.getText();

            if (log.isDebugEnabled()) {
                final String messageFormat = "DateTimeControl modify listener (invalid), text: [{0}]"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, text);
                log.debug(message);
            }

            workItemField.setValue(datepickerCombo, text);
        }
    }
}
