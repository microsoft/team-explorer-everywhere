// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A control contribution item that is resizable based on the widget contents.
 *
 * @threadsafety unknown
 */
public abstract class ResizableControlContribution extends ContributionItem {
    Control control;
    ToolItem toolItem;

    /**
     * Creates a resizable control contribution item with the given id.
     *
     * @param id
     *        the contribution item id
     */
    protected ResizableControlContribution(final String id) {
        super(id);
    }

    /**
     * {@inheritDoc}
     */
    protected int computeWidth(final Control control) {
        return control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;
    }

    /**
     * Creates and returns the control for this contribution item under the
     * given parent composite.
     *
     * @param parent
     *        the parent composite (not <code>null</code>)
     * @return the new control (not <code>null</code>)
     */
    protected abstract Control createControl(Composite parent);

    /**
     * {@inheritDoc}
     */
    @Override
    public final void fill(final Composite parent) {
        createControl(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void fill(final Menu parent, final int index) {
        throw new RuntimeException("Control contribution items are not available for use in menus."); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void fill(final ToolBar parent, final int index) {
        control = createControl(parent);
        toolItem = new ToolItem(parent, SWT.SEPARATOR, index);
        toolItem.setControl(control);
        toolItem.setWidth(computeWidth(control));
    }

    /**
     * Resizes this control contribution to the preferences of the receiver.
     */
    public final void resize() {
        if (toolItem != null && !toolItem.isDisposed() && control != null && !control.isDisposed()) {
            toolItem.setWidth(computeWidth(control));
        }
    }
}
