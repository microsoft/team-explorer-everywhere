// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.dialogs;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.wit.SaveWorkItemCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemForm;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemFormHeader;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;
import com.microsoft.tfs.util.Check;

public class WorkItemEditorDialog extends BaseDialog {
    private final TFSServer server;
    private final WorkItem workItem;

    private WorkItemFormHeader workItemFormHeader;
    private WorkItemForm workItemForm;

    private final FieldTracker fieldTracker;
    private final WorkItemStateListener stateListener;

    private final static Log log = LogFactory.getLog(WorkItemEditorDialog.class);

    public WorkItemEditorDialog(final Shell parentShell, final TFSServer server, final WorkItem workItem) {
        super(parentShell);

        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(workItem, "workItem"); //$NON-NLS-1$

        this.server = server;
        this.workItem = workItem;

        fieldTracker = new FieldTracker();
        stateListener = new StateListener();
    }

    @Override
    protected String provideDialogTitle() {
        final String messageFormat = Messages.getString("WorkItemEditorDialog.DialogTitleFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(workItem.getFields().getID()));
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        workItem.open();

        workItemFormHeader = new WorkItemFormHeader(dialogArea, SWT.NONE, workItem, fieldTracker);
        workItemFormHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        workItemForm = new WorkItemForm(dialogArea, SWT.NONE, server, workItem, fieldTracker);
        workItemForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        workItemFormHeader.refresh();
    }

    @Override
    protected void hookAfterButtonsCreated() {
        workItem.addWorkItemStateListener(stateListener);

        if (!workItem.isValid()) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
    }

    @Override
    protected void okPressed() {
        if (!workItem.isValid()) {
            final String messageFormat = Messages.getString("WorkItemEditorDialog.SaveFailedFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, fieldTracker.getMessageFromFirstInvalidField(workItem));

            MessageDialog.openError(getShell(), Messages.getString("WorkItemEditorDialog.ErrorDialogTitle"), message); //$NON-NLS-1$
            return;
        }

        if (workItem.isDirty()) {
            final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getShell());
            final SaveWorkItemCommand command = new SaveWorkItemCommand(workItem);
            final IStatus status = executor.execute(command);

            if (!status.isOK()) {
                return;
            }
        }

        super.okPressed();
    }

    private class StateListener extends WorkItemStateAdapter {
        @Override
        public void validStateChanged(final boolean isValid, final WorkItem workItem) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    final Button button = getButton(IDialogConstants.OK_ID);

                    if (button != null && !button.isDisposed()) {
                        button.setEnabled(isValid);
                    }
                }
            });
        }

        @Override
        public void dirtyStateChanged(final boolean isDirty, final WorkItem workItem) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    final String text =
                        isDirty ? Messages.getString("WorkItemEditorDialog.SaveButtonText") : IDialogConstants.OK_LABEL; //$NON-NLS-1$
                    final Button button = getButton(IDialogConstants.OK_ID);

                    if (button != null && !button.isDisposed()) {
                        button.setText(text);
                    }
                }
            });
        }
    }
}
