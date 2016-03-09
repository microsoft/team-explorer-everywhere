// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;

public class PromptViewOldVersionDialog extends BaseDialog {
    public static final int SERVER_OPTION = 1;
    public static final int WORKSPCE_OPTION = 2;

    private static final String PROMPT_TEXT = Messages.getString("PromptViewOldVersionDialog.PromptLabelText"); //$NON-NLS-1$

    private int selectedOption;

    public PromptViewOldVersionDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite container = (Composite) super.createDialogArea(parent);

        selectedOption = SERVER_OPTION;

        final FormLayout layout = new FormLayout();
        layout.marginHeight = getHorizontalMargin();
        layout.marginWidth = getVerticalMargin();
        layout.spacing = getVerticalSpacing();
        container.setLayout(layout);

        final Label label = new Label(container, SWT.NONE);
        label.setText(PROMPT_TEXT);
        final FormData fd1 = new FormData();
        fd1.top = new FormAttachment(0, 0);
        fd1.left = new FormAttachment(0, 0);
        label.setLayoutData(fd1);

        final Button serverButton = new Button(container, SWT.RADIO);
        serverButton.setText(Messages.getString("PromptViewOldVersionDialog.ServerVersionButtonText")); //$NON-NLS-1$
        serverButton.setSelection(true);
        final FormData fd2 = new FormData();
        fd2.top = new FormAttachment(label, 0, SWT.BOTTOM);
        fd2.left = new FormAttachment(0, 0);
        serverButton.setLayoutData(fd2);

        serverButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedOption = SERVER_OPTION;
            }
        });

        final Button workspaceButton = new Button(container, SWT.RADIO);
        workspaceButton.setText(Messages.getString("PromptViewOldVersionDialog.WorkspaceVersionButtonText")); //$NON-NLS-1$
        final FormData fd3 = new FormData();
        fd3.top = new FormAttachment(serverButton, 0, SWT.BOTTOM);
        fd3.left = new FormAttachment(0, 0);
        workspaceButton.setLayoutData(fd3);

        workspaceButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedOption = WORKSPCE_OPTION;
            }
        });

        return container;
    }

    public int getSelectedOption() {
        return selectedOption;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("PromptViewOldVersionDialog.DialogTitle"); //$NON-NLS-1$
    }
}
