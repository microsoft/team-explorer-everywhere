// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.widgets.Composite;

public class SplitterControl extends BaseWorkItemControl {
    @Override
    public void addToComposite(final Composite parent) {
        /*
         * Sash sash = new Sash(parent, SWT.VERTICAL);
         */
    }

    @Override
    public int getMinimumRequiredColumnCount() {
        return 1;
    }
}
