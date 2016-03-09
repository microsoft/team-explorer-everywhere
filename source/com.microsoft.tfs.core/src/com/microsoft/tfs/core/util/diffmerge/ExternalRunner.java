// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.diffmerge;

import java.io.File;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.formatters.MergeToolArgumentFormatter;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.process.ProcessFinishedHandler;
import com.microsoft.tfs.util.process.ProcessRunner;
import com.microsoft.tfs.util.process.ProcessRunner.ProcessRunnerState;

/**
 * Static methods to handle the creation and completion of external processes
 * launched to compare or merge file content.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class ExternalRunner {
    private static final Log log = LogFactory.getLog(ExternalRunner.class);

    /**
     * Runs an external merge tool to complete the given three way merge. The
     * tool is chosen by the extension of the three way merge's modified file
     * name.
     * <p>
     * You probably don't want to call this method to perform merges. See
     * {@link ThreeWayMerge#beginExternalMerge(Conflict, ExternalTool, ProcessFinishedHandler, OutputStream, OutputStream)}
     * instead.
     *
     * @param twm
     *        the three way merge to run (must not be <code>null</code>)
     * @param tool
     *        the merge tool to use (must not be <code>null</code>)
     * @param finishedHandler
     *        an event handler whose methods are invoked when the process runner
     *        reaches one of its terminal states. The caller would normally
     *        implement the handler to call
     *        {@link #endMerge(ProcessRunner, String)} when the runner reaches
     *        any terminal state. May be null if no state information is
     *        desired.
     * @param capturedStandardOutput
     *        a stream to capture the text written by the child process to its
     *        standard output stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in {@link ProcessRunner}'s Javadoc about
     *        deadlock.</b>
     * @param capturedStandardError
     *        a stream to capture the text written by the child process to its
     *        standard error stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in {@link ProcessRunner}'s Javadoc about
     *        deadlock.</b>
     * @return the merge process runner started to complete the merge
     *         asynchronously. You can query this object for completion. null if
     *         and only if there was no merge tool configured that can handle
     *         this content.
     * @throws ExternalToolException
     *         if the configured merge command or arguments string caused a
     *         problem creating the merge tool.
     */
    public static ProcessRunner beginMerge(
        final ThreeWayMerge twm,
        final ExternalTool tool,
        final ProcessFinishedHandler finishedHandler,
        final OutputStream capturedStandardOutput,
        final OutputStream capturedStandardError) throws ExternalToolException {
        Check.notNull(twm, "twm"); //$NON-NLS-1$
        Check.notNull(tool, "tool"); //$NON-NLS-1$

        Check.isTrue(new File(twm.getTheirFileName()).exists(), "Their file " + twm.getTheirFileName() + " must exist"); //$NON-NLS-1$ //$NON-NLS-2$
        Check.isTrue(new File(twm.getYourFileName()).exists(), "Your file " + twm.getYourFileName() + " must exist"); //$NON-NLS-1$ //$NON-NLS-2$
        Check.isTrue(
            new File(twm.getMergedFileName()).exists() == false,
            "Merged output file " + twm.getMergedFileName() + " must not exist"); //$NON-NLS-1$ //$NON-NLS-2$

        log.info(MessageFormat.format("Beginning external merge for {0}", twm.getYourFileName())); //$NON-NLS-1$

        final String[] arguments = new MergeToolArgumentFormatter().formatArguments(
            tool,
            twm.getTheirFileName(),
            twm.getYourFileName(),
            twm.getBaseFileName(),
            twm.getMergedFileName(),
            twm.getTheirFileLabel(),
            twm.getYourFileLabel(),
            twm.getBaseFileLabel(),
            twm.getMergedFileLabel());

        // Copy the command and arguments into one array.
        final String[] fullCommandAndArguments = combineCommandAndArguments(tool.getCommand(), arguments);

        /*
         * Run the process asynchronously. The call to runAsync() returns
         * immediately, and the ProcessFinishedHandler allows the caller to
         * track progress.
         */
        final ProcessRunner mergeProcessRunner = new ProcessRunner(
            fullCommandAndArguments,
            null,
            null,
            finishedHandler,
            capturedStandardOutput,
            capturedStandardError);

        log.info(MessageFormat.format(
            "Running external merge tool like: {0}", //$NON-NLS-1$
            mergeProcessRunner.getCommandLineForDisplay()));

        ProcessRunner.runAsync(mergeProcessRunner);

        log.info("External merge tool started"); //$NON-NLS-1$

        return mergeProcessRunner;
    }

    /**
     * Ends an external merge that was run via {@link ProcessRunner}. If the
     * runner is not already in a terminal state, this method
     * {@link ProcessRunner#waitForFinish()} and blocks until it enters one.
     * <p>
     * It is safe to call into this method inside a
     * {@link ProcessFinishedHandler} method, because the state of the runner is
     * guaranteed to be a terminal state when these methods are invoked, and
     * this method can proceed immediately if the runner has reached a terminal
     * state.
     *
     * @param mergeProcessRunner
     *        the process runner returned by
     *        {@link #beginMerge(ThreeWayMerge, ExternalTool, ProcessFinishedHandler, OutputStream, OutputStream)}
     *        If null, the merge is a failure and false is returned.
     * @param mergedFileName
     *        the file name of the merge result to check for on disk. Caller
     *        does not need to ensure this file exists (this method does that).
     *        Must not be null.
     * @return true if the merge was a success, false if it was not (and the
     *         merged file was deleted by this method).
     */
    public static boolean endMerge(final ProcessRunner mergeProcessRunner, final String mergedFileName) {
        Check.notNull(mergedFileName, "mergedFileName"); //$NON-NLS-1$

        if (mergeProcessRunner == null) {
            return false;
        }

        final File mergedFile = new File(mergedFileName);

        log.info("Waiting for external merge tool exit via waitForFinish()"); //$NON-NLS-1$

        // Make sure we're in a terminal state.
        mergeProcessRunner.waitForFinish();

        final ProcessRunnerState state = mergeProcessRunner.getState();

        if (state == ProcessRunnerState.INTERRUPTED) {
            log.info("External merge tool runner state was INTERRUPTED"); //$NON-NLS-1$
            mergedFile.delete();
            return false;
        } else if (state == ProcessRunnerState.EXEC_FAILED) {
            log.info("External merge tool runner state was EXEC_FAILED", mergeProcessRunner.getExecutionError()); //$NON-NLS-1$
            mergedFile.delete();
            return false;
        }

        Check.isTrue(state == ProcessRunnerState.COMPLETED, "Process runner returned unknown terminal state " //$NON-NLS-1$
            + state.getClass().getName());

        final int ret = mergeProcessRunner.getExitCode();
        log.info(MessageFormat.format("External merge tool runner state was COMPLETED with exit code {0}", ret)); //$NON-NLS-1$

        if (ret != 0) {
            mergedFile.delete();
            return false;
        }

        if (mergedFile.exists() == false) {
            log.warn(MessageFormat.format(
                "External merge tool didn't create an output file {0}; failed merge", //$NON-NLS-1$
                mergedFileName));
            return false;
        }

        if (mergedFile.length() == 0) {
            log.warn(
                MessageFormat.format(
                    "External merge tool created an empty output file {0}, which isn't good enough; failed merge", //$NON-NLS-1$
                    mergedFileName));
            mergedFile.delete();
            return false;
        }

        log.info("External merge created good merge output file"); //$NON-NLS-1$
        return true;
    }

    /**
     * Combine the given command string and argument strings into one array to
     * pass to something like {@link Runtime#exec(String[])}.
     *
     * @param command
     *        the command (must not be <code>null</code>)
     * @param arguments
     *        the arguments (must not be <code>null</code>)
     * @return a new array containing first the given command then all the
     *         arguments in their original order.
     */
    public static String[] combineCommandAndArguments(final String command, final String[] arguments) {
        Check.notNull(command, "command"); //$NON-NLS-1$
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$

        final String[] fullCommandAndArguments = new String[arguments.length + 1];

        fullCommandAndArguments[0] = command;

        for (int i = 0; i < arguments.length; i++) {
            fullCommandAndArguments[i + 1] = arguments[i];
        }

        return fullCommandAndArguments;
    }
}
