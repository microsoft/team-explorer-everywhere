// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

/**
 * AccessibilityLabel provides an accessible (copyable) read-only text display
 * for the UI. This is typically implemented using a backing read-only text
 * control, however may be implemented differently depending on the platform.
 */
public class AccessibilityLabel extends Composite {
    private final Text textWidget;

    public AccessibilityLabel(final Composite parent, int style) {
        super(parent, SWT.NONE);

        style = checkStyle(style);

        final FillLayout layout = new FillLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.spacing = 0;
        setLayout(layout);

        textWidget = new Text(this, style | SWT.READ_ONLY);
        textWidget.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    }

    private int checkStyle(final int style) {
        return (style & SWT.WRAP);
    }

    public void setText(final String text) {
        textWidget.setText(text);
    }

    public String getText() {
        return textWidget.getText();
    }

    public void setAutomationID(final String id) {
        AutomationIDHelper.setWidgetID(textWidget, id);
    }
}
