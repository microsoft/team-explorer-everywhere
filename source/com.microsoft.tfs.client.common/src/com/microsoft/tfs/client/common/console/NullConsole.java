// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.console;

/**
 * A place holder for a {@link TFSConsole) object
 *
 * @threadsafety unknown
 */
public final class NullConsole implements TFSEclipseConsole {
    @Override
    public void printErrorMessage(final String message) {

    }

    @Override
    public void printMessage(final String message) {

    }

    @Override
    public void printWarning(final String message) {

    }

    @Override
    public void showConsole() {

    }
}
