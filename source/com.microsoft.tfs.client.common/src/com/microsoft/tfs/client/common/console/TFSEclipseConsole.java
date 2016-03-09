// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.console;

/**
 * Defines essential methods to write TFS console to.
 *
 * @threadsafety unknown
 */
public interface TFSEclipseConsole {
    /**
     * Prints an info message to the console. The console will be raised if it
     * is not visible.
     *
     * @param message
     *        The message to print
     */
    public void printMessage(final String message);

    /**
     * Prints a warning message to the console. The console will be raised if it
     * is not visible.
     *
     * @param message
     *        The message to print
     */
    public void printWarning(final String message);

    /**
     * Prints an error message to the console. The console will be raised if it
     * is not visible.
     *
     * @param message
     *        The message to print
     */
    public void printErrorMessage(final String message);

    public void showConsole();
}
