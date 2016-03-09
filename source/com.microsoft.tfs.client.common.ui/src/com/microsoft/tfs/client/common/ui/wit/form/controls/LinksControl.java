// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemLinksControl;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlOptions;
import com.microsoft.tfs.core.clients.workitem.internal.form.WIFormControlImpl;

public class LinksControl extends LabelableControl {
    @Override
    public boolean wantsVerticalFill() {
        return true;
    }

    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        final WIFormControlImpl control = (WIFormControlImpl) getFormElement();
        final WIFormLinksControlOptions options = control.getLinksControlOptions();
        final WorkItemLinksControl wilc =
            new WorkItemLinksControl(parent, SWT.NONE, getServer(), getWorkItem(), options);
        wilc.init();

        wilc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, columnsToTake, 1));
        ControlSize.setCharHeightHint(wilc, 12);
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }
}
