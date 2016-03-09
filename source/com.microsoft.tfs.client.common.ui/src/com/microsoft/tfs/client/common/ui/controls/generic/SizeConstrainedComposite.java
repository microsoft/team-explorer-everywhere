// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;

/**
 * This class exists to provide simple size constraints (in either x or y
 * directions) for a Composite. This may be useful when using {@link Text}
 * and/or {@link Label} fields, which resist wrapping whenever possible (and
 * will consequently grow horizontally only bounded by the {@link Display}).
 * This may be particular useful when used with {@link WizardDialog}s, which
 * will allow embedded controls to size themselves based on default layout.
 */
public class SizeConstrainedComposite extends Composite {
    /**
     * Used to determine that the size should not grow beyond its current size.
     */
    public static final int STATIC = -2;

    private int defaultWidthHint = SWT.DEFAULT;
    private int defaultHeightHint = SWT.DEFAULT;

    public SizeConstrainedComposite(final Composite parent, final int style) {
        super(parent, style);
    }

    public void setDefaultSize(final int defaultWidthHint, final int defaultHeightHint) {
        this.defaultWidthHint = defaultWidthHint;
        this.defaultHeightHint = defaultHeightHint;
    }

    @Override
    public Point computeSize(int wHint, int hHint, final boolean changed) {
        if (wHint == SWT.DEFAULT) {
            wHint = getWidthHint();
        }

        if (hHint == SWT.DEFAULT) {
            hHint = getHeightHint();
        }

        final Point p = super.computeSize(wHint, hHint, changed);

        return p;
    }

    private int getWidthHint() {
        if (defaultWidthHint == SWT.DEFAULT) {
            return SWT.DEFAULT;
        }

        if (defaultWidthHint == STATIC) {
            return getSize().x;
        }

        /* Determine any layout margins that may be in play */
        return defaultWidthHint - getLayoutMargin().x;
    }

    private int getHeightHint() {
        if (defaultHeightHint == SWT.DEFAULT) {
            return SWT.DEFAULT;
        }

        if (defaultHeightHint == STATIC) {
            return getSize().y;
        }

        /* Determine any layout margins that may be in play */
        return defaultHeightHint - getLayoutMargin().y;
    }

    private Point getLayoutMargin() {
        final Layout layout = getLayout();

        if (layout == null) {
            return new Point(0, 0);
        } else if (layout instanceof GridLayout) {
            final GridLayout gridLayout = (GridLayout) layout;
            return new Point(gridLayout.marginWidth, gridLayout.marginHeight);
        } else if (layout instanceof FillLayout) {
            final FillLayout fillLayout = (FillLayout) layout;
            return new Point(fillLayout.marginWidth, fillLayout.marginHeight);
        } else if (layout instanceof FormLayout) {
            final FormLayout formLayout = (FormLayout) layout;
            return new Point(formLayout.marginWidth, formLayout.marginHeight);
        }

        return new Point(0, 0);
    }
}
