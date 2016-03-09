// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.console;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.eclipse.ui.console.MessageConsoleStream;

public class ConsoleStream {
    private final String id;
    private final Console console;
    private final MessageConsoleStream stream;

    ConsoleStream(final String id, final Console console, final MessageConsoleStream stream) {
        this.id = id;
        this.console = console;
        this.stream = stream;
    }

    public String getID() {
        return id;
    }

    public MessageConsoleStream getStream() {
        return stream;
    }

    public void print(final String message) {
        printConsoleMessage(message, false);
    }

    public void println(final String message) {
        printConsoleMessage(message, true);
    }

    private void printConsoleMessage(final String message, final boolean newline) {
        final Log log = console.getLog();

        if (log.isDebugEnabled()) {
            final String messageFormat = "[{0}]: {1}"; //$NON-NLS-1$
            final String text = MessageFormat.format(messageFormat, id, message);
            log.debug(text);
        }

        if (!console.isEnabled()) {
            return;
        }

        console.throwIfDisposed();

        /*
         * println() and print() can be called from any thread
         */

        if (newline) {
            stream.println(message);
        } else {
            stream.print(message);
        }
    }
}
