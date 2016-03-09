// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLabelPositionEnum;

public class PlainTextControl extends LabelableControl {
    private static final Log log = LogFactory.getLog(PlainTextControl.class);

    private Font font;

    @Override
    public boolean wantsVerticalFill() {
        final WIFormLabelPositionEnum labelPosition = getControlDescription().getLabelPosition();
        if ((WIFormLabelPositionEnum.LEFT == labelPosition) || (WIFormLabelPositionEnum.RIGHT == labelPosition)) {
            return false;
        }

        return isFormElementLastAmongSiblings();
    }

    @Override
    protected Field getFieldForToolTipSupport() {
        final String fieldName = getControlDescription().getFieldName();

        if (fieldName == null) {
            return null;
        }

        return getWorkItem().getFields().getField(fieldName);
    }

    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        /*
         * Some controls have no field backing them.
         */
        final String fieldName = getControlDescription().getFieldName();
        final Field field = (fieldName != null) ? getWorkItem().getFields().getField(fieldName) : null;

        initializeFont(parent.getDisplay());

        if (font != null) {
            parent.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(final DisposeEvent e) {
                    font.dispose();
                }
            });
        }

        final Text text = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);

        if (font != null) {
            text.setFont(font);
        }

        if (field == null || isFormReadonly()) {
            text.setEditable(false);
        }

        final boolean wantsVerticalFill = wantsVerticalFill();
        text.setLayoutData(new GridData(
            SWT.FILL,
            wantsVerticalFill ? SWT.FILL : SWT.CENTER,
            true,
            wantsVerticalFill,
            columnsToTake,
            1));

        ControlSize.setCharHeightHint(text, 3);
        ControlSize.setCharWidthHint(text, 1);

        if (field != null) {
            if (!isFormReadonly()) {
                final FieldUpdateModifyListener fieldUpdateModifyListener = new FieldUpdateModifyListener(field);
                text.addModifyListener(fieldUpdateModifyListener);
                text.setData(FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY, fieldUpdateModifyListener);
            }

            final FieldChangeListener fieldChangeListener = new FieldChangeListener() {
                @Override
                public void fieldChanged(final FieldChangeEvent event) {
                    final Display display = text.getDisplay();
                    if (display.getThread() == Thread.currentThread()) {
                        fieldChangedSafe(event, text);
                        return;
                    }

                    UIHelpers.runOnUIThread(display, true, new Runnable() {
                        @Override
                        public void run() {
                            fieldChangedSafe(event, text);
                        }
                    });
                }
            };

            field.addFieldChangeListener(fieldChangeListener);

            parent.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(final DisposeEvent e) {
                    field.removeFieldChangeListener(fieldChangeListener);
                }
            });

            /*
             * fire a "fake" field change event to the field change listener
             * this sets up the text for this control
             */
            final FieldChangeEvent fieldChangeEvent = new FieldChangeEvent();
            fieldChangeEvent.field = field;
            fieldChangeListener.fieldChanged(fieldChangeEvent);

            getFieldTracker().addField(field);
            getFieldTracker().setFocusReceiver(field, new FieldTracker.FocusReceiver() {
                @Override
                public boolean setFocus() {
                    return text.setFocus();
                }
            });
        }
    }

    private void fieldChangedSafe(final FieldChangeEvent event, final Text textControl) {
        final Field field = event.field;

        if (textControl.getData(FieldUpdateModifyListener.MODIFICATION_KEY) != null) {
            if (log.isTraceEnabled()) {
                final String messageFormat =
                    "PlainTextControl FieldChangeListener called for field change on [{0}]: skipping since field is being modified by UI"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, Integer.toString(field.getID()));
                log.trace(message);
            }
            return;
        }

        if (log.isTraceEnabled()) {
            final String messageFormat = "PlainTextControl FieldChangeListener called for field change on [{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(field.getID()));
            log.trace(message);
        }

        final ModifyListener modifyListener =
            (ModifyListener) textControl.getData(FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY);

        if (modifyListener != null) {
            textControl.removeModifyListener(modifyListener);
        }

        try {
            textControl.setText(getFieldDataAsString(getControlDescription().getFieldName()));
        } finally {
            if (modifyListener != null) {
                textControl.addModifyListener(modifyListener);
            }
        }
    }

    private void initializeFont(final Display display) {
        final FontData[] tahomaFontData = display.getFontList("Tahoma", true); //$NON-NLS-1$
        if (tahomaFontData != null && tahomaFontData.length > 0) {
            font = new Font(display, "Tahoma", 10, SWT.NORMAL); //$NON-NLS-1$
        }
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }
}
