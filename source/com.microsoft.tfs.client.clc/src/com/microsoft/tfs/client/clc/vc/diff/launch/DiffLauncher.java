// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff.launch;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.clc.EnvironmentVariables;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.externaltools.CLCTools;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.formatters.CompareToolArgumentFormatter;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolException;
import com.microsoft.tfs.core.util.diffmerge.ExternalRunner;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.process.ProcessRunner;
import com.microsoft.tfs.util.process.ProcessRunner.ProcessRunnerState;

/**
 * Starts the external process specified by the
 * {@link EnvironmentVariables#EXTERNAL_DIFF_COMMAND} environment variable.
 */
public class DiffLauncher {
    private final static Log log = LogFactory.getLog(DiffLauncher.class);

    public DiffLauncher() {
    }

    /**
     * Launches the appropriate external diff tool for the given launch items.
     * The tool to use is specified by the
     * {@link com.microsoft.tfs.client.clc.EnvironmentVariables#EXTERNAL_DIFF_COMMAND}
     * environment variable.
     *
     * @param source
     *        the source diff item (usually shown on the left side of comparison
     *        displays). Not null.
     * @param target
     *        the target diff item (usually shown on the right side of
     *        comparison displays) Not null.
     * @throws ExternalToolException
     *         if the environment variable was not set or was set to an empty
     *         string
     * @throws CLCException
     *         if the {@link DiffLaunchItem}s caused an error creating temp
     *         files or building labels
     */
    public void launchDiff(final DiffLaunchItem source, final DiffLaunchItem target)
        throws ExternalToolException,
            CLCException {
        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(target, "target"); //$NON-NLS-1$

        log.info(MessageFormat.format(
            "Beginning CLC external diff for {0} and {1}", //$NON-NLS-1$
            source.getFilePath(),
            target.getFilePath()));

        final ExternalTool tool = CLCTools.getCompareTool();

        /*
         * The tool performs substitution, then we make one array for process
         * creation.
         */
        final String[] fullCommandAndArguments = ExternalRunner.combineCommandAndArguments(
            tool.getCommand(),
            new CompareToolArgumentFormatter().formatArguments(
                tool,
                source.getFilePath(),
                target.getFilePath(),
                source.getLabel(),
                target.getLabel()));

        /*
         * Run the process synchronously. Let the process use our standard
         * output and error streams.
         */
        final ProcessRunner process = new ProcessRunner(
            fullCommandAndArguments,
            null,
            null,
            null,
            new ProcessRunner.SystemOutputOutputStream(),
            new ProcessRunner.SystemErrorOutputStream());

        log.info(MessageFormat.format("Running CLC external diff like: {0}", process.getCommandLineForDisplay())); //$NON-NLS-1$

        process.run();

        if (process.getState() == ProcessRunnerState.EXEC_FAILED) {
            final String messageFormat = Messages.getString("DiffLauncher.CouldNotStartExternalToolFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, process.getCommandLineForDisplay(), process.getExecutionError());

            log.warn(message, process.getExecutionError());
            throw new CLCException(message);
        }

        log.info(MessageFormat.format("CLC external diff exited with status {0}", process.getExitCode())); //$NON-NLS-1$
    }
}
