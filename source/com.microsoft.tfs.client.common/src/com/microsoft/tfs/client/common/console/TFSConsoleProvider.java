// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.console;

/**
 * Provides an instance of a {@link TFSConsole) object created in (
 * {@link TFSCommonUIClientPlugin)
 *
 * @threadsafety unknown
 */
public interface TFSConsoleProvider {
    /**
     * Gets a TFSConsole.
     *
     * @return the console instance to use, or <code>null</code> if the
     *         implementation cannot provide one
     */
    TFSEclipseConsole getConsole();
}
