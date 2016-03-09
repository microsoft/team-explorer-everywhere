// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.util.Check;

public abstract class BaseTask implements Task {
    private final Shell shell;

    private ICommandExecutor commandExecutor;

    public BaseTask(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        this.shell = shell;
        commandExecutor = UICommandExecutorFactory.newUICommandExecutor(shell);
    }

    protected Shell getShell() {
        return shell;
    }

    @Override
    public void setCommandExecutor(final ICommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public ICommandExecutor getCommandExecutor() {
        return commandExecutor;
    }
}
