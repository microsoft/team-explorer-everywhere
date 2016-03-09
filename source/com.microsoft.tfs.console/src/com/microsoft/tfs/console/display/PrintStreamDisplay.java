// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.console.display;

import java.io.PrintStream;

import com.microsoft.tfs.util.Check;

/**
 *         Implements a display for the command-line client that uses
 *         caller-defined PrintWriters for all display.
 */
public class PrintStreamDisplay implements Display {
    private final PrintStream outputStream;
    private final PrintStream errorStream;
    private final int width;
    private final int height;

    /**
     * Creates a display with caller-defined print streams.
     *
     * @param outputStream
     *        the stream to use for normal messages (not null).
     * @param errorStream
     *        the stream to use for error messages (not null).
     * @param width
     *        the terminal width to simulate. -1 to declare an unknown width.
     * @parma height the terminal height to simulate. -1 to declare an unknown
     *        height.
     */
    public PrintStreamDisplay(
        final PrintStream outputStream,
        final PrintStream errorStream,
        final int width,
        final int height) {
        super();

        Check.notNull(outputStream, "outputStream"); //$NON-NLS-1$
        Check.notNull(errorStream, "errorStream"); //$NON-NLS-1$

        this.outputStream = outputStream;
        this.errorStream = errorStream;
        this.width = width;
        this.height = height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final String text) {
        outputStream.print(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        outputStream.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printLine(final String line) {
        outputStream.println(line);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printErrorLine(final String line) {
        errorStream.println(line);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream getPrintStream() {
        return outputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream getErrorPrintStream() {
        return errorStream;
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
