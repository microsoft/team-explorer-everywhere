// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls;

import org.eclipse.swt.widgets.Control;

/**
 * A page in the tool strip tab items.
 */
public interface ToolStripTabPage {
    public boolean isValid();

    public String getName();

    public Control createControl(org.eclipse.swt.widgets.Composite parent);
}
