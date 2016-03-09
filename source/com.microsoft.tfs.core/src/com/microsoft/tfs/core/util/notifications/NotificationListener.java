// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.notifications;

/**
 * Listens for notifications from a {@link NotificationManager}.
 *
 * @threadsafety thread-compatible
 */
public interface NotificationListener {
    /**
     * Called when a message is received by a {@link NotificationManager},
     *
     * @param notification
     *        the notification (must not be <code>null</code>)
     * @param param1
     *        the first parameter
     * @param param2
     *        the second parameter
     */
    void notificationReceived(Notification notification, long param1, long param2);
}
