// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.console.display;

import java.io.PrintStream;

/**
 *         Command-line client display interface.
 */
public interface Display {
    /**
     * Prints the text to the ouput without appending a newline.
     *
     * @param text
     *        the text to print. A newline will not be added by the
     *        implementation.
     */
    public void print(String text);

    /**
     * Flushes the standard output stream. Useful if using
     * {@link #print(String)}. {@link #printLine(String)} flushes automatically.
     */
    public void flush();

    /**
     * Prints a standard line of output.
     *
     * @param line
     *        the text to print. A newline will be added by the implementation.
     */
    public void printLine(String line);

    /**
     * Prints an error.
     *
     * @param line
     *        the text to print. A newline will be added by the implementation.
     */
    public void printErrorLine(String line);

    /**
     * @return the print stream for this display, null if none supported.
     */
    public PrintStream getPrintStream();

    /**
     * @return the print stream for the error messages for this display, null if
     *         none supported.
     */
    public PrintStream getErrorPrintStream();

    /**
     * @return the width of the display in characters (columns in a terminal).
     *         If the value is -1, the width is unknown.
     */
    public int getWidth();

    /**
     * @return the height of the display in characters (rows in a terminal). If
     *         the value is -1, the height is unknown.
     */
    public int getHeight();
}
