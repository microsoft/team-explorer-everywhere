// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.wizard;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public abstract class BaseWizardPage extends WizardPage {
    private boolean initializedMetrics = false;

    private int horizontalSpacing = 0;
    private int verticalSpacing = 0;

    private int spacing = 0;

    private int horizontalMargin = 0;
    private int verticalMargin = 0;

    protected BaseWizardPage(final String pageName) {
        this(pageName, null, (ImageDescriptor) null);
    }

    protected BaseWizardPage(final String pageName, final String title, final ImageDescriptor titleImage) {
        super(pageName, title, titleImage);

        /*
         * Note: cannot compute metrics here since wizard pages may be
         * instantiated before the wizard has built any graphical elements, and
         * we need some control or another to make a GC.
         */
    }

    private final void computeMetrics() {
        Control control = getControl();

        if (control == null && Display.getCurrent() != null) {
            control = Display.getCurrent().getActiveShell();
        }

        if (control == null) {
            return;
        }

        /* Compute metrics in pixels */
        final GC gc = new GC(control);
        final FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();

        horizontalSpacing = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_SPACING);
        verticalSpacing = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_SPACING);

        spacing = Math.max(horizontalSpacing, verticalSpacing);

        horizontalMargin = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_MARGIN);
        verticalMargin = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_MARGIN);

        initializedMetrics = true;
    }

    public int getHorizontalSpacing() {
        if (initializedMetrics == false) {
            computeMetrics();
        }

        return horizontalSpacing;
    }

    public int getVerticalSpacing() {
        if (initializedMetrics == false) {
            computeMetrics();
        }

        return verticalSpacing;
    }

    public int getSpacing() {
        if (initializedMetrics == false) {
            computeMetrics();
        }

        return spacing;
    }

    public int getHorizontalMargin() {
        if (initializedMetrics == false) {
            computeMetrics();
        }

        return horizontalMargin;
    }

    public int getVerticalMargin() {
        if (initializedMetrics == false) {
            computeMetrics();
        }

        return verticalMargin;
    }
}
