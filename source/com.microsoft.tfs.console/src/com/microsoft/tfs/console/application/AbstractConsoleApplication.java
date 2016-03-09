// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.console.application;

import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.console.input.Input;

/**
 * An interface which console applications may implement to support console IO
 * abstraction.
 * <p>
 * {@link AbstractConsoleApplication} implementations must have a zero-argument
 * constructor to support dynamic creation from class name. It must also
 * implement a "public static void main(String[])" method.
 */
public interface AbstractConsoleApplication {
    /**
     * Sets the display this abstract console application will use for all
     * output.
     *
     * @param display
     *        the display to use (not null).
     */
    public void setDisplay(Display display);

    /**
     * @return the display currently in use by this abstract console
     *         application.
     */
    public Display getDisplay();

    /**
     * Sets the input this abstract console application will use for all input.
     *
     * @param input
     *        the input to use (not null).
     */
    public void setInput(Input input);

    /**
     * @return the input currently in use by this abstract console application.
     */
    public Input getInput();

    /**
     * Runs the application with the given command-line arguments. The
     * implementation of this method will resemble a static main() method.
     *
     * @param args
     *        command-line arguments to give (not null).
     * @return the exit status of the application.
     */
    public int run(String[] args);
}
