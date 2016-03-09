// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * An interface to console platform utilities. See {@link ConsoleUtils} for an
 * implementation of this interface.
 */
public interface Console {
    /**
     * Gets the width of the console in which this application is running.
     *
     * @return the width of the console in columns, 0 if the width could not be
     *         determined.
     */
    public int getConsoleColumns();

    /**
     * Gets the height of the console in which this application is running.
     *
     * @return the height of the console in rows, 0 if the height could not be
     *         determined.
     */
    public int getConsoleRows();

    /**
     * Disables character echo on the console.
     *
     * @return true if the operation succeeded, false if it did not.
     */
    public boolean disableEcho();

    /**
     * Enables character echo on the console.
     *
     * @return true if the operation succeeded, false if it did not.
     */
    public boolean enableEcho();
}
