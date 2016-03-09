// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.celleditor;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;

/*
 * This is a big hack to fix a Mac bug.
 *
 * SWT/MacOS has a strange bug in TabFolder / CCombo interaction. TabFolder
 * selection (ie, changing tabs) does not unfocus the CCombo. Thus, when used
 * you use a ComboBoxCellEditor inside a TabFolder, if you click the TabFolder
 * (to change tabs), your cell edited value is *NOT* committed.
 *
 * This adds a selection filter on Display to catch clicks outside of the cell
 * editor, which commits the value.
 */

public class SafeComboBoxCellEditor extends ComboBoxCellEditor {
    public SafeComboBoxCellEditor() {
        super();
    }

    public SafeComboBoxCellEditor(final Composite parent, final String[] items) {
        super(parent, items);
    }

    public SafeComboBoxCellEditor(final Composite parent, final String[] items, final int style) {
        super(parent, items, style);
    }

    @Override
    protected Control createControl(final Composite parent) {
        final Control control = super.createControl(parent);

        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            final Listener macSelectionListener = new MacSelectionListener(parent);

            control.getDisplay().addFilter(SWT.Selection, macSelectionListener);
            control.addListener(SWT.Dispose, new Listener() {
                @Override
                public void handleEvent(final Event e) {
                    e.widget.getDisplay().removeFilter(SWT.Selection, macSelectionListener);
                }
            });
        }

        return control;
    }

    /*
     * This is the listener which will be attached as a selection filter on
     * Display. It will catch any selection events which occur outside the scope
     * of our cell editor / CCombo.
     */
    private class MacSelectionListener implements Listener {
        private final Composite cellEditorParent;

        public MacSelectionListener(final Composite cellEditorParent) {
            this.cellEditorParent = cellEditorParent;
        }

        @Override
        public void handleEvent(final Event e) {
            if (e.widget.isDisposed()) {
                return;
            }

            if (e.widget instanceof Control) {
                /*
                 * Widgets shell is not ours - this could be an event in our
                 * popup list. SWT handles these events properly, so ignore it.
                 */
                if (((Control) e.widget).getShell() != cellEditorParent.getShell()) {
                    return;
                }

                /*
                 * Walk up the selected control's parentage. If this control is
                 * the CCombo, or part of the CCombo (the text field or
                 * disclosure button), then the selection should proceed
                 * normally.
                 *
                 * Determine this by looking for our parent in the ancestry.
                 * Anything up to (and including) our parent is handled properly
                 * by SWT.
                 */
                for (Control testControl = (Control) e.widget; testControl != null; testControl =
                    testControl.getParent()) {
                    if (testControl.isDisposed() || testControl == cellEditorParent) {
                        return;
                    }
                }
            }

            /*
             * Click occurred outside the bounds of our CCombo - fire a FocusOut
             * event
             */
            focusLost();
        }
    }
}
