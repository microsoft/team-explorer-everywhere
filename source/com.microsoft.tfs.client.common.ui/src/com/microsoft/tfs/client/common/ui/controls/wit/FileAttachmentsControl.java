// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.wit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;

import com.microsoft.tfs.client.common.commands.wit.DownloadFileAttachmentCommand;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.ElementLongComparator;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.client.common.ui.framework.tooltip.IToolTipProvider;
import com.microsoft.tfs.client.common.ui.wit.form.BaseWITComponentControl;
import com.microsoft.tfs.client.common.ui.wit.form.FileAttachmentDialog;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;
import com.microsoft.tfs.core.clients.workitem.files.Attachment;
import com.microsoft.tfs.core.clients.workitem.files.AttachmentFactory;
import com.microsoft.tfs.util.temp.TempStorageService;

public class FileAttachmentsControl extends BaseWITComponentControl {
    private WorkItemStateListener workItemStateListener;

    private IAction openAction;
    private IAction openInBrowserAction;
    private IAction openLocallyAction;
    private IAction downloadToAction;
    private IAction addAttachmentAction;
    private IAction deleteAttachmentAction;
    private IAction copyUrlToClipboardAction;

    private Button openButton;
    private Button downloadButton;
    private Button addButton;
    private Button deleteButton;

