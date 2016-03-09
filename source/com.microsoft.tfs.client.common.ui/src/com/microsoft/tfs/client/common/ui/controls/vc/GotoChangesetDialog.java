// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.vc.FindChangesetDialog;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;

public class GotoChangesetDialog extends ExtendedButtonDialog {
    private final TFSRepository repository;
    private Text changesetIdText;
    protected int changesetId;

    public GotoChangesetDialog(final Shell parentShell, final TFSRepository repository) {
        super(parentShell);
        this.repository = repository;
    }

    public int getChangesetID() {
        return changesetId;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        // Layout the dialog contents.
        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        SWTUtil.createLabel(dialogArea, Messages.getString("GotoChangesetDialog.ChangesetLabelText")); //$NON-NLS-1$
        changesetIdText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(changesetIdText);
        ControlSize.setCharWidthHint(changesetIdText, 42);
        changesetIdText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                int id = 0;
                try {
                    id = Integer.parseInt(changesetIdText.getText());
                } catch (final NumberFormatException nfe) {
                    // ignore
                }
                changesetId = id;
                getButton(IDialogConstants.OK_ID).setEnabled(id > 0);
            }
        });

        final Button button = new Button(dialogArea, SWT.PUSH);
        button.setText(Messages.getString("GotoChangesetDialog.FindButtonText")); //$NON-NLS-1$
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                // Close down this dialog and launch a different one.
                cancelPressed();

                final FindChangesetDialog findChangesetDialog = new FindChangesetDialog(getShell(), repository);
                findChangesetDialog.setCloseOnlyMode(true);
                findChangesetDialog.open();
            }
        });

        ButtonHelper.setButtonToButtonBarSize(button);
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("GotoChangesetDialog.DialogTitle"); //$NON-NLS-1$
    }
}
