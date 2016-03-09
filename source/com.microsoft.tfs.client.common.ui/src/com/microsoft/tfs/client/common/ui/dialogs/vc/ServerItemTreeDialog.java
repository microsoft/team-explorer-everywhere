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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemControlUtils;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemTreeControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public class ServerItemTreeDialog extends BaseDialog {
    private final String title;
    private final String initialPath;
    private final ServerItemSource serverItemSource;
    private final ServerItemType[] visibleTypes;

    private ServerItemTreeControl serverItemTreeControl;
    private Text selectedServerItemText;

    public ServerItemTreeDialog(
        final Shell parentShell,
        final String title,
        final String initialPath,
        final ServerItemSource serverItemSource,
        final ServerItemType[] visibleTypes) {
        super(parentShell);

        Check.notNull(title, "title"); //$NON-NLS-1$
        Check.notNull(serverItemSource, "serverItemSource"); //$NON-NLS-1$
        Check.notNull(visibleTypes, "visibleTypes"); //$NON-NLS-1$

        this.title = title;
        this.initialPath = initialPath;
        this.serverItemSource = serverItemSource;
        this.visibleTypes = visibleTypes;
    }

    @Override
    protected void hookAddToDialogArea(final Composite composite) {
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("ServerItemTreeDialog.TfsLabelText")); //$NON-NLS-1$

        final Text text = new Text(composite, SWT.BORDER);
        text.setText(serverItemSource.getServerName());
        text.setEditable(false);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(text);

        label = new Label(composite, SWT.NONE);

        if (displayFiles()) {
            label.setText(Messages.getString("ServerItemTreeDialog.ItemsLabelText")); //$NON-NLS-1$
        } else {
            label.setText(Messages.getString("ServerItemTreeDialog.FoldersLabelText")); //$NON-NLS-1$
        }

        GridDataBuilder.newInstance().vIndent(getVerticalSpacing()).applyTo(label);

        serverItemTreeControl = new ServerItemTreeControl(composite, SWT.NONE);
        serverItemTreeControl.setVisibleServerItemTypes(visibleTypes);
        serverItemTreeControl.setServerItemSource(serverItemSource);
        ServerItemControlUtils.setInitialSelection(initialPath, serverItemTreeControl);

        GridDataBuilder.newInstance().fill().grab().applyTo(serverItemTreeControl);

        label = new Label(composite, SWT.NONE);

        if (displayFiles()) {
            label.setText(Messages.getString("ServerItemTreeDialog.ItemPathLabelText")); //$NON-NLS-1$
        } else {
            label.setText(Messages.getString("ServerItemTreeDialog.FolderPathLabelText")); //$NON-NLS-1$
        }

        GridDataBuilder.newInstance().vIndent(getVerticalSpacing()).applyTo(label);

        selectedServerItemText = new Text(composite, SWT.BORDER);
        selectedServerItemText.setEditable(false);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(selectedServerItemText);

        serverItemTreeControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final IStructuredSelection ss = (IStructuredSelection) event.getSelection();
                final TypedServerItem item = (TypedServerItem) ss.getFirstElement();
                selectedServerItemChanged(item);
            }
        });

        if (displayFiles()) {
            serverItemTreeControl.addDoubleClickListener(new IDoubleClickListener() {
                @Override
                public void doubleClick(final DoubleClickEvent event) {
                    final TypedServerItem doubleClickedElement =
                        (TypedServerItem) ((IStructuredSelection) event.getSelection()).getFirstElement();

                    if (ServerItemType.isFile(doubleClickedElement.getType())
                        || ServerItemType.isGitRepository(doubleClickedElement.getType())) {
                        selectedServerItemChanged(doubleClickedElement);
                        setReturnCode(IDialogConstants.OK_ID);
                        close();
                    }
                }
            });
        }

        selectedServerItemChanged(serverItemTreeControl.getSelectedItem());

        serverItemTreeControl.setFocus();
    }

    private boolean displayFiles() {
        for (int i = 0; i < visibleTypes.length; i++) {
            if (ServerItemType.FILE.equals(visibleTypes[i])) {
                return true;
            }
        }

        return false;
    }

    protected void selectedServerItemChanged(final TypedServerItem serverItem) {
        if (serverItem == null) {
            selectedServerItemText.setText(""); //$NON-NLS-1$
        } else {
            selectedServerItemText.setText(serverItem.getServerPath());
        }
    }

    public TypedServerItem getSelectedItem() {
        return serverItemTreeControl.getSelectedItem();
    }

    public String getSelectedServerPath() {
        return serverItemTreeControl.getSelectedItem().getServerPath();
    }

    public ServerItemType getSelectedServerItemType() {
        return serverItemTreeControl.getSelectedItem().getType();
    }

    public ItemSpec getItemSpec() {
        final ServerItemType type = serverItemTreeControl.getSelectedItem().getType();
        final RecursionType recurType = (type == ServerItemType.FILE) ? RecursionType.NONE : RecursionType.FULL;

        return new ItemSpec(serverItemTreeControl.getSelectedItem().getServerPath(), recurType);
    }

    protected String getInitialPath() {
        return initialPath;
    }

    @Override
    protected String provideDialogTitle() {
        return title;
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
        final Button okButton = getButton(IDialogConstants.OK_ID);
        new ButtonValidatorBinding(okButton).bind(serverItemTreeControl.getSelectionValidator());
    }
}