    /*
     * The date format used to display dates in the tooltips.
     */
    private final DateFormat tooltipDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);

    private String lastDownloadDirectory;

    public FileAttachmentsControl(
        final Composite parent,
        final int style,
        final TFSServer server,
        final WorkItem workItemInput) {
        super(parent, style | SWT.MULTI, server, workItemInput);
    }

    @Override
    protected void hookInit() {
        // Create and wire all actions for this control.
        setupActions();

        // handle initial validation and populate table with the work item.
        bindWorkItemToTable();

        final WorkItem workItem = getWorkItem();
        workItemStateListener = new WorkItemStateAdapter() {
            @Override
            public void saved(final WorkItem workItem) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }

            @Override
            public void synchedToLatest(final WorkItem workItem) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        };

        workItem.addWorkItemStateListener(workItemStateListener);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                workItem.removeWorkItemStateListener(workItemStateListener);
            }
        });
    }

    private void performOpen(DownloadAttachmentOpenType openType) {
        final Attachment attachment = (Attachment) getSelectedItem();

        if (attachment.isNewlyCreated()) {
            openType = DownloadAttachmentOpenType.LOCAL;
        } else if (openType == DownloadAttachmentOpenType.USE_PREFERENCE) {
            openType = DownloadAttachmentOpenType.getPreferredOpenType();
        }

        String toLaunch = null;

        if (openType == DownloadAttachmentOpenType.BROWSER) {
            final URL downloadURL = ((Attachment) getSelectedItem()).getURL();
            toLaunch = downloadURL.toExternalForm();
        } else {
            if (attachment.isNewlyCreated()) {
                toLaunch = attachment.getLocalFile().getAbsolutePath();
            } else {
                String extension = null;
                final int ix = attachment.getFileName().lastIndexOf("."); //$NON-NLS-1$
                if (ix != -1) {
                    extension = attachment.getFileName().substring(ix);
                }

                try {
                    toLaunch = TempStorageService.getInstance().createTempFile(extension).getAbsolutePath();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }

                final DownloadFileAttachmentCommand downloadCommand = new DownloadFileAttachmentCommand(
                    attachment.getURL(),
                    new File(toLaunch),
                    getServer().getConnection());

                final IStatus downloadStatus =
                    UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(downloadCommand);

                if (!downloadStatus.isOK() && downloadStatus.getSeverity() != IStatus.CANCEL) {
                    return;
                }
            }
        }

        Launcher.launch(toLaunch);
    }

    private void performAddAttachment() {
        final FileAttachmentDialog dialog = new FileAttachmentDialog(getShell());
        if (dialog.open() == IDialogConstants.OK_ID) {
            final File[] files = dialog.getSelectedFiles();
            for (int i = 0; i < files.length; i++) {
                final Attachment attachment = AttachmentFactory.newAttachment(files[i], dialog.getComment());
                getWorkItem().getAttachments().add(attachment);
            }
            refresh();
        }
    }

    private void performDeleteAttachments() {
        if (!MessageBoxHelpers.dialogConfirmPrompt(
            getShell(),
            Messages.getString("FileAttachmentsControl.ConfirmDeleteDialogTitle"), //$NON-NLS-1$
            Messages.getString("FileAttachmentsControl.ConfirmDeleteDialogBody"))) //$NON-NLS-1$
        {
            return;
        }

        final Object[] selectedAttachments = getSelectedItems();
        for (int i = 0; i < selectedAttachments.length; i++) {
            getWorkItem().getAttachments().remove((Attachment) selectedAttachments[i]);
        }

        refresh();
    }

    private void performDownloadTo() {
        final DirectoryDialog dialog = new DirectoryDialog(getShell());

        if (lastDownloadDirectory != null) {
            dialog.setFilterPath(lastDownloadDirectory);
        }

        final String directoryPath = dialog.open();
        if (directoryPath != null) {
            lastDownloadDirectory = directoryPath;

            final Object[] selectedAttachments = getSelectedItems();
            for (int i = 0; i < selectedAttachments.length; i++) {
                final Attachment attachment = (Attachment) selectedAttachments[i];
                final File targetFile = new File(directoryPath, attachment.getFileName());
                if (targetFile.exists()) {
                    final String title = Messages.getString("FileAttachmentsControl.ConfirmOverwriteDialogTitle"); //$NON-NLS-1$
                    final String messageFormat =
                        Messages.getString("FileAttachmentsControl.ConfirmOverwriteDialogTextFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, targetFile.getAbsolutePath());

                    if (!MessageBoxHelpers.dialogConfirmPrompt(getShell(), title, message)) {
                        continue;
                    }
                }

                final DownloadFileAttachmentCommand downloadCommand =
                    new DownloadFileAttachmentCommand(attachment.getURL(), targetFile, getServer().getConnection());

                UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(downloadCommand);
            }
        }
    }

    private void performCopyURLToClipboard() {
        final Attachment attachment = (Attachment) getSelectedItem();
        if (attachment.isNewlyCreated()) {
            UIHelpers.copyToClipboard(attachment.getLocalFile().getAbsolutePath());
        } else {
            UIHelpers.copyToClipboard(attachment.getURL().toExternalForm());
        }
    }

    @Override
    protected void handleSelectionChanged(final Object[] selectedItems) {
        final boolean oneAttachmentSelected = (selectedItems.length == 1);
        final boolean oneOrMoreAttachmentsSelected = (selectedItems.length > 0);

        boolean oneOrMoreSelectedAndAllAreNonNew = oneOrMoreAttachmentsSelected;
        if (oneOrMoreSelectedAndAllAreNonNew) {
            for (int i = 0; i < selectedItems.length; i++) {
                final Attachment fileAttachment = (Attachment) selectedItems[i];
                if (fileAttachment.isNewlyCreated()) {
                    oneOrMoreSelectedAndAllAreNonNew = false;
                    break;
                }
            }
        }

        openAction.setEnabled(oneAttachmentSelected);
        openInBrowserAction.setEnabled(oneAttachmentSelected);
        openLocallyAction.setEnabled(oneAttachmentSelected);

        downloadToAction.setEnabled(oneOrMoreSelectedAndAllAreNonNew);

        addAttachmentAction.setEnabled(true);
        deleteAttachmentAction.setEnabled(oneOrMoreAttachmentsSelected);

        copyUrlToClipboardAction.setEnabled(oneAttachmentSelected);

        openButton.setEnabled(oneAttachmentSelected);
        downloadButton.setEnabled(oneOrMoreSelectedAndAllAreNonNew);
        addButton.setEnabled(true);
        deleteButton.setEnabled(oneOrMoreAttachmentsSelected);
    }

    @Override
    protected IToolTipProvider getToolTipProvider() {
        return new IToolTipProvider() {
            @Override
            public String getToolTipText(final Object element) {
                final Attachment attachment = (Attachment) element;

                final String attachComment = attachment.getComment() != null ? attachment.getComment() : ""; //$NON-NLS-1$
                final String attachDate = tooltipDateFormatter.format(attachment.getAttachmentAddedDate());
                final String modifiedDate = tooltipDateFormatter.format(attachment.getLastModifiedDate());

                final String commentFormat = Messages.getString("FileAttachmentsControl.CommentTooltipFormat"); //$NON-NLS-1$
                final String comment = MessageFormat.format(commentFormat, attachComment);

                final String attachDateFormat = Messages.getString("FileAttachmentsControl.AttachDateTooltipFormat"); //$NON-NLS-1$
                final String attDate = MessageFormat.format(attachDateFormat, attachDate);

                final String modifiedDateFormat =
                    Messages.getString("FileAttachmentsControl.ModifiedDateTooltipFormat"); //$NON-NLS-1$
                final String modDate = MessageFormat.format(modifiedDateFormat, modifiedDate);

                final StringBuffer buffer = new StringBuffer();
                buffer.append(comment);
                buffer.append(NEWLINE);
                buffer.append(attDate);
                buffer.append(NEWLINE);
                buffer.append(modDate);

                return buffer.toString();
            }
        };
    }

    @Override
    protected void createButtons(final Composite parent) {
        openButton =
            createButton(parent, Messages.getString("FileAttachmentsControl.OpenButtonText"), new SelectionAdapter() //$NON-NLS-1$
        {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    performOpen(DownloadAttachmentOpenType.USE_PREFERENCE);
                }
            });

        downloadButton =
            createButton(parent, Messages.getString("FileAttachmentsControl.DownloadButtonText"), new SelectionAdapter() //$NON-NLS-1$
        {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    performDownloadTo();
                }
            });

        addButton =
            createButton(parent, Messages.getString("FileAttachmentsControl.AddButtonText"), new SelectionAdapter() //$NON-NLS-1$
        {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    performAddAttachment();
                }
            });

        deleteButton =
            createButton(parent, Messages.getString("FileAttachmentsControl.DeleteButtonText"), new SelectionAdapter() //$NON-NLS-1$
        {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    performDeleteAttachments();
                }
            });
    }

    @Override
    protected int getNumberOfButtons() {
        return 4;
    }

    private void setupActions() {
        openAction = new Action() {
            @Override
            public void run() {
                performOpen(DownloadAttachmentOpenType.USE_PREFERENCE);
            }
        };
        openAction.setText(Messages.getString("FileAttachmentsControl.OpenActionText")); //$NON-NLS-1$

        openInBrowserAction = new Action() {
            @Override
            public void run() {
                performOpen(DownloadAttachmentOpenType.BROWSER);
            }
        };
        openInBrowserAction.setText(Messages.getString("FileAttachmentsControl.OpenInBrowserActionText")); //$NON-NLS-1$

        openLocallyAction = new Action() {
            @Override
            public void run() {
                performOpen(DownloadAttachmentOpenType.LOCAL);
            }
        };
        openLocallyAction.setText(Messages.getString("FileAttachmentsControl.OpenLocallyActionText")); //$NON-NLS-1$

        downloadToAction = new Action() {
            @Override
            public void run() {
                performDownloadTo();
            }
        };
        downloadToAction.setText(Messages.getString("FileAttachmentsControl.DownloadActionText")); //$NON-NLS-1$

        addAttachmentAction = new Action() {
            @Override
            public void run() {
                performAddAttachment();
            }
        };
        addAttachmentAction.setText(Messages.getString("FileAttachmentsControl.AddAttachmentButtonText")); //$NON-NLS-1$

        deleteAttachmentAction = new Action() {
            @Override
            public void run() {
                performDeleteAttachments();
            }
        };
        deleteAttachmentAction.setText(Messages.getString("FileAttachmentsControl.DeleteAttachmentButtonText")); //$NON-NLS-1$

        copyUrlToClipboardAction = new Action() {
            @Override
            public void run() {
                performCopyURLToClipboard();
            }
        };
        copyUrlToClipboardAction.setText(Messages.getString("FileAttachmentsControl.CopyUrlActionText")); //$NON-NLS-1$
    }

    @Override
    protected Object[] getItemsFromWorkItem(final WorkItem workItem) {
        final ArrayList<Attachment> attachments = new ArrayList<Attachment>();

        for (final Attachment attachment : workItem.getAttachments()) {
            if (!attachment.isPendingDelete()) {
                attachments.add(attachment);
            }
        }

        return attachments.toArray();
    }

    @Override
    protected String[] getTableColumnNames() {
        return new String[] {
            Messages.getString("FileAttachmentsControl.ColumnNameName"), //$NON-NLS-1$
            Messages.getString("FileAttachmentsControl.ColumnNameSize"), //$NON-NLS-1$
            Messages.getString("FileAttachmentsControl.ColumnNameComment") //$NON-NLS-1$
        };
    }

    @Override
    protected Image getImageForColumn(final Object element, final int columnIndex) {
        return null;
    }

    @Override
    protected String getTextForColumn(final Object element, final int columnIndex) {
        final Attachment attachment = (Attachment) element;
        switch (columnIndex) {
            case 0:
                return attachment.getFileName();

            case 1:
                return attachment.getFileSizeAsString();

            case 2:
                final String s = attachment.getComment();
                return (s == null ? "" : s); //$NON-NLS-1$
        }
        return null;
    }

    @Override
    protected void handleItemDoubleClick(final Object selectedItem) {
        performOpen(DownloadAttachmentOpenType.USE_PREFERENCE);
    }

    @Override
    protected void addSorting(final TableViewer tableViewer) {
        final TableViewerSorter sorter = new TableViewerSorter(tableViewer);
        sorter.setComparator(1, new ElementLongComparator() {
            @Override
            protected long getLong(final Object element) {
                return ((Attachment) element).getFileSize();
            }
        });
        tableViewer.setSorter(sorter);
    }

    @Override
    protected void fillMenuBeforeShow(final IMenuManager manager) {
        manager.add(openAction);
        manager.add(downloadToAction);
        manager.add(addAttachmentAction);
        manager.add(deleteAttachmentAction);

        manager.add(new Separator());

        final DownloadAttachmentOpenType preferredOpenType = DownloadAttachmentOpenType.getPreferredOpenType();
        if (DownloadAttachmentOpenType.BROWSER == preferredOpenType) {
            manager.add(openLocallyAction);
        } else {
            manager.add(openInBrowserAction);
        }

        manager.add(copyUrlToClipboardAction);
    }
}
