// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.wit.form.controls.IWorkItemControl;
import com.microsoft.tfs.core.clients.workitem.form.WIFormColumn;
import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLayout;
import com.microsoft.tfs.core.clients.workitem.form.WIFormTab;

/**
 * Models a Layout, Column, or Tab - child controls are stacked vertically.
 */
public class VerticalStackControl extends Composite {
    private final WIFormElement formElement;
    private final FormContext formContext;

    private IWorkItemControl[] controls;

    public VerticalStackControl(
        final Composite parent,
        final int style,
        final WIFormElement formElement,
        final FormContext formContext) {
        super(parent, style);

        this.formElement = formElement;
        this.formContext = formContext;

        formContext.getDebuggingContext().debug(this, formElement);
        formContext.getWorkItemEditorContextMenu().setMenuOnControl(this);
        populate();
    }

    public boolean wantsVerticalFill() {
        for (int i = 0; i < controls.length; i++) {
            if (controls[i].wantsVerticalFill()) {
                return true;
            }
        }
        return false;
    }

    private void populate() {
        final WIFormElement[] childElements = formElement.getChildElements();

        controls = Helpers.getControls(childElements, formContext);

        final int numColumns = Helpers.getMinimalNumberOfColumnsRequired(controls);
        final GridLayout layout = new GridLayout(numColumns == 0 ? 1 : numColumns, false);

        final int margin = (shouldUseMargin() ? 5 : 0);
        layout.marginHeight = margin;
        layout.marginWidth = margin;
        formContext.getDebuggingContext().setupGridLayout(layout);

        setLayout(layout);

        // Bug 2283: If we have no child controls in a column then we render the
        // columns composite higher than Visual Studio would do in this
        // instance. Therefore fill the column with a single pixel invisible
        // label instead.
        if (controls.length == 0 && formElement instanceof WIFormColumn) {
            final Label label = new Label(this, SWT.NONE);
            label.setVisible(false);
            final GridData gd = new GridData();
            gd.widthHint = 1;
            gd.heightHint = 1;
            label.setLayoutData(gd);
        }
        for (int i = 0; i < controls.length; i++) {
            controls[i].addToComposite(this);
        }

    }

    private boolean shouldUseMargin() {
        return (formElement instanceof WIFormLayout) || (formElement instanceof WIFormTab);
    }
}
