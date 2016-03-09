// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.notifications;

import com.microsoft.tfs.util.Closable;

/**
 * Sends and receives intra- and inter-process notification messages. The scope
 * of message delivery is determined by the implementation.
 * <p>
 * Always call {@link #close()} to ensure system resources are released and
 * references to listeners are removed.
 */
public interface NotificationManager extends Closable {
    /**
     * Adds a notification listener.
     *
     * @param listener
     *        the listener to add
     */
    void addListener(NotificationListener listener);

    /**
     * Removes a notification listener.
     *
     * @param listener
     *        the listener to remove
     */
    void removeListener(NotificationListener listener);

    /**
     * {@inheritDoc}
     * <p>
     * Closing a {@link NotificationManager} flushes the send queue and removes
     * all listeners.
     */
    @Override
    void close();

    /**
     * Sends a {@link Notification}. The message may be queued before being
     * sent.
     *
     * @param notification
     *        the notification to send (must not be <code>null</code>)
     * @param param1
     *        the first parameter to send
     * @param param2
     *        the second parameter to send
     */
    void sendNotification(Notification notification, int param1, int param2);
}