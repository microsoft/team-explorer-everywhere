// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.notifications;

/**
 * Internal data for {@link MessageWindowNotificationManager}.
 *
 * @threadsafety immutable
 */
class QueuedNotification {
    public final Notification notification;
    public final long param1;
    public final long param2;

    public QueuedNotification(final Notification notification, final long param1, final long param2) {
        this.notification = notification;
        this.param1 = param1;
        this.param2 = param2;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof QueuedNotification == false) {
            return false;
        }

        final QueuedNotification other = (QueuedNotification) obj;

        return other.notification == notification && other.param1 == param1 && other.param2 == param2;
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + notification.getValue();
        result = result * 37 + (int) (param1 ^ (param1 >>> 32));
        result = result * 37 + (int) (param2 ^ (param2 >>> 32));

        return result;
    }
}
