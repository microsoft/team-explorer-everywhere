// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.console;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.tfs.util.Check;

/**
 * Consoles register themselves with the ConsoleManager.
 */
public class Console {
    public static final String DEFAULT_STREAM_ID = "default"; //$NON-NLS-1$

    private final Log log;
    private final MessageConsole messageConsole;
    private final Object disposedLock = new Object();
    private final Object enabledLock = new Object();
    private final Map<String, ConsoleStream> streams = new HashMap<String, ConsoleStream>();
    private boolean disposed = false;
    private boolean enabled = true;

    public Console(final String name, final String loggingId) {
        this(name, loggingId, null);
    }

    public Console(final String name, final String loggingId, final ImageDescriptor imageDescriptor) {
        messageConsole = new TFSMessageConsole(name, imageDescriptor);

        String logName = Console.class.getName();
        if (loggingId != null) {
            logName = logName + "." + loggingId; //$NON-NLS-1$
        }
        log = LogFactory.getLog(logName);

        addStream(DEFAULT_STREAM_ID);

        /*
         * can be called from any thread
         */
        ConsoleManager.getDefault().addConsole(this);
    }

    protected MessageConsole getMessageConsole() {
        return messageConsole;
    }

    public void setEnabled(final boolean enabled) {
        synchronized (enabledLock) {
            this.enabled = enabled;
        }
    }

    public boolean isEnabled() {
        synchronized (enabledLock) {
            return enabled;
        }
    }

    public void setTabWidth(final int tabWidth) {
        throwIfDisposed();

        /*
         * setTabWidth() can be called from any thread
         */
        messageConsole.setTabWidth(tabWidth);
    }

    public void setWatermarks(final int low, final int high) {
        throwIfDisposed();

        /*
         * can be called from any thread
         */
        messageConsole.setWaterMarks(low, high);
    }

    public void addStream(final String id) {
        addStream(id, null);
    }

    public void addStream(final String id, final Color color) {
        Check.notNull(id, "id"); //$NON-NLS-1$

        throwIfDisposed();

        final MessageConsoleStream messageConsoleStream;

        synchronized (streams) {
            if (streams.containsKey(id)) {
                final String messageFormat = "this console already contains a stream with id [{0}]"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, id);
                throw new IllegalArgumentException(message);
            }

            /*
             * newMessageStream() can be called from any thread
             */
            messageConsoleStream = messageConsole.newMessageStream();
            final ConsoleStream stream = new ConsoleStream(id, Console.this, messageConsoleStream);
            streams.put(id, stream);
        }

        if (Display.getCurrent() != null) {
            Display.getCurrent().asyncExec(new Runnable() {
                @Override
                public void run() {
                    messageConsoleStream.setColor(color);
                }
            });
        }
    }

    public ConsoleStream getStream(final String id) {
        Check.notNull(id, "id"); //$NON-NLS-1$

        throwIfDisposed();

        ConsoleStream stream;

        synchronized (streams) {
            stream = streams.get(id);

            if (stream == null) {
                stream = streams.get(DEFAULT_STREAM_ID);
            }
        }

        return stream;
    }

    public ConsoleStream getDefaultStream() {
        return getStream(DEFAULT_STREAM_ID);
    }

    public void print(final String message) {
        getDefaultStream().print(message);
    }

    public void println(final String message) {
        getDefaultStream().println(message);
    }

    public void dispose() {
        synchronized (disposedLock) {
            if (disposed) {
                return;
            }

            disposed = true;
        }

        /*
         * removeConsoles() can be called from any thread
         */
        ConsoleManager.getDefault().removeConsole(this);
    }

    Log getLog() {
        return log;
    }

    public boolean isDisposed() {
        synchronized (disposedLock) {
            return disposed;
        }
    }

    void throwIfDisposed() {
        synchronized (disposedLock) {
            if (!disposed) {
                return;
            }
        }

        final String messageFormat = "This console ({0}) is disposed"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, messageConsole.getName());
        throw new IllegalStateException(message);
    }
}
