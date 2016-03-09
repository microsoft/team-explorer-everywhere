// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.controls.wit.WorkItemHistoryControl;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.fields.Field;

public class LogControl extends LabelableControl {
    @Override
    public boolean wantsVerticalFill() {
        return true;
    }

    @Override
    protected Field getFieldForToolTipSupport() {
        return getWorkItem().getFields().getField(CoreFieldReferenceNames.HISTORY);
    }

    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        final WorkItemHistoryControl historyControl =
            new WorkItemHistoryControl(parent, SWT.NONE, getWorkItem(), !isFormReadonly());

        historyControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, columnsToTake, 1));

        final Field workItemField = getWorkItem().getFields().getField(CoreFieldReferenceNames.HISTORY);

        getFieldTracker().addField(workItemField);
        getFieldTracker().setFocusReceiver(workItemField, new FieldTracker.FocusReceiver() {
            @Override
            public boolean setFocus() {
                return historyControl.setFocus();
            }
        });
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }
}
