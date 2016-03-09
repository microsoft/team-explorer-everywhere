// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.util.Check;

/**
 * This is an abstract command executor class. It exists only to add the various
 * UI-related {@link ICommandFinishedCallback}s.
 */
public abstract class AbstractUICommandExecutor extends CommandExecutor {
    public AbstractUICommandExecutor(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        setCommandFinishedCallback(UICommandFinishedCallbackFactory.getDefaultCallback(shell));
    }
}
