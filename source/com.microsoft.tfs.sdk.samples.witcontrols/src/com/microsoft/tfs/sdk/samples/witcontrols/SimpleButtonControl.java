// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.witcontrols;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.wit.form.FormContext;
import com.microsoft.tfs.client.common.ui.wit.form.controls.IWorkItemControl;
import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;

/**
 * Simple custom control that displays a button and raises a message box that
 * displays the current work item id when clicked
 */
public class SimpleButtonControl implements IWorkItemControl {
    private FormContext formContext;

    @Override
    public final void init(final WIFormElement formElement, final FormContext formContext) {
        this.formContext = formContext;
    }

    @Override
    public int getMinimumRequiredColumnCount() {
        return 1;
    }

    @Override
    public boolean wantsVerticalFill() {
        return false;
    }

    @Override
    public void addToComposite(final Composite parent) {
        final Button button = new Button(parent, SWT.NONE);
        button.setText("Click Me!"); //$NON-NLS-1$
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final String messageTitle = "Message from your work item"; //$NON-NLS-1$
                final String message = "Hello, I am work item #" + formContext.getWorkItem().getID(); //$NON-NLS-1$
                MessageDialog.openInformation(parent.getShell(), messageTitle, message);
            }
        });
    }
}
