// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;

public abstract class BasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    public static final CodeMarker CODEMARKER_VISIBLE_TRUE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.prefs.preferencePage#setVisibleTrue"); //$NON-NLS-1$

    public static final CodeMarker CODEMARKER_VISIBLE_FALSE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.prefs.preferencePage#setVisibleFalse"); //$NON-NLS-1$

    private int horizontalSpacing = 0;
    private int verticalSpacing = 0;

    private int spacing = 0;

    private int horizontalMargin = 0;
    private int verticalMargin = 0;

    public BasePreferencePage() {
        super();
    }

    public BasePreferencePage(final String title) {
        super(title);
    }

    public BasePreferencePage(final String title, final ImageDescriptor image) {
        super(title, image);
    }

    private void computeMetrics() {
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
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
     * .swt.widgets.Composite)
     */
    @Override
    abstract protected Control createContents(Composite parent);

    // abstract protected void initializeDefaults();

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(getPreferenceStore());
    }

    /*
     * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
     */
    @Override
    public IPreferenceStore getPreferenceStore() {
        return TFSCommonUIClientPlugin.getDefault().getPreferenceStore();
    }

    public int getHorizontalSpacing() {
        computeMetrics();

        return horizontalSpacing;
    }

    public int getVerticalSpacing() {
        computeMetrics();

        return verticalSpacing;
    }

    public int getSpacing() {
        computeMetrics();

        return spacing;
    }

    public int getHorizontalMargin() {
        computeMetrics();

        return horizontalMargin;
    }

    public int getVerticalMargin() {
        computeMetrics();

        return verticalMargin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);

        CodeMarkerDispatch.dispatch(
            visible ? BasePreferencePage.CODEMARKER_VISIBLE_TRUE : BasePreferencePage.CODEMARKER_VISIBLE_FALSE);
    }
}
