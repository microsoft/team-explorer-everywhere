// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A default implementation of {@link ListenerExceptionHandler}. This
 * implementation logs all exceptions and does not prevent the
 * {@link ListenerList} from continuing iteration when an exception is thrown by
 * a listener.
 *
 * @see ListenerExceptionHandler
 * @see ListenerList
 */
public class DefaultExceptionHandler implements ListenerExceptionHandler {
    /**
     * A singleton instance of {@link DefaultExceptionHandler} to use. Since
     * {@link DefaultExceptionHandler} is stateless, this default instance
     * should be used anywhere an instance of {@link DefaultExceptionHandler} is
     * needed.
     */
    public static final ListenerExceptionHandler INSTANCE = new DefaultExceptionHandler();

    private static final Log log = LogFactory.getLog(DefaultExceptionHandler.class);

    private DefaultExceptionHandler() {

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.listeners.ListenerExceptionHandler#onException
     * (java.lang.Object, com.microsoft.tfs.util.listeners.ListenerRunnable,
     * com.microsoft.tfs.util.listeners.ListenerList, java.lang.Throwable)
     */
    @Override
    public boolean onException(
        final Object listener,
        final ListenerRunnable listenerRunnable,
        final ListenerList listenerList,
        final Throwable exception) {
        final String messageFormat = "listener [{0}] threw exception {1}"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, listener, exception);
        log.warn(message, exception);
        return true;
    }
}
