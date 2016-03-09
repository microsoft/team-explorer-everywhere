// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemControlUtils;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemPicker;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

public class ServerItemPickerDialog extends BaseDialog {
    private final String title;
    private final String initialPath;
    private final ServerItemSource serverItemSource;

    private ServerItemPicker serverItemPicker;

    public ServerItemPickerDialog(
        final Shell parentShell,
        final String title,
        final String initialPath,
        final ServerItemSource serverItemSource) {
        super(parentShell);

        Check.notNull(title, "title"); //$NON-NLS-1$
        Check.notNull(serverItemSource, "serverItemSource"); //$NON-NLS-1$

        this.title = title;
        this.initialPath = initialPath;
        this.serverItemSource = serverItemSource;
    }

    public TypedServerItem getSelectedServerItem() {
        return serverItemPicker.getServerItemTable().getSelectedServerItem();
    }

    public String getSelectedServerPath() {
        return getSelectedServerItem().getServerPath();
    }

    @Override
    protected void hookAddToDialogArea(final Composite composite) {
        final GridLayout layout = SWTUtil.gridLayout(composite, 2);

        serverItemPicker = new ServerItemPicker(composite, SWT.NONE);
        GridDataBuilder.newInstance().grab().fill().hSpan(layout).applyTo(serverItemPicker);

        SWTUtil.createLabel(composite, Messages.getString("ServerItemPickerDialog.NameLabelText")); //$NON-NLS-1$

        final Text selectedItemText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(selectedItemText);

        serverItemPicker.getServerItemTable().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                final TypedServerItem selectedItem = (TypedServerItem) selection.getFirstElement();
                if (selectedItem == null) {
                    selectedItemText.setText(""); //$NON-NLS-1$
                } else {
                    selectedItemText.setText(selectedItem.getServerPath());
                }
            }
        });

        serverItemPicker.getServerItemTable().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                final TypedServerItem item = (TypedServerItem) selection.getFirstElement();
                if (item != null && item.getType() == ServerItemType.FILE) {
                    okPressed();
                }
            }
        });

        serverItemPicker.setServerItemSource(serverItemSource);

        final boolean initialSelection = ServerItemControlUtils.setInitialSelection(initialPath, serverItemPicker);
        if (!initialSelection) {
            serverItemPicker.setCurrentFolderPath(ServerPath.ROOT);
        }
    }

    @Override
    protected String provideDialogTitle() {
        return title;
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
        final Button okButton = getButton(IDialogConstants.OK_ID);
        new ButtonValidatorBinding(okButton).bind(serverItemPicker.getServerItemTable().getSelectionValidator());
    }
}
