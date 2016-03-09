// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.controls.vc.BranchHistoryTreeControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistoryTreeItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Dialog for the switch mechanism.
 */
public class SwitchToBranchDialog extends BaseDialog {
    private final Workspace workspace;
    private final String serverPath;
    private String switchToServerPath;

    private BranchHistoryTreeControl branchHistoryControl;

    public String getSwitchToServerPath() {
        return switchToServerPath;
    }

    private Text pathText;

    /**
     *
     * @param parentShell
     */
    public SwitchToBranchDialog(final Shell parentShell, final Workspace workspace, final String serverPath) {
        super(parentShell);
        this.workspace = workspace;
        this.serverPath = serverPath;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * provideDialogTitle()
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("SwitchToBranchDialog.DialogTitle"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * hookAddToDialogArea(org.eclipse. swt.widgets.Composite)
     */
    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout dialogLayout = new GridLayout(2, false);

        dialogLayout.marginWidth = getHorizontalMargin();
        dialogLayout.marginHeight = getVerticalMargin();
        dialogLayout.horizontalSpacing = getHorizontalSpacing();
        dialogLayout.verticalSpacing = getVerticalSpacing();

        dialogArea.setLayout(dialogLayout);

        final Label label =
            SWTUtil.createLabel(dialogArea, Messages.getString("SwitchToBranchDialog.SelectBranchLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(dialogLayout).applyTo(label);

        branchHistoryControl = new BranchHistoryTreeControl(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(dialogLayout).grab().fill().wHint(getMinimumMessageAreaWidth()).hCHint(
            branchHistoryControl,
            12).applyTo(branchHistoryControl);

        SWTUtil.createLabel(dialogArea, Messages.getString("SwitchToBranchDialog.BranchLabelText")); //$NON-NLS-1$

        pathText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(pathText);
        pathText.setText(serverPath);
        pathText.setEditable(false);

        branchHistoryControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final BranchHistoryTreeItem item = branchHistoryControl.getSelectedBranchHistoryTreeItem();
                selectedBranchHistoryItemChanged(item);
            }
        });

        branchHistoryControl.setSourceItem(workspace, new TypedServerItem(serverPath, ServerItemType.FOLDER));
    }

    @Override
    protected void hookAfterButtonsCreated() {
        /* OK button only enabled when item is selected */
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    private void setSwitchToPath(final String serverPath) {
        if (serverPath != null && ServerPath.isServerPath(serverPath)) {
            pathText.setText(serverPath);
        }
    }

    protected void selectedBranchHistoryItemChanged(final BranchHistoryTreeItem item) {
        if (item == null) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        } else {
            setSwitchToPath(item.getServerItem());
            getButton(IDialogConstants.OK_ID).setEnabled(item.getItem() != null && item.getItem().getDeletionID() == 0);
        }
    }

    @Override
    protected void hookDialogAboutToClose() {
        switchToServerPath = pathText.getText().trim();
    }
}
