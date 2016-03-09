// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.filemodification;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.ui.framework.command.ProgressMonitorDialogCommandExecutor;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationAdvisor;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationOptionsProvider;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationStatusReporter;

/**
 * Provides configuration information specific to the UI Eclipse plug-in for
 * file modification processing by the non-UI Eclipse plug-in. Options are
 * supplied from user preferences, the shell is the workbench's shell, and the
 * status reporter reports errors via pop-up dialogs.
 *
 * @threadsafety unknown
 */
public class TFSFileModificationUIAdvisor implements TFSFileModificationAdvisor {
    /**
     * {@inheritDoc}
     */
    @Override
    public TFSFileModificationOptionsProvider getOptionsProvider(final boolean attemptUi, final Object shell) {
        final Shell displayShell = getShell(attemptUi, shell);

        if (displayShell == null) {
            return null;
        }

        return new TFSFileModificationUIOptionsProvider(displayShell);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandExecutor getSynchronousCommandExecutor(final boolean attemptUi, final Object shell) {
        final Shell displayShell = getShell(attemptUi, shell);

        if (displayShell == null) {
            return null;
        }

        return new ProgressMonitorDialogCommandExecutor(displayShell);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TFSFileModificationStatusReporter getStatusReporter(final boolean attemptUi, final Object shell) {
        final Shell displayShell = getShell(attemptUi, shell);

        if (displayShell == null) {
            return null;
        }

        return new TFSFileModificationUIStatusReporter(displayShell);
    }

    private Shell getShell(final boolean attemptUi, final Object shell) {
        if (attemptUi == false || (shell != null && !(shell instanceof Shell))) {
            return null;
        }

        Shell displayShell = (Shell) shell;

        if (displayShell == null) {
            final Display display = Display.getDefault();

            final Shell[] queriedShell = new Shell[1];
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    queriedShell[0] = Display.getDefault().getActiveShell();
                }
            });
            displayShell = queriedShell[0];
        }

        if (displayShell == null || displayShell.isDisposed()) {
            return null;
        }

        return displayShell;
    }
}
