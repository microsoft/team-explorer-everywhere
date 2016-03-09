// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.core.clients.workitem.files.FileAttachmentMaxLengths;
import com.microsoft.tfs.core.clients.workitem.files.LocalFileStatus;
import com.microsoft.tfs.core.clients.workitem.files.WorkItemAttachmentUtils;

public class FileAttachmentDialog extends BaseDialog {
    private Label attachmentLabel;
    private Text filePathText;
    private Text commentText;
    private File[] selectedFiles;
    private String comment;
    private boolean multiMode = false;

    public FileAttachmentDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("FileAttachmentDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        dialogArea.setLayout(layout);

        attachmentLabel = new Label(dialogArea, SWT.NONE);
        attachmentLabel.setText(Messages.getString("FileAttachmentDialog.AttachmentLabelText")); //$NON-NLS-1$
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
        attachmentLabel.setLayoutData(gd);

        filePathText = new Text(dialogArea, SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        filePathText.setLayoutData(gd);

        final Button browseButton = new Button(dialogArea, SWT.NONE);
        browseButton.setText(Messages.getString("FileAttachmentDialog.BrowseButtonText")); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
                final String filepath = dialog.open();
                if (filepath != null) {
                    final String[] filenames = dialog.getFileNames();
                    final String filterPath = dialog.getFilterPath();
                    selectedFiles = new File[filenames.length];

                    for (int i = 0; i < filenames.length; i++) {
                        if (filterPath != null && filterPath.trim().length() > 0) {
                            selectedFiles[i] = new File(filterPath, filenames[i]);
                        } else {
                            selectedFiles[i] = new File(filenames[i]);
                        }
                    }

                    if (filenames.length == 1) {
                        attachmentLabel.setText(Messages.getString("FileAttachmentDialog.AttachmentLabelText")); //$NON-NLS-1$

                        filePathText.setText(selectedFiles[0].getAbsolutePath());
                        filePathText.setEditable(true);
                        multiMode = false;
                    } else {
                        attachmentLabel.setText(Messages.getString("FileAttachmentDialog.MutliAttachmentsLabelText")); //$NON-NLS-1$

                        final StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < selectedFiles.length; i++) {
                            sb.append("\"" + selectedFiles[i].getAbsolutePath() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
                        }

                        filePathText.setText(sb.toString());
                        filePathText.setEditable(false);
                        multiMode = true;
                    }
                }
            }
        });

        final Label commentLabel = new Label(dialogArea, SWT.NONE);
        commentLabel.setText(Messages.getString("FileAttachmentDialog.CommentLabelText")); //$NON-NLS-1$
        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        commentLabel.setLayoutData(gd);

        commentText = new Text(dialogArea, SWT.BORDER);
        commentText.setTextLimit(FileAttachmentMaxLengths.COMMENT_MAX_LENGTH);
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
        commentText.setLayoutData(gd);
    }

    @Override
    protected void okPressed() {
        if (!multiMode) {
            final String inputFilePath = filePathText.getText().trim();
            if (inputFilePath.length() == 0) {
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("FileAttachmentDialog.ErrorDialogTitle"), //$NON-NLS-1$
                    Messages.getString("FileAttachmentDialog.ErrorDialogText")); //$NON-NLS-1$
                return;
            }
            final File file = new File(inputFilePath);

            final LocalFileStatus status = WorkItemAttachmentUtils.validateLocalFileForUpload(file);

            if (status != LocalFileStatus.VALID) {
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("FileAttachmentDialog.ErrorDialogTitle"), //$NON-NLS-1$
                    status.getErrorMessage(file, inputFilePath));
                return;
            }

            selectedFiles = new File[] {
                file
            };
        } else {
            for (int i = 0; i < selectedFiles.length; i++) {
                final LocalFileStatus status = WorkItemAttachmentUtils.validateLocalFileForUpload(selectedFiles[i]);

                if (status != LocalFileStatus.VALID) {
                    MessageBoxHelpers.errorMessageBox(
                        getShell(),
                        Messages.getString("FileAttachmentDialog.ErrorDialogTitle"), //$NON-NLS-1$
                        status.getErrorMessage(selectedFiles[i], null));
                    return;
                }
            }
        }

        comment = commentText.getText();

        super.okPressed();
    }

    public File[] getSelectedFiles() {
        return selectedFiles;
    }

    public String getComment() {
        return comment;
    }
}
