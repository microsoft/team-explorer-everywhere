// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.console.display;

import java.io.PrintStream;
import java.io.PrintWriter;

import com.microsoft.tfs.jni.ConsoleUtils;

/**
 * Implements a terminal display for the command-line client that sends data to
 * the JVM's default console streams.
 */
public class ConsoleDisplay implements Display {
    private final PrintWriter outputWriter;
    private final PrintWriter errorWriter;
    private final int width;
    private final int height;

    /**
     * The width to use if the underlying console's size can't be determined
     * (maybe the terminal is misconfigured or JNI and fallback detection
     * failed).
     */
    public final static int DEFAULT_CONSOLE_WIDTH = 80;

    /**
     * The height to use if the underlying console's size can't be determined
     * (maybe the terminal is misconfigured or JNI and fallback detection
     * failed).
     */
    public final static int DEFAULT_CONSOLE_HEIGHT = 25;

    /**
     * The maximum width supported by the display. Detected widths that are
     * larger than this are set to the maximum.
     * <p>
     * This check prevents some pathological configurations (Eclipse's debug
     * console, which is extremely wide) from causing resource exhaustion during
     * line fills.
     */
    public final static int MAXIMUM_CONSOLE_WIDTH = 16000;

    /**
     * The maximum height supported by the display. Detected heights that are
     * larger than this are set to the maximum.
     * <p>
     * This check prevents some pathological configurations (Eclipse's debug
     * console, which is extremely wide) from causing resource exhaustion during
     * line fills.
     */
    public final static int MAXIMUM_CONSOLE_HEIGHT = 16000;

    /**
     * Creates a terminal display using the streams and properties of the
     * default JVM console. The dimensions of the display are automatically
     * detected.
     *
     * @param useBufferedStream
     *        if true, printLine() and printErrorLine() will write to the
     *        buffered output streams instead of the unbuffered streams. Usually
     *        buffering is not specifically desired.
     */
    public ConsoleDisplay(final boolean bufferOutput) {
        super();

        outputWriter = new PrintWriter(System.out, bufferOutput == false);
        errorWriter = new PrintWriter(System.err, bufferOutput == false);

        int consoleWidth = ConsoleUtils.getInstance().getConsoleColumns();
        int consoleHeight = ConsoleUtils.getInstance().getConsoleRows();

        if (consoleWidth == 0) {
            consoleWidth = DEFAULT_CONSOLE_WIDTH;
        } else if (consoleWidth > MAXIMUM_CONSOLE_WIDTH) {
            consoleWidth = MAXIMUM_CONSOLE_WIDTH;
        }

        if (consoleHeight == 0) {
            consoleHeight = DEFAULT_CONSOLE_HEIGHT;
        } else if (consoleHeight > MAXIMUM_CONSOLE_HEIGHT) {
            consoleHeight = MAXIMUM_CONSOLE_HEIGHT;
        }

        width = consoleWidth;
        height = consoleHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final String text) {
        outputWriter.print(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        outputWriter.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printLine(final String line) {
        outputWriter.println(line);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printErrorLine(final String line) {
        errorWriter.println(line);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream getPrintStream() {
        return System.out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream getErrorPrintStream() {
        return System.err;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return height;
    }
}
