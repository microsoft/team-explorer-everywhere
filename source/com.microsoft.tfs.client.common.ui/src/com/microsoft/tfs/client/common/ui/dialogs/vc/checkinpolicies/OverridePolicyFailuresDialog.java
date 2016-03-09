// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.checkinpolicies;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;

public class OverridePolicyFailuresDialog extends BaseDialog {
    private final String customErrorMessage;

    private String comment;
    private boolean enableOverride;

    public OverridePolicyFailuresDialog(final Shell parentShell) {
        this(parentShell, null);
    }

    public OverridePolicyFailuresDialog(final Shell parentShell, final String customErrorMessage) {
        super(parentShell);

        this.customErrorMessage = customErrorMessage;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        SWTUtil.gridLayout(dialogArea);

        final Composite composite = SWTUtil.createComposite(dialogArea);
        SWTUtil.gridLayout(composite, 2, false, 5, 10);

        SWTUtil.createLabel(composite, dialogArea.getDisplay().getSystemImage(SWT.ICON_WARNING));

        if (customErrorMessage != null) {
            SWTUtil.createLabel(composite, customErrorMessage);
        } else {
            SWTUtil.createLabel(composite, Messages.getString("OverridePolicyFailuresDialog.ErrorStatusLabelText")); //$NON-NLS-1$
        }

        final Button button = SWTUtil.createButton(
            dialogArea,
            SWT.CHECK,
            Messages.getString("OverridePolicyFailuresDialog.OverridePolicyButtonText")); //$NON-NLS-1$

        final Label reasonLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("OverridePolicyFailuresDialog.ReasonLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(reasonLabel);
        reasonLabel.setEnabled(false);

        final Text text = new Text(dialogArea, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().applyTo(text);
        ControlSize.setCharHeightHint(text, 4);
        text.setEnabled(false);

        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                comment = ((Text) e.widget).getText();
                updateOK();
            }
        });

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                enableOverride = ((Button) e.widget).getSelection();
                reasonLabel.setEnabled(enableOverride);
                text.setEnabled(enableOverride);
                updateOK();
            }
        });
    }

    public String getOverrideComment() {
        return comment;
    }

    private void updateOK() {
        final Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setEnabled(enableOverride && comment != null && comment.trim().length() > 0);
    }

    @Override
    protected void hookAfterButtonsCreated() {
        updateOK();
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("OverridePolicyFailuresDialog.PolicyFailureDialogTitle"); //$NON-NLS-1$
    }
}
