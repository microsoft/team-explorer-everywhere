// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.notifications;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.MessageWindow;
import com.microsoft.tfs.jni.MessageWindow.MessageListener;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * Implements {@link NotificationManager} using a {@link MessageWindow}.
 * Currently this implementation is only useful on Windows platforms. It is safe
 * to use on other platforms, but it will not send or receive any messages.
 * <p>
 * Unlike the Visual Studio implementation, this class does not support
 * "immediate" notification sending (all notifications will be queued and sent
 * in batches).
 * <p>
 * Also, all queued notifications are always "collapsed" on both params (which
 * is optional but always used in the VS implementation). This means a
 * notification that exactly matches an already-queued notification will not be
 * queued.
 *
 * @threadsafety thread-safe
 */
public class MessageWindowNotificationManager extends AbstractNotificationManager {
    private final static Log log = LogFactory.getLog(MessageWindowNotificationManager.class);

    private static final long SEND_DELAY_MS = 5 * 1000;
    private static final long SEND_PERIOD_MS = 5 * 1000;

    /*
     * The class name and user data (version) must be compatible with the Visual
     * Studio products we need to communicate with.
     */
    private static final String MESSAGE_WINDOW_CLASS_NAME = "TeamFoundationNotificationWindow"; //$NON-NLS-1$
    private static final String MESSAGE_WINDOW_TITLE = "TEE TeamFoundationNotificationWindow"; //$NON-NLS-1$
    private static final long[] MESSAGE_WINDOW_USER_DATA_TARGETS = new long[] {
        NotificationWindowVersion.LATEST,
        NotificationWindowVersion.DEV10_RTM
    };

    /**
     * The hidden message window we use to send and receive notifications.
     * <code>null</code> on non-Windows platforms.
     */
    private final MessageWindow messageWindow;

    /**
     * Holds notifications before they're sent. Synchronized on itself.
     */
    private final LinkedList<QueuedNotification> sendQueue = new LinkedList<QueuedNotification>();

    /**
     * Sends queued notifications.
     */
    private final Timer timer = new Timer(false);

    /**
     * Creates a {@link MessageWindowNotificationManager}. On Windows this
     * constructor creates a hidden message-only window ({@link MessageWindow})
     * to do IPC. On non-Windows platforms, the class sends or receives no
     * messages.
     * <p>
     * The application must be in a state where window creation will succeed
     * when this constructor is used.
     */
    public MessageWindowNotificationManager() {
        super();

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            this.messageWindow = new MessageWindow(
                0,
                MESSAGE_WINDOW_CLASS_NAME,
                MESSAGE_WINDOW_TITLE,
                NotificationWindowVersion.LATEST,
                new MessageListener() {
                    @Override
                    public void messageReceived(final int msg, final long wParam, final long lParam) {
                        final Notification n = Notification.fromValue(msg);
                        if (n == null) {
                            log.info(
                                MessageFormat.format(
                                    "Ignoring unknown notification msg={0}, wParam={1}, lParam={2}", //$NON-NLS-1$
                                    Integer.toString(msg),
                                    Long.toHexString(wParam),
                                    Long.toHexString(lParam)));
                        } else {
                            fireNotificationReceived(n, wParam, lParam);
                        }
                    }
                });

            // Schedule a recurring sender task
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendQueuedNotifications();
                }
            }, SEND_DELAY_MS, SEND_PERIOD_MS);
        } else {
            this.messageWindow = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // Cancel the sending timer
        timer.cancel();

        // Remove all listeners to prevent any more messages being received
        clearListeners();

        // Flush remaining notifications
        sendQueuedNotifications();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendNotification(final Notification notification, final int param1, final int param2) {
        Check.notNull(notification, "notification"); //$NON-NLS-1$

        synchronized (sendQueue) {
            for (final QueuedNotification ni : sendQueue) {
                if (notification.equals(ni)) {
                    return;
                }
            }

            sendQueue.add(new QueuedNotification(notification, param1, param2));
        }
    }

    /**
     * Called by the timer to send all the queued notifications.
     */
    private void sendQueuedNotifications() {
        QueuedNotification[] notifications = null;

        synchronized (sendQueue) {
            notifications = sendQueue.toArray(new QueuedNotification[sendQueue.size()]);
            sendQueue.clear();
        }

        if (notifications.length == 0) {
            return;
        }

        for (final QueuedNotification ni : notifications) {
            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                messageWindow.sendMessage(
                    MESSAGE_WINDOW_CLASS_NAME,
                    MESSAGE_WINDOW_USER_DATA_TARGETS,
                    ni.notification.getValue(),
                    ni.param1,
                    ni.param2);
            } else {
                // Implement other platform sender mechanisms here?
            }
        }
    }
}
