// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.form;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.validation.ErrorLabel;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;
import com.microsoft.tfs.util.valid.Severity;
import com.microsoft.tfs.util.valid.ValidatorBinding;

public class TextField {
    public static interface ValueChangedListener {
        public void onValueChanged(String value, TextField textField);
    }

    private final String label;
    private final ErrorLabel errorLabel;
    private final Label fieldLabel;
    private final Text field;
    private final SingleListenerFacade valueChangedListener = new SingleListenerFacade(ValueChangedListener.class);

    private boolean ignoreTextModifyEvents = false;
    private String value;

    public TextField(final Composite parent, final int textStyle, final String label, final String tooltip) {
        errorLabel = new ErrorLabel(parent, SWT.NONE);

        fieldLabel = new Label(parent, SWT.NONE);
        GridDataBuilder.newInstance().hAlign(SWT.RIGHT).applyTo(fieldLabel);

        final String messageFormat = Messages.getString("TextField.FieldLabelFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, label);
        fieldLabel.setText(message);
        fieldLabel.setToolTipText(tooltip);

        field = new Text(parent, textStyle | SWT.BORDER);
        final int wHint = ControlSize.convertCharWidthToPixels(field, 80);
        GridDataBuilder.newInstance().hGrab().hFill().wHint(wHint).applyTo(field);

        this.label = label;

        field.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                if (ignoreTextModifyEvents) {
                    return;
                }

                value = ((Text) e.widget).getText().trim();
                if (value.length() == 0) {
                    value = null;
                }

                fireValueChangedListeners();
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                ((Text) e.widget).selectAll();
            }
        });

        clearErrorState();
    }

    public String getLabel() {
        return label;
    }

    public void addValueChangedListener(final ValueChangedListener listener) {
        valueChangedListener.addListener(listener);
    }

    public void removeValueChangedListener(final ValueChangedListener listener) {
        valueChangedListener.removeListener(listener);
    }

    public ValidatorBinding getValidatorBinding() {
        return errorLabel.getValidatorBinding();
    }

    public void setFocus() {
        field.setFocus();
    }

    public void setEnabled(final boolean enabled) {
        fieldLabel.setEnabled(enabled);
        field.setEnabled(enabled);
    }

    public void setValue(String value) {
        if (value != null && value.trim().length() == 0) {
            value = null;
        }

        this.value = value;

        ignoreTextModifyEvents = true;
        if (value != null) {
            field.setText(value);
        } else {
            field.setText(""); //$NON-NLS-1$
        }
        ignoreTextModifyEvents = false;

        fireValueChangedListeners();
    }

    public void clearValue() {
        value = null;

        ignoreTextModifyEvents = true;
        field.setText(""); //$NON-NLS-1$
        ignoreTextModifyEvents = false;

        fireValueChangedListeners();
    }

    public boolean hasValue() {
        return value != null;
    }

    public String getValue() {
        return value;
    }

    public void clearErrorState() {
        errorLabel.clearErrorState();
    }

    public void setErrorState(final Severity severity, final String message) {
        errorLabel.setErrorState(severity, message);
    }

    private void fireValueChangedListeners() {
        final ValueChangedListener listener = (ValueChangedListener) valueChangedListener.getListener();
        listener.onValueChanged(value, this);
    }
}
