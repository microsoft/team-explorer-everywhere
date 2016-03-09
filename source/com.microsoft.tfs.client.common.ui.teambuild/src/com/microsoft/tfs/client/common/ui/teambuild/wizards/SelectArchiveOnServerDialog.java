// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
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

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemControlUtils;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemTreeControl;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.commands.CreateUploadZipCommand;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

public class SelectArchiveOnServerDialog extends ExtendedButtonDialog {
    private final String title;
    private final String buildToolName;
    private final String initialPath;
    private final ServerItemSource serverItemSource;
    private final ServerItemType[] visibleTypes;
    private static final int ADD_ID = IDialogConstants.CLIENT_ID + 1;

    private ServerItemTreeControl serverItemTreeControl;
    private Text selectedServerItemText;

    private String serverPath;

    private final IBuildDefinition buildDefinition;

    public SelectArchiveOnServerDialog(
        final Shell parentShell,
        final String title,
        final String buildToolName,
        final String initialPath,
        final ServerItemType[] visibleTypes,
        final IBuildDefinition buildDefinition) {
        super(parentShell);

        Check.notNull(title, "title"); //$NON-NLS-1$
        Check.notNull(buildDefinition, "buildDefinition"); //$NON-NLS-1$
        Check.notNull(visibleTypes, "visibleTypes"); //$NON-NLS-1$

        this.title = title;
        this.buildToolName = buildToolName;
        this.initialPath = initialPath;
        this.buildDefinition = buildDefinition;
        this.serverItemSource = new VersionedItemSource(
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getOrCreateServer(
                buildDefinition.getBuildServer().getConnection()));
        this.visibleTypes = visibleTypes;
        addExtendedButtonDescription(ADD_ID, Messages.getString("SelectArchiveOnServerDialog.AddButtonLabel"), false); //$NON-NLS-1$
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
        label.setText(Messages.getString("SelectArchiveOnServerDialog.TfsLabelText")); //$NON-NLS-1$

        final Text text = new Text(composite, SWT.BORDER);
        text.setText(serverItemSource.getServerName());
        text.setEditable(false);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(text);

        label = new Label(composite, SWT.NONE);

        if (displayFiles()) {
            label.setText(Messages.getString("SelectArchiveOnServerDialog.ItemsLabelText")); //$NON-NLS-1$
        } else {
            label.setText(Messages.getString("SelectArchiveOnServerDialog.FoldersLabelText")); //$NON-NLS-1$
        }

        GridDataBuilder.newInstance().vIndent(getVerticalSpacing()).applyTo(label);

        serverItemTreeControl = new ServerItemTreeControl(composite, SWT.NONE);
        serverItemTreeControl.setVisibleServerItemTypes(visibleTypes);
        serverItemTreeControl.setServerItemSource(serverItemSource);
        ServerItemControlUtils.setInitialSelection(initialPath, serverItemTreeControl);

        GridDataBuilder.newInstance().fill().grab().applyTo(serverItemTreeControl);

        label = new Label(composite, SWT.NONE);

        if (displayFiles()) {
            label.setText(Messages.getString("SelectArchiveOnServerDialog.ItemPathLabelText")); //$NON-NLS-1$
        } else {
            label.setText(Messages.getString("SelectArchiveOnServerDialog.FolderPathLabelText")); //$NON-NLS-1$
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

                    if (ServerItemType.isFile(doubleClickedElement.getType())) {
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

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (buttonId == ADD_ID) {
            if (!Platform.isCurrentPlatform(Platform.WINDOWS)) {
                MessageBoxHelpers.messageBox(
                    getShell(),
                    Messages.getString("SelectArchiveOnServerDialog.UnsupportedPlatformMessage"), //$NON-NLS-1$
                    Messages.getString("SelectArchiveOnServerDialog.UnsupportedPlatformText")); //$NON-NLS-1$
                return;
            }

            final String uploadDialogTitle =
                MessageFormat.format(Messages.getString("UploadArchiveDialog.DialogTitleFormat"), buildToolName); //$NON-NLS-1$
            final UploadArchiveDialog uploadDialog = new UploadArchiveDialog(
                getShell(),
                uploadDialogTitle,
                buildToolName,
                getSelectedServerFolder(),
                buildDefinition);
            if (uploadDialog.open() == IDialogConstants.OK_ID) {
                final String localDir = uploadDialog.getLocalPath();
                final String zipFileName = uploadDialog.getArchiveName();
                final String serverDir = uploadDialog.getServerPath();

                if (localDir != null && serverDir != null && zipFileName != null) {
                    final CreateUploadZipCommand command =
                        new CreateUploadZipCommand(localDir, zipFileName, serverDir, buildDefinition, buildToolName);

                    final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getShell());
                    final IStatus status = executor.execute(command);
                    if (status.isOK()) {
                        // Set the current selection to the parent of the
                        // uploaded folder so that the tree refreshes
                        // appropriately
                        ServerItemControlUtils.setInitialSelection(serverDir, serverItemTreeControl);
                        serverItemTreeControl.refreshTree();
                        serverPath = ServerPath.combine(serverDir, zipFileName);
                        ServerItemControlUtils.setInitialSelection(serverPath, serverItemTreeControl);
                        selectedServerItemText.setText(serverPath);
                    }
                }
            }
        }
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
            serverPath = serverItem.getServerPath();
            selectedServerItemText.setText(serverPath);
        }
    }

    public TypedServerItem getSelectedItem() {
        return serverItemTreeControl.getSelectedItem();
    }

    public String getSelectedServerFolder() {
        final TypedServerItem selectedItem = serverItemTreeControl.getSelectedItem();
        final String selectedServerPath = selectedItem.getServerPath();
        if (ServerItemType.isFile(selectedItem.getType())) {
            return selectedItem.getParent().getServerPath();
        } else if (ServerPath.isDirectChild(ServerPath.ROOT, selectedServerPath)) {
            return ServerPath.combine(selectedServerPath, "Libs"); //$NON-NLS-1$
        } else {
            return selectedServerPath;
        }
    }

    public String getServerPath() {
        return serverPath;
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
