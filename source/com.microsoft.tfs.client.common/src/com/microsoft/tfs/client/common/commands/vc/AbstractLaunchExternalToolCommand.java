// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.framework.command.ExtendedStatus;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.process.ProcessRunner;
import com.microsoft.tfs.util.process.ProcessRunner.ProcessRunnerState;

/**
 * Provides some helper methods for commands which launch external tools.
 *
 * @threadsafety unknown
 */
public abstract class AbstractLaunchExternalToolCommand extends TFSCommand {
    private static final Log log = LogFactory.getLog(AbstractLaunchExternalToolCommand.class);

    /**
     * Creates an {@link IStatus} for the given {@link ProcessRunner}'s
     * (terminal) current state, pulling out error text if the
     * {@link ProcessRunner} failed to launch the command.
     *
     * @param runner
     *        the process runner (must not be <code>null</code>)
     * @return an {@link IStatus}
     */
    protected IStatus makeStatusForProcessRunner(final ProcessRunner runner) {
        Check.notNull(runner, "runner"); //$NON-NLS-1$

        /*
         * Only return an error if the command failed to start. Ignore the other
         * terminal states (interrupted, success).
         */
        if (runner.getState() == ProcessRunnerState.EXEC_FAILED) {
            final String message = "Failed to launch an external program."; //$NON-NLS-1$
            log.warn(message, runner.getExecutionError());

            return new ExtendedStatus(
                new Status(IStatus.WARNING, TFSCommonClientPlugin.PLUGIN_ID, 0, message, runner.getExecutionError()),
                ExtendedStatus.LOG_TO_PLATFORM_LOG
                    | ExtendedStatus.LOG_TO_PRIVATE_LOG
                    | ExtendedStatus.SHOW_MESSAGE_IN_DIALOG
                    | ExtendedStatus.SHOW_MESSAGE_IN_CONSOLE);
        }

        return Status.OK_STATUS;
    }
}
