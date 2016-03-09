// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.client.common.framework.command.helpers.CommandFinishedCallbackHelpers;
import com.microsoft.tfs.client.common.framework.status.UncaughtCommandExceptionStatus;
import com.microsoft.tfs.client.common.ui.framework.console.Console;
import com.microsoft.tfs.client.common.ui.framework.console.ConsoleManager;
import com.microsoft.tfs.client.common.ui.framework.console.ConsoleStream;

/**
 * This is a command finished callback suitable for displaying messages in a
 * console.
 *
 * This will write statuses with the severity {@link IStatus#ERROR} to the
 * "error" stream of the console if it exists and statuses with the severity
 * {@link IStatus#WARNING} to the "warning" stream of the console if it exists.
 * All other messages are written to the default stream.
 */
public class ConsoleWriterCommandFinishedCallback implements ICommandFinishedCallback {
    /**
     * The minimum severity to write to the console (inclusive.)
     */
    private final int minimumSeverity = IStatus.INFO;

    /**
     * The maximum severity to write to the console (inclusive.) Use this to
     * ignore IStatus.CANCEL going to the console (value 8.)
     */
    private final int maximumSeverity = IStatus.ERROR;

    /**
     * Always log uncaught exceptions, regardless of severity.
     */
    private final boolean alwaysLogUncaughtExceptions = true;

    private final static Log log = LogFactory.getLog(ConsoleWriterCommandFinishedCallback.class);

    @Override
    public void onCommandFinished(final ICommand command, final IStatus status) {
        if ((status.getSeverity() >= minimumSeverity && status.getSeverity() <= maximumSeverity)
            || (alwaysLogUncaughtExceptions && status instanceof UncaughtCommandExceptionStatus)) {
            final Console console = ConsoleManager.getDefault().getDefaultConsole();

            if (console != null) {
                ConsoleStream consoleStream;

                if (status.getSeverity() == IStatus.ERROR) {
                    consoleStream = console.getStream("error"); //$NON-NLS-1$
                } else if (status.getSeverity() == IStatus.WARNING) {
                    consoleStream = console.getStream("warning"); //$NON-NLS-1$
                } else {
                    consoleStream = console.getDefaultStream();
                }

                consoleStream.println(CommandFinishedCallbackHelpers.getMessageForStatus(status));

                if (status instanceof UncaughtCommandExceptionStatus && status.getException() != null) {
                    consoleStream.println(status.getException().getLocalizedMessage());
                }
            } else {
                log.error("Could not locate TFS console from Console Manager"); //$NON-NLS-1$
            }
        }
    }
}
