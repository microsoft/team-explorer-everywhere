// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.CommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.client.common.framework.command.MultiCommandFinishedCallback;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.util.Check;

public class UICommandFinishedCallbackFactory extends CommandFinishedCallbackFactory {
    protected UICommandFinishedCallbackFactory() {
    }

    /**
     * This will attempt to derive the shell from the current context. It is
     * recommended instead that you use {@link #getDefaultCallback(Shell)}
     * instead.
     *
     * @return A command finished callback that does not participate in UI.
     */
    public static ICommandFinishedCallback getDefaultCallback() {
        return getDefaultCallback(ShellUtils.getWorkbenchShell());
    }

    /**
     * The standard UI command finished callback. This will display a warning or
     * error dialog given the severity.
     *
     * @return A command finished callback that participates in UI.
     */
    public static ICommandFinishedCallback getDefaultCallback(final Shell shell) {
        /* Make sure to include the default non-UI callbacks */
        final ICommandFinishedCallback nonUiCallbacks = CommandFinishedCallbackFactory.getDefaultCallback();

        final ICommandFinishedCallback uiCallbacks =
            MultiCommandFinishedCallback.combine(getConsoleWriterCallback(), getErrorDialogCallback(shell));

        return MultiCommandFinishedCallback.combine(nonUiCallbacks, uiCallbacks);
    }

    public static ICommandFinishedCallback getDefaultNoErrorDialogCallback() {
        /* Make sure to include the default non-UI callbacks */
        final ICommandFinishedCallback nonUiCallbacks = CommandFinishedCallbackFactory.getDefaultCallback();

        final ICommandFinishedCallback uiCallbacks = getConsoleWriterCallback();

        return MultiCommandFinishedCallback.combine(nonUiCallbacks, uiCallbacks);
    }

    public static final ICommandFinishedCallback getConsoleWriterCallback() {
        return new ConsoleWriterCommandFinishedCallback();
    }

    public static final ICommandFinishedCallback getErrorDialogCallback(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        return new ErrorDialogCommandFinishedCallback(shell);
    }
}
