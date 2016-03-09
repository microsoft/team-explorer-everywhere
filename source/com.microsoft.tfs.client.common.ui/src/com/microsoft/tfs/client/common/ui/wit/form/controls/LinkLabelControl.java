// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.widgets.Composite;

public class LinkLabelControl extends LabelableControl {
    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        // There's no associated control with a label control.
    }

    @Override
    protected int getControlColumns() {
        return 0;
    }

    @Override
    protected boolean isLabelOnly() {
        return true;
    }
}
