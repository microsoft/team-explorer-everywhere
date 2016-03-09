// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public abstract class ModifyFolderMappingDialog extends BaseDialog {
    private String localFolder;
    final private String repositoryFolder;
    final private String mappingReason;

    private Button browseLocalFolder;
    private Text statusText;
    private Text localFolderField;
    private Text repositoryFolderField;

    public ModifyFolderMappingDialog(
        final Shell parentShell,
        final Workspace workspace,
        final String repositoryFolder,
        final String localFolder) {
        super(parentShell);

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(repositoryFolder, "repositoryFolder"); //$NON-NLS-1$

        this.repositoryFolder = repositoryFolder;
        this.localFolder = localFolder;
        this.mappingReason = getMappingStatusText(workspace, repositoryFolder);

        this.setOptionIncludeDefaultButtons(false);
        addButtonDescription(IDialogConstants.OK_ID, getOkButtonText(), true);
        addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    public abstract String getPurposeText();

    public abstract String getOkButtonText();

    public abstract String getTitleText();

    @Override
    protected String provideDialogTitle() {
        return getTitleText();
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite container = (Composite) super.createDialogArea(parent);

        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        if (localFolder == null) {
            localFolder = ""; //$NON-NLS-1$
        }

        final String purpose = getPurposeText();
        if (purpose != null) {
            final Label purposeLabel = new Label(container, SWT.NONE);
            purposeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
            purposeLabel.setText(purpose);
        }

        if (mappingReason != null) {
            final Label statusLabel = new Label(container, SWT.NONE);
            statusLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
            statusLabel.setText(Messages.getString("ModifyFolderMappingDialog.CurrentStatusLabel")); //$NON-NLS-1$

            statusText = new Text(container, SWT.BORDER);
            statusText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
            statusText.setText(mappingReason);
            statusText.setEnabled(false);
        }

        final Label label = new Label(container, SWT.NONE);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
        label.setText(Messages.getString("SetWorkingFolderDialog.RepositoryFolderLabelText")); //$NON-NLS-1$

        repositoryFolderField = new Text(container, SWT.BORDER);
        repositoryFolderField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        repositoryFolderField.setText(repositoryFolder);
        repositoryFolderField.setEditable(false);

        // fill the last cell on this grid row with a spacer.
        SWTUtil.createGridLayoutSpacer(container);

        final Label label_1 = new Label(container, SWT.NONE);
        label_1.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
        label_1.setText(Messages.getString("SetWorkingFolderDialog.LocalFolderLabelText")); //$NON-NLS-1$

        localFolderField = new Text(container, SWT.BORDER);
        localFolderField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        localFolderField.setText(localFolder);
        localFolderField.setEditable(false);

        browseLocalFolder = new Button(container, SWT.NONE);
        browseLocalFolder.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        browseLocalFolder.setText(Messages.getString("SetWorkingFolderDialog.BrowseLocalButtonText")); //$NON-NLS-1$
        browseLocalFolder.setEnabled(false);

        ControlSize.setCharWidthHint(repositoryFolderField, 80);

        ButtonHelper.setButtonsToButtonBarSize(new Button[] {
            browseLocalFolder
        });

        return container;
    }

    @Override
    protected void hookAfterButtonsCreated() {
        getButton(IDialogConstants.OK_ID).setFocus();
    }

    public static String getMappingStatusText(final Workspace workspace, final String serverPath) {
        final WorkingFolder workingFolder = workspace.getExactMappingForServerPath(serverPath);

        // There is a direct mapping for this server path.
        if (workingFolder != null) {
            if (workingFolder.isCloaked() == false) {
                return Messages.getString("ModifyFolderMappingDialog.ServerPathMapped"); //$NON-NLS-1$
            } else {
                return Messages.getString("ModifyFolderMappingDialog.ServerPathCloaked"); //$NON-NLS-1$
            }
        }

        // There is no direct mapping. Find the closest parent mapping.
        final WorkingFolder closestWorkingFolder = workspace.getClosestMappingForServerPath(serverPath);

        // There is no parent mapping, the server path is not mapped.
        if (closestWorkingFolder == null) {
            return Messages.getString("ModifyFolderMappingDialog.ServerPathNotMapped"); //$NON-NLS-1$
        }

        // We have a parent mapping. Is it cloaked?
        if (closestWorkingFolder.isCloaked()) {
            return Messages.getString("ModifyFolderMappingDialog.ServerPathCloakedByParent"); //$NON-NLS-1$
        }

        // The server path is mapped due to a parent mapping.
        if (workspace.isServerPathMapped(serverPath)) {
            if (closestWorkingFolder.getDepth() == RecursionType.FULL) {
                return Messages.getString("ModifyFolderMappingDialog.ServerPathMappedByParent"); //$NON-NLS-1$
            } else {
                return Messages.getString("ModifyFolderMappingDialog.ServerPathMappedByParentOneLvel"); //$NON-NLS-1$
            }
        } else {
            return Messages.getString("ModifyFolderMappingDialog.ServerPathNotMapped"); //$NON-NLS-1$
        }
    }
}
