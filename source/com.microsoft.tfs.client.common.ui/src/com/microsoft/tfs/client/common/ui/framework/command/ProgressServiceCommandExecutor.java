// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;

/**
 * <p>
 * An {@link ICommandExecutor} implementation that makes use of the workbench's
 * progress service.
 * </p>
 *
 * <p>
 * In addition, this executor will raise an {@link ErrorDialog} on the if the
 * status when a command is finished meets certain criteria, as determined by
 * {@link ErrorDialogCommandFinishedCallback}.
 * </p>
 */
public class ProgressServiceCommandExecutor extends RunnableContextCommandExecutor {
    /**
     * Creates a new {@link ProgressServiceCommandExecutor}.
     */
    public ProgressServiceCommandExecutor() {
        super(ShellUtils.getWorkbenchShell(), PlatformUI.getWorkbench().getProgressService());
    }
}
