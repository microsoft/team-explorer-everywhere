// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.wit;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;

public class SetQueryItemNameDialog extends BaseDialog {
    public static final String QUERYITEM_NAME_TEXT_ID = "SetQueryItemNameDialog.queryItemNameText"; //$NON-NLS-1$

    private String originalName = null;
    private QueryFolder parent = null;

    private String initialName;
    private Text folderNameText;

    private String name;

    public SetQueryItemNameDialog(final Shell parentShell) {
        super(parentShell);

        setOptionPersistGeometry(false);
        setOptionResizableDirections(SWT.HORIZONTAL);
    }

    public void setOriginalName(final String originalName) {
        this.originalName = originalName;
    }

    public void setParent(final QueryFolder parent) {
        this.parent = parent;
    }

    @Override
    protected String provideDialogTitle() {
        return (originalName == null) ? Messages.getString("SetQueryItemNameDialog.NewFolderDialogTitle") //$NON-NLS-1$
            : Messages.getString("SetQueryItemNameDialog.RenameQueryDialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        SWTUtil.createLabel(dialogArea, Messages.getString("SetQueryItemNameDialog.NameLabelText")); //$NON-NLS-1$

        folderNameText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(folderNameText);
        ControlSize.setCharWidthHint(folderNameText, 42);
        AutomationIDHelper.setWidgetID(folderNameText, QUERYITEM_NAME_TEXT_ID);

        folderNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                name = folderNameText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(name.length() > 0);
            }
        });

        if (originalName != null) {
            initialName = originalName;
        } else if (parent != null) {
            while (initialName == null) {
                final QueryItem[] peers = parent.getItems();

                for (int i = 0; i >= 0; i++) {
                    String attemptedName;
                    if (i == 0) {
                        attemptedName = Messages.getString("SetQueryItemNameDialog.DefaultFolderName"); //$NON-NLS-1$
                    } else {
                        final String messageFormat =
                            Messages.getString("SetQueryItemNameDialog.DefaultFolderNameFormat"); //$NON-NLS-1$
                        attemptedName = MessageFormat.format(messageFormat, Integer.toString(i + 1));
                    }

                    boolean exists = false;
                    for (int j = 0; j < peers.length; j++) {
                        if (peers[j].getName().equalsIgnoreCase(attemptedName)) {
                            exists = true;
                        }
                    }

                    if (!exists) {
                        initialName = attemptedName;
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void hookAfterButtonsCreated() {
        if (initialName != null) {
            folderNameText.setText(initialName);
            folderNameText.setSelection(0, initialName.length());
        } else {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
    }

    @Override
    protected void okPressed() {
        if (name == null || name.length() == 0) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("SetQueryItemNameDialog.InvalidNameDialogTitle"), //$NON-NLS-1$
                Messages.getString("SetQueryItemNameDialog.InvalidNameDialogText")); //$NON-NLS-1$
            return;
        }

        if (parent != null && !name.equalsIgnoreCase(originalName)) {
            final QueryItem[] peers = parent.getItems();

            for (int i = 0; i < peers.length; i++) {
                if (peers[i].getName().equalsIgnoreCase(name)) {
                    MessageDialog.openError(
                        getShell(),
                        Messages.getString("SetQueryItemNameDialog.ItemExistsDialogTitle"), //$NON-NLS-1$
                        Messages.getString("SetQueryItemNameDialog.ItemExistsDialogText")); //$NON-NLS-1$

                    folderNameText.setFocus();
                    folderNameText.setSelection(0, name.length());
                    return;
                }
            }
        }

        if (name.equals(originalName)) {
            super.cancelPressed();
        } else {
            super.okPressed();
        }
    }

    public String getName() {
        return name;
    }
}
