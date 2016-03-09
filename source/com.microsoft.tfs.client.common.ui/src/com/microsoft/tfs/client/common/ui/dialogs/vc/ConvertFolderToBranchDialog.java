// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;

public class ConvertFolderToBranchDialog extends BaseDialog {

    public static final String OWNER_TEXTBOX_ID = "ConvertFolderToBranchDialog.ownerTextbox"; //$NON-NLS-1$
    public static final String DESCRIPTION_TEXTBOX_ID = "ConvertFolderToBranchDialog.descriptionTextbox"; //$NON-NLS-1$
    public static final String RECURSE_CHECKBOX_ID = "ConvertFolderToBranchDialog.recursiveCheckbox"; //$NON-NLS-1$

    private final String user;
    private final TFSItem item;
    private String owner;
    private String description;

    boolean recurse;

    private Text ownerText;
    private Text descriptionText;

    private Button recurseCheck;

    public ConvertFolderToBranchDialog(
        final Shell parentShell,
        final TFSItem item,
        final String user,
        final TFSRepository repository) {
        super(parentShell);
        this.item = item;
        this.user = user;
        setOptionIncludeDefaultButtons(false);
        addButtonDescription(
            IDialogConstants.OK_ID,
            Messages.getString("ConvertFolderToBranchDialog.ConvertButtonText"), //$NON-NLS-1$
            true);
        addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

    }

    @Override
    protected String provideDialogTitle() {
        if (item == null) {
            return Messages.getString("ConvertFolderToBranchDialog.DialogTitle"); //$NON-NLS-1$
        } else {
            final String messageFormat = Messages.getString("ConvertFolderToBranchDialog.DialogTitleFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, item.getName());

        }
    }

    @Override
    protected void hookAddToDialogArea(final Composite container) {
        final Composite composite = new Composite(container, SWT.NONE);
        final GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);
        GridDataBuilder.newInstance().fill().grab().applyTo(composite);

        final Label branchLabel =
            SWTUtil.createLabel(composite, Messages.getString("ConvertFolderToBranchDialog.BranchNameLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hSpan(layout).applyTo(branchLabel);

        final Text branchText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
        branchText.setText(item.getSourceServerPath());
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab(true).applyTo(branchText);

        SWTUtil.createLabel(composite, Messages.getString("ConvertFolderToBranchDialog.OwnerLabelText")); //$NON-NLS-1$
        ownerText = new Text(composite, SWT.BORDER);
        AutomationIDHelper.setWidgetID(ownerText, OWNER_TEXTBOX_ID);

        ownerText.setText(user);
        GridDataBuilder.newInstance().hFill().hGrab(true).applyTo(ownerText);

        final Label descriptionLabel =
            SWTUtil.createLabel(composite, Messages.getString("ConvertFolderToBranchDialog.DescriptionLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().applyTo(descriptionLabel);

        descriptionText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        AutomationIDHelper.setWidgetID(descriptionText, DESCRIPTION_TEXTBOX_ID);
        GridDataBuilder.newInstance().hSpan(layout).fill().grab().applyTo(descriptionText);
        ControlSize.setCharHeightHint(descriptionText, 10);

        recurseCheck = new Button(composite, SWT.CHECK);
        AutomationIDHelper.setWidgetID(recurseCheck, RECURSE_CHECKBOX_ID);
        recurseCheck.setText(Messages.getString("ConvertFolderToBranchDialog.RecursiveCheckboxText")); //$NON-NLS-1$
        recurseCheck.setSelection(true);
        GridDataBuilder.newInstance().hSpan(layout).applyTo(recurseCheck);

        descriptionText.setFocus();
    }

    @Override
    protected void hookDialogAboutToClose() {
        super.hookDialogAboutToClose();
        owner = ownerText.getText();
        description = descriptionText.getText();
        recurse = recurseCheck.getSelection();
    }

    public String getOwner() {
        return owner;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRecursive() {
        return recurse;
    }

}