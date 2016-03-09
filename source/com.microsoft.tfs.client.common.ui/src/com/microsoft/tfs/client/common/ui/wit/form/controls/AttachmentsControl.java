// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.controls.wit.FileAttachmentsControl;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;

public class AttachmentsControl extends LabelableControl {
    @Override
    public boolean wantsVerticalFill() {
        return true;
    }

    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        final FileAttachmentsControl fac = new FileAttachmentsControl(parent, SWT.NONE, getServer(), getWorkItem());
        fac.init();

        fac.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, columnsToTake, 1));
        ControlSize.setCharHeightHint(fac, 12);
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }
}
