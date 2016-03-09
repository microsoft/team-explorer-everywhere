// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.notifications;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.ListenerRunnable;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * Provides lister management for implementations of {@link NotificationManager}
 * .
 *
 * @threadsafety thread-safe
 */
abstract class AbstractNotificationManager implements NotificationManager {
    private final SingleListenerFacade listeners = new SingleListenerFacade(NotificationListener.class);

    public AbstractNotificationManager() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(final NotificationListener listener) {
        listeners.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(final NotificationListener listener) {
        listeners.removeListener(listener);
    }

    /**
     * Removes all the listeners.
     */
    protected void clearListeners() {
        listeners.getListenerList().clear();
    }

    /**
     * Fires the
     * {@link NotificationListener#notificationReceived(Notification, long, long)}
     * event.
     *
     * @param notification
     *        the notification to send (must not be <code>null</code>)
     * @param param1
     *        the first parameter
     * @param param2
     *        the second parameter
     */
    protected void fireNotificationReceived(final Notification notification, final long param1, final long param2) {
        Check.notNull(notification, "notification"); //$NON-NLS-1$

        listeners.getListenerList().foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((NotificationListener) listener).notificationReceived(notification, param1, param2);
                return true;
            }
        });
    }
}
