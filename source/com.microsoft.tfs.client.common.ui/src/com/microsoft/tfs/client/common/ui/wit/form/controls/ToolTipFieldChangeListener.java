// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;

/**
 * A work item field change listener that can update a control's tool tip based
 * on the help text of a work item field.
 */
public class ToolTipFieldChangeListener implements FieldChangeListener {
    private final Control control;

    public ToolTipFieldChangeListener(final Control control) {
        this.control = control;
    }

    @Override
    public void fieldChanged(final FieldChangeEvent event) {
        final Field field = event.field;

        UIHelpers.runOnUIThread(control.getDisplay(), true, new Runnable() {
            @Override
            public void run() {
                if (control.isDisposed()) {
                    return;
                }

                control.setToolTipText(field.getHelpText());
            }
        });
    }
}
