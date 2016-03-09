// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.console.display;

import java.io.PrintStream;

/**
 *         Doesn't display anything anywhere, but implements all of the methods
 *         that Display requires.
 */
public class NullDisplay implements Display {
    public NullDisplay() {
        super();
    }

    @Override
    public void print(final String text) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void printLine(final String line) {
    }

    @Override
    public void printErrorLine(final String line) {
    }

    @Override
    public PrintStream getPrintStream() {
        return null;
    }

    @Override
    public PrintStream getErrorPrintStream() {
        return null;
    }

    @Override
    public int getHeight() {
        return 25;
    }

    @Override
    public int getWidth() {
        return 80;
    }
}
