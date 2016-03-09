// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;

import com.microsoft.tfs.util.process.ProcessRunner;
import com.microsoft.tfs.util.process.ProcessRunner.ProcessRunnerState;

/**
 * Helps test code execute external programs to test native implementations.
 */
public abstract class ExecHelpers {
    /**
     * Does the dirty work of creating a side process, executing the given
     * arguments (as they would be passed to {@link Runtime#exec(String[])},
     * reading the output (if desired), trapping exeptions, waiting for the exit
     * code, and returning the code.
     * <p>
     * Provided for our delegates to use.
     *
     * @param args
     *        the arguments to pass to {@link Runtime#exec(String)}
     * @param processOutput
     *        if not null, the process's output (to it's standard out stream) is
     *        captured into this StringBuffer. If null, the process's output is
     *        not read or saved.
     */
    public static int exec(final String[] args, final StringBuffer processOutput) {
        /*
         * If the caller wants it, capture to a stream in memory.
         */
        final ByteArrayOutputStream tempOutput = (processOutput == null) ? null : new ByteArrayOutputStream();

        final ProcessRunner runner = new ProcessRunner(args, null, null, null, tempOutput, null);

        runner.run();

        if (runner.getState() == ProcessRunnerState.EXEC_FAILED) {
            throw new RuntimeException(MessageFormat.format(
                "Error executing external command: {0}", //$NON-NLS-1$
                buildCommandForError(args)), runner.getExecutionError());
        }

        if (runner.getState() == ProcessRunnerState.INTERRUPTED) {
            throw new RuntimeException(MessageFormat.format(
                "Error executing external command (interrupted): {0}", //$NON-NLS-1$
                buildCommandForError(args)));
        }

        if (tempOutput != null) {
            processOutput.append(tempOutput.toString());
        }

        return runner.getExitCode();
    }

    /**
     * Converts the given exec components to a command string suitable for
     * displaying in an error message.
     * <p>
     * Provided for our delegates to use.
     */
    public static String buildCommandForError(final String[] args) {
        // Build an error string that's readable from the array.
        final StringBuffer command = new StringBuffer();

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                command.append(' ');
            }

            command.append("\"" + args[i] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return command.toString();
    }
}
