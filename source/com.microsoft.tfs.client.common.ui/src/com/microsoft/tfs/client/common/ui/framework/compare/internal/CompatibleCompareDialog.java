// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare.internal;

import java.text.MessageFormat;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.compare.CustomCompareEditorInput;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class CompatibleCompareDialog extends BaseDialog {
    private final CompareEditorInput input;

    private final IPropertyChangeListener compareChangeListener = new CompatibleCompareChangeListener();

    public CompatibleCompareDialog(final Shell shell, final CompareEditorInput input) {
        super(shell);

        Check.notNull(input, "input"); //$NON-NLS-1$
        this.input = input;
    }

    @Override
    protected String provideDialogTitle() {
        String title = input.getTitle();

        if (title == null) {
            title = Messages.getString("CompatibleCompareDialog.DialogTitle"); //$NON-NLS-1$
        }

        return title;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        dialogArea.setLayout(new FillLayout());
        input.createContents(dialogArea);
    }

    @Override
    protected void hookAfterButtonsCreated() {
        getButton(IDialogConstants.OK_ID).setText(Messages.getString("CompatibleCompareDialog.OkButtonLabel")); //$NON-NLS-1$
        getButton(IDialogConstants.OK_ID).setEnabled(input.isSaveNeeded());
    }

    @Override
    protected void okPressed() {
        final ICommand saveCommand = new ResourceChangingCommand(new SaveCompareInputCommand(input));
        final IStatus status = new CommandExecutor().execute(saveCommand);

        if (status.isOK()) {
            /*
             * If the input is our CustomCompareEditorInput, then call the
             * setOkPressed method. Otherwise, this is a 3.2-style
             * CompareEditorInput from Eclipse (which did not have the
             * okPressed() method), so just do a noop.
             */
            if (input instanceof CustomCompareEditorInput) {
                ((CustomCompareEditorInput) input).setOKPressed();
            }

            super.okPressed();
        } else {
            ErrorDialog.openError(getShell(), Messages.getString("CompatibleCompareDialog.DialogTitle"), null, status); //$NON-NLS-1$
        }
    }

    @Override
    protected void hookDialogAboutToClose() {
        input.removePropertyChangeListener(compareChangeListener);
    }

    private class CompatibleCompareChangeListener implements IPropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            getButton(IDialogConstants.OK_ID).setEnabled(input.isSaveNeeded());
        }
    }

    private static class SaveCompareInputCommand extends Command {
        private final CompareEditorInput input;

        public SaveCompareInputCommand(final CompareEditorInput input) {
            Check.notNull(input, "input"); //$NON-NLS-1$

            this.input = input;
        }

        @Override
        public String getName() {
            return MessageFormat.format(
                Messages.getString("CompatibleCompareDialog.SaveCommandNameFormat"), //$NON-NLS-1$
                input.getName());
        }

        @Override
        public String getErrorDescription() {
            return MessageFormat.format(
                Messages.getString("CompatibleCompareDialog.SaveCommandErrorFormat"), //$NON-NLS-1$
                input.getName());
        }

        @Override
        public String getLoggingDescription() {
            return MessageFormat.format(
                Messages.getString("CompatibleCompareDialog.SaveCommandNameFormat", LocaleUtil.ROOT), //$NON-NLS-1$
                input.getName());
        }

        @Override
        protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
            input.saveChanges(progressMonitor);

            return Status.OK_STATUS;
        }
    }
}
