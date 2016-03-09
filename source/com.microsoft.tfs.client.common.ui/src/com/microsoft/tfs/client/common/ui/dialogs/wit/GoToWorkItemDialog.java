// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.wit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

public class GoToWorkItemDialog extends BaseDialog {
    public static final String WORKITEM_ID_TEXT_ID = "GoToWorkItemDialog.workItemIdText"; //$NON-NLS-1$

    private Text workItemIdText;
    protected int workItemId;

    public GoToWorkItemDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        SWTUtil.createLabel(dialogArea, Messages.getString("GoToWorkItemDialog.IdLabelText")); //$NON-NLS-1$
        workItemIdText = new Text(dialogArea, SWT.BORDER);
        AutomationIDHelper.setWidgetID(workItemIdText, WORKITEM_ID_TEXT_ID);

        GridDataBuilder.newInstance().hGrab().hFill().applyTo(workItemIdText);
        ControlSize.setCharWidthHint(workItemIdText, 42);
        workItemIdText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                int id = 0;
                try {
                    id = Integer.parseInt(workItemIdText.getText());
                } catch (final NumberFormatException nfe) {
                    // ignore
                }
                workItemId = id;
                getButton(IDialogConstants.OK_ID).setEnabled(id > 0);
            }
        });
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("GoToWorkItemDialog.GotoWorkItemDialogTitle"); //$NON-NLS-1$
    }

    public int getID() {
        return workItemId;
    }

}
