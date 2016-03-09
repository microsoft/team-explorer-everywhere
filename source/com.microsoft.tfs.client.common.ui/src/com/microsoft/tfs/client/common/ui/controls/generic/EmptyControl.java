// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

/**
 * This represents an empty control (ie, 0 pixels by 0 pixels). This is useful
 * for falling back on legacy platforms when modern controls are unavailable.
 *
 * Note that you may still have layout problems (ie, a 0x0 control will still
 * have spacing around it) so it is recommended only for use as a fallback.
 */
public class EmptyControl extends Composite {
    public EmptyControl(final Composite parent, final int style) {
        super(parent, SWT.NONE);
        setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
    }

    @Override
    public Point computeSize(final int xHint, final int yHint, final boolean changed) {
        return new Point(0, 0);
    }
}
