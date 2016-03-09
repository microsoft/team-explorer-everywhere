// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class MoveDialog extends BaseDialog {
    public static final String DESTINATION_TEXT_ID = "MoveDialog.destinationText"; //$NON-NLS-1$
    private final TFSRepository repository;
    private final String sourceServerPath;
    private String destinationServerPath;

    private Text sourceText;
    private Text destinationText;

    public MoveDialog(final Shell parentShell, final TFSRepository repository, final String sourceServerPath) {
        super(parentShell);

        this.repository = repository;
        this.sourceServerPath = sourceServerPath;
        destinationServerPath = sourceServerPath;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("MoveDialog.MoveDialogTitle"); //$NON-NLS-1$
    }

    public void setDestinationServerPath(final String destinationServerPath) {
        this.destinationServerPath = destinationServerPath;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label sourceLabel = new Label(dialogArea, SWT.NONE);
        sourceLabel.setText(Messages.getString("MoveDialog.SourceLabelText")); //$NON-NLS-1$

        sourceText = new Text(dialogArea, SWT.BORDER);
        sourceText.setText(sourceServerPath);
        sourceText.setEnabled(false);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(sourceText);

        final Label destinationLabel = new Label(dialogArea, SWT.NONE);
        destinationLabel.setText(Messages.getString("MoveDialog.DestinationLabelText")); //$NON-NLS-1$

        destinationText = new Text(dialogArea, SWT.BORDER);
        AutomationIDHelper.setWidgetID(destinationText, DESTINATION_TEXT_ID);
        destinationText.setText(destinationServerPath);
        destinationText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                getButton(IDialogConstants.OK_ID).setEnabled(destinationText.getText().length() > 0);
            }
        });
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(destinationText);

        final Button browseButton = new Button(dialogArea, SWT.PUSH);
        browseButton.setText(Messages.getString("MoveDialog.BrowseButtonText")); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browse();
            }
        });
    }

    private void browse() {
        String destinationPath = destinationText.getText();

        if (!destinationPath.startsWith(ServerPath.ROOT)) {
            destinationPath = ServerPath.ROOT;
        }

        final ServerItemTreeDialog treeDialog = new ServerItemTreeDialog(
            getShell(),
            Messages.getString("MoveDialog.SelectFolderDialogTitle"), //$NON-NLS-1$
            destinationPath,
            new VersionedItemSource(repository.getVersionControlClient().getConnection()),
            ServerItemType.ALL_FOLDERS);

        if (treeDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        destinationPath =
            ServerPath.combine(treeDialog.getSelectedServerPath(), ServerPath.getFileName(sourceText.getText()));

        destinationText.setText(destinationPath);
    }

    @Override
    protected void okPressed() {
        try {
            destinationServerPath = ServerPath.canonicalize(destinationText.getText());
        } catch (final ServerPathFormatException e) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("MoveDialog.InvalidServerPathDialogTitle"), //$NON-NLS-1$
                e.getLocalizedMessage());
            return;
        }

        if (!destinationServerPath.startsWith(ServerPath.ROOT)) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("MoveDialog.InvalidServerPathDialogTitle"), //$NON-NLS-1$
                Messages.getString("MoveDialog.InvalidServerPathDialogText")); //$NON-NLS-1$
            return;
        }

        super.okPressed();
    }

    public String getDestinationServerPath() {
        return destinationServerPath;
    }
}
