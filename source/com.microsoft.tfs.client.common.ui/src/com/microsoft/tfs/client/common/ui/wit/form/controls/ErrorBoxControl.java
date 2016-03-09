// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class ErrorBoxControl extends LabelableControl {
    private final String errorMessage;

    public ErrorBoxControl(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        final Text text = new Text(parent, SWT.BORDER);
        text.setEditable(false);
        text.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
        text.setText(errorMessage);

        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, columnsToTake, 1));
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }
}
