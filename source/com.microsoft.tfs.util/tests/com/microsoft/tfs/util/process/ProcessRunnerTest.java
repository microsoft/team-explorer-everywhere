// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.process.ProcessRunner.ProcessRunnerState;

import junit.framework.TestCase;

/**
 * Tests external process runners in a very simple way. Uses the "dir" command,
 * which exists on Windows systems and many Linux systems.
 */
public class ProcessRunnerTest extends TestCase {
    private String getValidCommand() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return "whoami.exe"; //$NON-NLS-1$
        } else {
            return "ls"; //$NON-NLS-1$
        }
    }

    private String getInvalidCommand() {
        return "THIS-commAND_is_INVALID"; //$NON-NLS-1$
    }

    private String getArgumentForOutput() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return "/?"; //$NON-NLS-1$
        } else {
            return "-la"; //$NON-NLS-1$
        }
    }

    private String getArgumentForError() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return "/zzzz"; //$NON-NLS-1$
        } else {
            return "-8"; //$NON-NLS-1$
        }
    }

    public void testSync() {
        final ProcessRunner runner = new ProcessRunner(new String[] {
            getValidCommand()
        }, null, null, null);

        assertEquals("\"" + getValidCommand() + "\"", runner.getCommandLineForDisplay()); //$NON-NLS-1$ //$NON-NLS-2$

        assertStateNew(runner);

        runner.run();

        assertStateCompleted(runner);

        assertTrue(runner.isFinished());
        assertEquals(0, runner.getExitCode());

        try {
            runner.getExecutionError();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }
    }

    public void testSyncAndStandardOutput() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final ProcessRunner runner = new ProcessRunner(new String[] {
            getValidCommand(),
            getArgumentForOutput()
        }, null, null, null, outputStream, null);

        assertEquals(
            "\"" + getValidCommand() + "\" \"" + getArgumentForOutput() + "\"", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            runner.getCommandLineForDisplay());

        assertStateNew(runner);

        runner.run();

        assertStateCompleted(runner);

        // We must get some output
        assertTrue(outputStream.toString().length() > 0);

        assertTrue(runner.isFinished());
        assertEquals(0, runner.getExitCode());

        try {
            runner.getExecutionError();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }
    }

    public void testStandardError() {
        final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        final ProcessRunner runner = new ProcessRunner(new String[] {
            getValidCommand(),
            getArgumentForError()
        }, null, null, null, null, errorStream);

        assertEquals(
            "\"" + getValidCommand() + "\" \"" + getArgumentForError() + "\"", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            runner.getCommandLineForDisplay());

        assertStateNew(runner);

        runner.run();

        assertStateCompleted(runner);

        // We must get some error text
        assertTrue(errorStream.toString().length() > 0);

        assertTrue(runner.isFinished());
        assertFalse(runner.getExitCode() == 0);

        try {
            runner.getExecutionError();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }
    }

    public void testSyncNonZeroExit() {
        final ProcessRunner runner = new ProcessRunner(new String[] {
            getValidCommand(),
            getArgumentForError()
        }, null, null, null);

        assertEquals(
            "\"" + getValidCommand() + "\" \"" + getArgumentForError() + "\"", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            runner.getCommandLineForDisplay());

        assertStateNew(runner);

        runner.run();

        assertStateCompleted(runner);

        assertTrue(runner.isFinished());
        assertFalse(runner.getExitCode() == 0);

        try {
            runner.getExecutionError();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }
    }

    public void testSyncExecFailed() {
        final ProcessRunner runner = new ProcessRunner(new String[] {
            getInvalidCommand()
        }, null, null, null);

        assertStateNew(runner);

        runner.run();

        assertStateExecFailed(runner);
        assertTrue(runner.isFinished());

        assertNotNull(runner.getExecutionError());
        assertTrue(runner.getExecutionError() instanceof Throwable);
        assertTrue(runner.getExecutionError() instanceof IOException);

        // Should throw when exec failed.
        try {
            runner.getExitCode();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }
    }

    public void testAsync() {
        final ProcessRunner runner = new ProcessRunner(new String[] {
            getValidCommand()
        }, null, null, null);

        assertEquals("\"" + getValidCommand() + "\"", runner.getCommandLineForDisplay()); //$NON-NLS-1$ //$NON-NLS-2$

        assertStateNew(runner);

        ProcessRunner.runAsync(runner);
        runner.waitForFinish();

        assertStateCompleted(runner);

        assertTrue(runner.isFinished());
        assertEquals(0, runner.getExitCode());

        try {
            runner.getExecutionError();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }
    }

    public void testAsyncNonZero() {
        final ProcessRunner runner = new ProcessRunner(new String[] {
            getValidCommand(),
            getArgumentForError()
        }, null, null, null);

        assertEquals(
            "\"" + getValidCommand() + "\" \"" + getArgumentForError() + "\"", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            runner.getCommandLineForDisplay());

        assertStateNew(runner);

        ProcessRunner.runAsync(runner);
        runner.waitForFinish();

        assertStateCompleted(runner);

        assertTrue(runner.isFinished());
        assertFalse(runner.getExitCode() == 0);

        try {
            runner.getExecutionError();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }
    }

    public void testAsyncExecFailed() {
        final ProcessRunner runner = new ProcessRunner(new String[] {
            getInvalidCommand()
        }, null, null, null);

        assertStateNew(runner);

        ProcessRunner.runAsync(runner);
        runner.waitForFinish();

        assertStateExecFailed(runner);
        assertTrue(runner.isFinished());

        assertNotNull(runner.getExecutionError());
        assertTrue(runner.getExecutionError() instanceof Throwable);
        assertTrue(runner.getExecutionError() instanceof IOException);

        // Should throw when exec failed.
        try {
            runner.getExitCode();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }
    }

    public void testIllegalStates() {
        final ProcessRunner runner = new ProcessRunner(new String[] {
            getInvalidCommand()
        }, null, null, null);

        /*
         * A new process runner will throw illegal state exceptions when queried
         * about errors, exit code, etc.: things that have not been produced.
         */

        try {
            runner.getExitCode();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }

        try {
            runner.getExecutionError();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }

        try {
            runner.interrupt();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }

        try {
            runner.waitForFinish();
            assertTrue(false);
        } catch (final IllegalStateException e) {
            // Exception is normal in this state.
        }
    }

    private void assertStateNew(final ProcessRunner runner) {
        assertTrue(runner.getState() == ProcessRunnerState.NEW);
    }

    private void assertStateCompleted(final ProcessRunner runner) {
        assertTrue(runner.getState() == ProcessRunnerState.COMPLETED);
    }

    private void assertStateExecFailed(final ProcessRunner runner) {
        assertTrue(runner.getState() == ProcessRunnerState.EXEC_FAILED);
    }
}
