// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.form;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class OptionButton {
    public static interface ValueChangedListener {
        public void onValueChanged(boolean value, OptionButton optionButton);
    }

    private final Button button;
    private final SingleListenerFacade valueChangedListener = new SingleListenerFacade(ValueChangedListener.class);
    private boolean value;

    public OptionButton(final Composite parent, final String text, final String tooltip) {
        final Label label = new Label(parent, SWT.NONE);
        label.setVisible(false);
        GridDataBuilder.newInstance().hSpan(2).applyTo(label);

        button = new Button(parent, SWT.CHECK);
        button.setText(text);
        button.setToolTipText(tooltip);

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                value = ((Button) e.widget).getSelection();
                fireValueChangedListeners();
            }
        });
    }

    public void addValueChangedListener(final ValueChangedListener listener) {
        valueChangedListener.addListener(listener);
    }

    public void removeValueChangedListener(final ValueChangedListener listener) {
        valueChangedListener.removeListener(listener);
    }

    public void setEnabled(final boolean enabled) {
        button.setEnabled(enabled);
    }

    public void setValue(final boolean value) {
        this.value = value;
        button.setSelection(value);
        fireValueChangedListeners();
    }

    public boolean getValue() {
        return value;
    }

    private void fireValueChangedListeners() {
        final ValueChangedListener listener = (ValueChangedListener) valueChangedListener.getListener();
        listener.onValueChanged(value, this);
    }
}
