// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.form.WIFormControl;

public class NullWorkItemControl extends BaseWorkItemControl {
    private WIFormControl controlDescription;

    @Override
    protected void hookInit() {
        controlDescription = (WIFormControl) getFormElement();
    }

    @Override
    public int getMinimumRequiredColumnCount() {
        return 1;
    }

    @Override
    public void addToComposite(final Composite parent) {
        final Label label = new Label(parent, SWT.NONE);

        final String messageFormat = Messages.getString("NullWorkItemControl.LabelTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, controlDescription.getLabel());
        label.setText(message);

        final int numColumns = ((GridLayout) parent.getLayout()).numColumns;
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, numColumns, 1));
    }
}
