// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.process;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Runs an external process, especially designed to be run in a new thread via
 * {@link ProcessRunner#runAsync(ProcessRunner)}, but can be run directly via
 * {@link ProcessRunner#run()}.
 * <p>
 * {@link #run()} blocks until the process completes.
 * {@link #runAsync(ProcessRunner)} runs the given runner in a new thread that
 * can be interrupted via {@link ProcessRunner#interrupt()}. Its state may also
 * be queried while it is running.
 * <p>
 *
 * @threadsafety thread-safe
 */
public class ProcessRunner implements Runnable {
    private static final Log log = LogFactory.getLog(ProcessRunner.class);

    /**
     * An {@link OutputStream} that simply writes everything to
     * {@link System#err}.
     */
    public static class SystemErrorOutputStream extends OutputStream {
        public SystemErrorOutputStream() {
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
            System.err.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.err.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            System.err.write(b, off, len);
        }

        @Override
        public void write(final int b) throws IOException {
            System.err.write(b);
        }
    }

    /**
     * An {@link OutputStream} that simply writes everything to
     * {@link System#out}.
     */
    public static class SystemOutputOutputStream extends OutputStream {
        public SystemOutputOutputStream() {
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.out.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            System.out.write(b, off, len);
        }

        @Override
        public void write(final int b) throws IOException {
            System.out.write(b);
        }
    }

    public static class ProcessRunnerState extends TypesafeEnum {
        public ProcessRunnerState(final int value) {
            super(value);
        }

        /**
         * The process runner has been constructed but
         * {@link ProcessRunner#run()} has not been invoked.
         * <p>
         * This is the initial state of all process runners.
         */
        public final static ProcessRunnerState NEW = new ProcessRunnerState(0);

        /**
         * {@link ProcessRunner#run()} has been invoked, but the process has not
         * completed.
         * <p>
         * This is an intermediate state.
         */
        public final static ProcessRunnerState RUNNING = new ProcessRunnerState(1);

        /**
         * {@link ProcessRunner#run()} was invoked but could not create the
         * process. Call {@link ProcessRunner#getError()} to get the exception
         * that resulted in this state.
         * <p>
         * This is a terminal state.
         */
        public final static ProcessRunnerState EXEC_FAILED = new ProcessRunnerState(2);

        /**
         * The process was created and run but the thread that is running this
         * {@link ProcessRunner} has been interrupted, so no exit code was read
         * from the process.
         * <p>
         * This is a terminal state.
         */
        public final static ProcessRunnerState INTERRUPTED = new ProcessRunnerState(3);

        /**
         * The process was created and run and exited with a valid exit code
         * (which may be an error). Call {@link ProcessRunner#getExitCode()} to
         * get the exit code.
         * <p>
         * This is a terminal state.
         */
        public final static ProcessRunnerState COMPLETED = new ProcessRunnerState(4);
    }

    /**
     * The external process we're running (which is null until this runnable is
     * run).
     */
    private Process process;

    /**
     * The state of this runner.
     */
    private ProcessRunnerState state;

    /**
     * The commands that will be run via {@link Runtime#exec(String[])}.
     */
    private final String[] commands;

    /**
     * Environment strings to pass to {@link Runtime#exec(String[])}. May be
     * null.
     */
    private final String[] environment;

    /**
     * Working directory to pass to {@link Runtime#exec(String[])}. May be null.
     */
    private final File workingDirectory;

    /**
     * Invoked when the process's state is set to one of terminal states. May be
     * null.
     */
    private final ProcessFinishedHandler finishedHandler;

    /**
     * Contains the stream the user wants to fill with the captured standard
     * output. May be null (then standard output from the child process is
     * consumed but not stored).
     */
    private final OutputStream capturedStandardOutput;

    /**
     * Contains the stream the user wants to fill with the captured standard
     * error. May be null (then standard error from the child process is
     * consumed but not stored).
     */
    private final OutputStream capturedStandardError;

    /**
     * The new thread this runner is running in (is null if no new thread was
     * created to run this process runner).
     */
    private Thread asyncThread;

    /**
     * Holds a command line string for display to the user in error or log
     * cases. This string is not actually run via {@link Runtime#exec(String)};
     * only the String[] version is invoked by this class.
     */
    final private String commandLineForDisplay;

    /**
     * Filled in if run() resulted in an exception that caused the external
     * process to fail to run or caused the exit code to be unreadable.
     */
    private Throwable error;

    /**
     * The exit code of the process.
     */
    private int exitCode = -1;

    /**
     * Used to name the IO reader threads we spawn for better debug log
     * messages. Incremented on every access.
     */
    private static long threadID = 0;

    /**
     * Constructs a process runner that will run the given commands via
     * {@link Runtime#exec(String[])} when {@link ProcessRunner#run()} is
     * invoked.
     *
     * @param commands
     *        the commands and arguments (passed directly to
     *        {@link Runtime#exec(String[])}) to run. Not null but may be empty
     *        (no process is created but command state evolves normally).
     * @param environment
     *        an optional array of environment variables to use in the new
     *        process. See {@link Runtime#exec(String[], String[], File)}). May
     *        be null.
     * @param workingDirectory
     *        an optional working directory to use in the new process. See
     *        {@link Runtime#exec(String[], String[], File)}). May be null.
     * @param completedHandler
     *        a {@link ProcessFinishedHandler} whose methods are invoked when
     *        the process runner reaches one of its terminal states.
     *        <p>
     *        This method is invoked in the context of the thread running the
     *        ProcessRunner, which may be the calling thread (
     *        {@link ProcessRunner#run()} was called directly) or may be another
     *        ({@link ProcessRunner#runAsync(ProcessRunner)} was called).
     *        <p>
     *        Pass null if you wish to poll for state or don't care about state
     *        at all.
     */
    public ProcessRunner(
        final String[] commands,
        final String[] environment,
        final File workingDirectory,
        final ProcessFinishedHandler completedHandler) {
        Check.notNull(commands, "commands"); //$NON-NLS-1$

        this.commands = commands;
        this.environment = environment;
        this.workingDirectory = workingDirectory;
        finishedHandler = completedHandler;
        capturedStandardOutput = null;
        capturedStandardError = null;

        commandLineForDisplay = makeCommandLineForDisplay(commands);
        state = ProcessRunnerState.NEW;
    }

    /**
     * Constructs a process runner that will run the given commands via
     * {@link Runtime#exec(String[])} when {@link ProcessRunner#run()} is
     * invoked.
     * <p>
     * <b>Buffer Warning</b>
     * <p>
     * If you pass OutputStreams to accept the child process's output, take care
     * not to use it in a way that causes the process runner to be unable to
     * write to it for the duration of the runner's life (until it has reached a
     * terminal state). Doing so might cause deadlock (for example, process
     * runner is waiting to write to the stream while you're waiting to read
     * from it).
     *
     * @param commands
     *        the commands and arguments (passed directly to
     *        {@link Runtime#exec(String[])}) to run. Not null but may be empty
     *        (no process is created but command state evolves normally).
     * @param environment
     *        an optional array of environment variables to use in the new
     *        process. See {@link Runtime#exec(String[], String[], File)}). May
     *        be null.
     * @param workingDirectory
     *        an optional working directory to use in the new process. See
     *        {@link Runtime#exec(String[], String[], File)}). May be null.
     * @param completedHandler
     *        a {@link ProcessFinishedHandler} whose methods are invoked when
     *        the process runner reaches one of its terminal states.
     *        <p>
     *        This method is invoked in the context of the thread running the
     *        ProcessRunner, which may be the calling thread (
     *        {@link ProcessRunner#run()} was called directly) or may be another
     *        ({@link ProcessRunner#runAsync(ProcessRunner)} was called).
     *        <p>
     *        Pass null if you wish to poll for state or don't care about state
     *        at all.
     * @param capturedStandardOutput
     *        a stream to capture the text written by the child process to its
     *        standard output stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in this method's Javadoc about deadlock.</b>
     * @param capturedStandardError
     *        a stream to capture the text written by the child process to its
     *        standard error stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in this method's Javadoc about deadlock.</b>
     */
    public ProcessRunner(
        final String[] commands,
        final String[] environment,
        final File workingDirectory,
        final ProcessFinishedHandler completedHandler,
        final OutputStream capturedStandardOutput,
        final OutputStream capturedStandardError) {
        Check.notNull(commands, "commands"); //$NON-NLS-1$

        this.commands = commands;
        this.environment = environment;
        this.workingDirectory = workingDirectory;
        finishedHandler = completedHandler;
        this.capturedStandardOutput = capturedStandardOutput;
        this.capturedStandardError = capturedStandardError;

        commandLineForDisplay = makeCommandLineForDisplay(commands);
        state = ProcessRunnerState.NEW;
    }

    /**
     * Causes the process runner to stop waiting for the external process to
     * complete if it is currently running in another thread. Does not terminate
     * the external process.
     * <p>
     * Do not call this method if you did not start the process runner via
     * {@link #runAsync(ProcessRunner)}. You will get an
     * {@link IllegalStateException} if you do.
     * <p>
     * If the process is still in the initial state (
     * {@link ProcessRunnerState#NEW}), an {@link IllegalStateException} is
     * thrown. If the process is in any other state, no exception is thrown.
     * <p>
     * The terminal state of the runner may not be
     * {@link ProcessRunnerState#INTERRUPTED} even if this method is invoked.
     * For example, the process might complete before this method completes, and
     * the state of the runner would become {@link ProcessRunnerState#COMPLETED}
     * instead. If the process failed to start in the first place, the state
     * would remain {@link ProcessRunnerState#EXEC_FAILED}.
     *
     * @throws IllegalStateException
     *         if this method was invoked when the runner's state was (the
     *         runner's state is {@link ProcessRunnerState#NEW}, or if the
     *         runner was not run via {@link #runAsync(ProcessRunner)}.
     */
    public synchronized void interrupt() {
        if (asyncThread == null) {
            throw new IllegalStateException("This runner was not started via runAsync() so it may not be interrupted"); //$NON-NLS-1$
        }

        if (state == ProcessRunnerState.NEW) {
            throw new IllegalStateException("A process runner cannot be interrupted before it is started"); //$NON-NLS-1$
        }

        /*
         * Interrupting our controlling thread will cause an interrupted
         * exception in Process.waitFor(), if the thread is currently blocking
         * there.
         *
         * If waitFor() has already exited, then the status will end up being
         * COMPLETED.
         */
        asyncThread.interrupt();
    }

    /**
     * Waits for the external process to finish running or be interrupted.
     * Returns only when the runner's state is one of the terminal states.
     * Returns immediately if the process has already finished.
     *
     * @throws IllegalStateException
     *         if this method was invoked when the runner was not started via
     *         {@link #runAsync(ProcessRunner)}.
     */
    public void waitForFinish() {
        synchronized (this) {
            if (asyncThread == null) {
                throw new IllegalStateException(
                    "This runner was not started via runAsync() so you can't wait for it to finish"); //$NON-NLS-1$
            }

            if (isFinished()) {
                return;
            }

            try {
                this.wait();
            } catch (final InterruptedException e) {
                /*
                 * Ignore. Since this method is never run in the context of the
                 * async thread, this is not the interrupted exception that's
                 * caused when interrupt() is called.
                 */
            }
        }
    }

    /**
     * Starts the given process runner in its own thread, returning immediately.
     * Check on the runner's state through its public methods or pass a
     * {@link ProcessFinishedHandler} to the runner on creation.
     *
     * @param runner
     *        the runner to start in its own thread (not null).
     */
    public static void runAsync(final ProcessRunner runner) {
        Check.notNull(runner, "runner"); //$NON-NLS-1$

        final Thread thread = new Thread(runner);
        thread.setName("Process Runner"); //$NON-NLS-1$

        /*
         * Sneak in a reference to the new thread. This must be done before
         * start() so that calls to interrupt() cause the correct exception
         * (because the runner is in the NEW state).
         */
        runner.setThread(thread);

        thread.start();
    }

    /**
     * Starts another process to run the commands this runner was constructed
     * with. Blocks until the process exits or {@link #interrupt()} is invoked.
     *
     * @see Runnable#run()
     */
    @Override
    public void run() {
        synchronized (this) {
            if (state != ProcessRunnerState.NEW) {
                throw new IllegalStateException("Can only run a ProcessRunner once"); //$NON-NLS-1$
            }
        }

        /*
         * If the commands were empty, we can skip the process creation which
         * may be heavy on some platforms and simply report a success.
         */
        if (commands.length == 0) {
            synchronized (this) {
                exitCode = 0;
                state = ProcessRunnerState.COMPLETED;
            }

            notifyTerminalState();
            return;
        }

        try {
            synchronized (this) {
                process = Runtime.getRuntime().exec(commands, environment, workingDirectory);
                state = ProcessRunnerState.RUNNING;
            }
        } catch (final IOException e) {
            synchronized (this) {
                error = e;
                state = ProcessRunnerState.EXEC_FAILED;
            }

            notifyTerminalState();
            return;
        }

        /*
         * If we do not pump (read) the child process's streams (standard out
         * and standard error), Windows will cause the child to block if it
         * writes more than a small amount of output (512 bytes or chars [not
         * sure which] in our testing).
         *
         * If the user of this runner is interested in the child's output, we
         * have to service the streams in other threads in order to prevent
         * becoming blind to an interruption delivered to this thread.
         * Specifically, reading from these streams is a blocking task, and
         * there is no way for the user to interrupt us while we block. If we
         * launch other threads, we arrive at process.waitFor() quickly in this
         * thread and can accept the interruption, then interrupt the readers.
         */

        final Thread outputReaderThread =
            new Thread(new ProcessOutputReader(process.getInputStream(), capturedStandardOutput));

        String messageFormat = "Standard Output Reader {0}"; //$NON-NLS-1$
        String message = MessageFormat.format(messageFormat, Long.toString(getNewThreadID()));
        outputReaderThread.setName(message);
        outputReaderThread.start();

        messageFormat = "Started IO waiter thread '{0}'"; //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, outputReaderThread.getName());
        log.debug(message);

        final Thread errorReaderThread =
            new Thread(new ProcessOutputReader(process.getErrorStream(), capturedStandardError));

        messageFormat = "Standard Error Reader {0}"; //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, Long.toString(getNewThreadID()));
        errorReaderThread.setName(message);
        errorReaderThread.start();

        messageFormat = "Started IO waiter thread '{0}'"; //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, errorReaderThread.getName());
        log.debug(message);

        int ret;
        try {
            /*
             * We must not hold the lock on this while we wait on the child, or
             * we could not be interrupted.
             */
            ret = process.waitFor();
        } catch (final InterruptedException e) {
            log.debug("Normal interruption, interrupting all IO readers"); //$NON-NLS-1$

            /*
             * We must join on all IO readers before entering a terminal state
             * to prevent the reader threads from later writing to their
             * streams. This method also performs the immediate interrupt.
             *
             * Ignore if there was an error joining because we just want to
             * terminate as INTERRUPTED anyway.
             */
            joinReaders(new Thread[] {
                outputReaderThread,
                errorReaderThread
            }, true);

            /*
             * This is the normal abort scenario. No exit code is available and
             * no error occurred.
             */
            synchronized (this) {
                state = ProcessRunnerState.INTERRUPTED;
            }

            notifyTerminalState();
            return;
        }

        /*
         * If we launched output reader threads, we have to wait for them to
         * complete here. This is usually a short wait because once we're this
         * far, process.waitFor() has finished so the readers will be reaching
         * the end of their input streams soon (and terminating).
         *
         * If we get an error back from the join, we want to consider this
         * entire runner INTERRUPTED because we can't trust the output streams
         * to have the entire contents of the process.
         */

        if (joinReaders(new Thread[] {
            outputReaderThread,
            errorReaderThread
        }, false) == false) {
            log.error("Error joining IO reader threads, setting INTERRUPTED"); //$NON-NLS-1$

            synchronized (this) {
                state = ProcessRunnerState.INTERRUPTED;
            }

            notifyTerminalState();
            return;
        }

        /*
         * Now that we have joined the IO reader threads, we can close the close
         * the streams in order to prevent Java from leaking the handles.
         */

        try {
            process.getOutputStream().close();
            process.getInputStream().close();
            process.getErrorStream().close();
        } catch (final IOException e) {
            /*
             * This exception is from Stream.close().
             *
             * A failure to configure the output streams is a critical error and
             * should be treated as a failure to launch the process. Setting
             * different state could cause the user to trust that his process
             * which returned a 0 exit code also printed no error text when it
             * actually did (and therefore failed).
             */

            log.error("Error closing child process's output streams after join, setting INTERRUPTED", e); //$NON-NLS-1$

            synchronized (this) {
                state = ProcessRunnerState.INTERRUPTED;
            }

            notifyTerminalState();
            return;
        }

        synchronized (this) {
            exitCode = ret;
            state = ProcessRunnerState.COMPLETED;
        }

        notifyTerminalState();
    }

    /**
     * Joins on the given threads in the given order, optionally interrupting
     * them first. If a thread is null, it is ignored.
     * <p>
     * If a thread throws an exception during join, false is returned, but all
     * other threads are still joined.
     *
     * @param threads
     *        the threads to join on (not null, but null elements are ignored).
     * @param immediateInterrupt
     *        if true, the threads are all immediately interrupted in order,
     *        then joined. If false, the threads are only joined.
     *
     * @return true if no error occurred joining the threads, false if an error
     *         occurred.
     */
    private boolean joinReaders(final Thread[] threads, final boolean immediateInterrupt) {
        Check.notNull(threads, "threads"); //$NON-NLS-1$

        boolean hadJoinError = false;

        if (immediateInterrupt) {
            for (int i = 0; i < threads.length; i++) {
                if (threads[i] == null) {
                    continue;
                }

                final String messageFormat = "Normal interruption of reader thread '{0}'"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, threads[i].getName());
                log.debug(message);

                threads[i].interrupt();
            }
        }

        for (int i = 0; i < threads.length; i++) {
            if (threads[i] == null) {
                continue;
            }

            try {
                threads[i].join();

                final String messageFormat = "Reader thread '{0}' joined"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, threads[i].getName());
                log.debug(message);
            } catch (final InterruptedException e) {
                final String messageFormat = "Error joining on reader '{0}'"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, threads[i].getName());
                log.warn(message, e);

                hadJoinError = true;
            }
        }

        return hadJoinError == false;
    }

    /**
     * @return a new, unique numeric thread ID appropriate for an IO reader
     *         thread.
     */
    private synchronized long getNewThreadID() {
        return threadID++;
    }

    /**
     * Builds a string that looks like a command line to show the user how his
     * external tool went wrong (or just for logging).
     *
     * @param commands
     *        first item is the command others are arguments. If null, returns
     *        null.
     * @return null if commands was null, else a string like: "/bin/ls" "-la"
     *         "/tmp"
     */
    private String makeCommandLineForDisplay(final String[] commands) {
        if (commands == null) {
            return null;
        }

        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < commands.length; i++) {
            if (i > 0) {
                sb.append(" "); //$NON-NLS-1$
            }
            sb.append("\"" + commands[i] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return sb.toString();
    }

    /**
     * @return an approximation of the command line that was run by this runner.
     *         The exact command line is not available becuase the String[]
     *         version of {@link Runtime#exec(String[])} is used. null if no
     *         external process was launched (a null command array was given
     *         during construction).
     */
    public String getCommandLineForDisplay() {
        return commandLineForDisplay;
    }

    /**
     * Gets the error object that caused the process to fail to run. Only call
     * this method if the state of the runner is
     * {@link ProcessRunnerState#EXEC_FAILED}.
     *
     * @return the error that caused the execution to fail.
     * @throws IllegalStateException
     *         if the runner's state is anything other than
     *         {@link ProcessRunnerState#EXEC_FAILED}.
     */
    public synchronized Throwable getExecutionError() {
        if (state != ProcessRunnerState.EXEC_FAILED) {
            throw new IllegalStateException("No error is available unless the process runner's state is EXEC_FAILED"); //$NON-NLS-1$
        }

        return error;
    }

    /**
     * Gets the exit status of the process that ran and completed. Only call
     * this method if the state of the runner is
     * {@link ProcessRunnerState#COMPLETED}.
     *
     * @return exit status of the process.
     * @throws IllegalStateException
     *         if the runner's state is anything other than
     *         {@link ProcessRunnerState#COMPLETED}.
     */
    public synchronized int getExitCode() {
        if (state != ProcessRunnerState.COMPLETED) {
            throw new IllegalStateException("No exit code is available unless the process runner's state is COMPLETED"); //$NON-NLS-1$
        }

        return exitCode;
    }

    /**
     * Gets the state of the runner.
     *
     * @return the state of this runner.
     */
    public synchronized ProcessRunnerState getState() {
        return state;
    }

    /**
     * @return true if the process runner has reached one of its terminal
     *         states, false if it has not.
     */
    public synchronized boolean isFinished() {
        return state == ProcessRunnerState.COMPLETED
            || state == ProcessRunnerState.EXEC_FAILED
            || state == ProcessRunnerState.INTERRUPTED;
    }

    /**
     * Sets the controlling thread for this process runner. The controlling
     * thread only needs to be set when the runner is started via
     * {@link #runAsync(ProcessRunner).
     *
     * @param thread
     *        the newly created thread running this runner (not null).
     */
    private synchronized void setThread(final Thread thread) {
        Check.notNull(thread, "thread"); //$NON-NLS-1$
        asyncThread = thread;
    }

    /**
     * Invoke when we have entered a terminal state. this.state must have been
     * set correctly before this method is invoked.
     * <p>
     * Does two things: invokes the appropriate method on the finished handler
     * if there is one, and notifies all listeners who are waiting on this via
     * {@link #waitForFinish()}.
     * <p>
     * Do not invoke this method inside a synchronized block. That might cause
     * deadlock if the {@link ProcessFinishedHandler} is written to cause
     * another thread to call back into this runner.
     */
    private void notifyTerminalState() {
        // The handler is final so we don't need to synchronize.
        if (finishedHandler != null) {
            if (state == ProcessRunnerState.COMPLETED) {
                finishedHandler.processCompleted(this);
            } else if (state == ProcessRunnerState.EXEC_FAILED) {
                finishedHandler.processExecFailed(this);
            } else if (state == ProcessRunnerState.INTERRUPTED) {
                finishedHandler.processInterrupted(this);
            } else {
                Check.isTrue(false, "State " + state.getClass().getName() + " is not a known terminal state"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        synchronized (this) {
            // Wake all threads in waitForFinish().
            notifyAll();
        }
    }
}
