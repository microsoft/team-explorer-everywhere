// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare.internal;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class CompatibleCompareUI {
    private static final Log log = LogFactory.getLog(CompatibleCompareUI.class);

    public static void openCompareDialog(final CompareEditorInput input) {
        Shell shell = null;
        final IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (activeWindow != null) {
            shell = ShellUtils.getBestParent(activeWindow.getShell());
        }

        if (shell == null) {
            log.error(
                MessageFormat.format(
                    "Could not open compare dialog for {0}, no active workbench window shell found", //$NON-NLS-1$
                    input.getName()));
            return;
        }

        /* Prepare the compare input */
        final PrepareCompareCommand prepareCommand = new PrepareCompareCommand(input);
        final IStatus prepareStatus = UICommandExecutorFactory.newUICommandExecutor(shell).execute(prepareCommand);

        if (!prepareStatus.isOK()) {
            return;
        }

        if (input.getCompareResult() == null) {
            MessageDialog.openInformation(
                shell,
                Messages.getString("CompatibleCompareUI.CompareNoDifferencesTitle"), //$NON-NLS-1$
                Messages.getString("CompatibleCompareUI.CompareNoDifferencesMessage")); //$NON-NLS-1$
            return;
        }

        final CompatibleCompareDialog dialog = new CompatibleCompareDialog(shell, input);
        dialog.open();
    }

    private static class PrepareCompareCommand extends Command {
        private final CompareEditorInput input;

        public PrepareCompareCommand(final CompareEditorInput input) {
            Check.notNull(input, "input"); //$NON-NLS-1$

            this.input = input;
        }

        @Override
        public String getName() {
            return MessageFormat.format(
                Messages.getString("CompatibleCompareUI.PrepareCommandNameFormat"), //$NON-NLS-1$
                input.getName());
        }

        @Override
        public String getErrorDescription() {
            return Messages.getString("CompatibleCompareUI.PrepareCommandErrorMessage"); //$NON-NLS-1$
        }

        @Override
        public String getLoggingDescription() {
            return MessageFormat.format(
                Messages.getString("CompatibleCompareUI.PrepareCommandNameFormat", LocaleUtil.ROOT), //$NON-NLS-1$
                input.getName());
        }

        @Override
        protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
            input.run(progressMonitor);

            if (input.getMessage() != null) {
                throw new Exception(input.getMessage());
            }

            return Status.OK_STATUS;
        }
    }
}
