// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.shutdown;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Messages;
import com.microsoft.tfs.util.TypesafeEnum;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <h1>Overview</h1> Manages the lifecycle of core objects that implement one or
 * more of the event listener interfaces in com.microsoft.tfs.core.events. This
 * class is a singleton which should be accessed through the public static
 * method getInstance().
 * <p>
 * The general use case for this class is that objects that implement the
 * lifecycle event listener interfaces will register themselves on creation with
 * this manager (via add*EventListener()). These events are fired on the
 * registered objects when the Java virtual machine is shutting down.
 * </p>
 * <h1>Usage Notes</h1>
 * <p>
 * To register events with the shutdown manager, simply get a reference via
 * {@link #getInstance()} and call
 * {@link #addShutdownEventListener(ShutdownEventListener, Priority)} and
 * {@link #removeShutdownEventListener(ShutdownEventListener, Priority)}.
 * Application should manually invoke {@link #shutdown()} when they exit (read
 * the Javadoc on the method). Because of how the JVM handles its shutdown
 * process, this class's shutdown event can be fired from any thread.
 * </p>
 * <p>
 * This class is a singleton (use {@link #getInstance()}).
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class ShutdownManager {
    public static class Priority extends TypesafeEnum {
        private Priority(final int value) {
            super(value);
        }

        /**
         * Invoke this listener early in the shutdown sequence.
         */
        public static final Priority EARLY = new Priority(0);

        /**
         * Invoke this listener after the early ones, and before the late ones.
         */
        public static final Priority MIDDLE = new Priority(1);

        /**
         * Invoke this listener late in the shutdown sequence.
         */
        public static final Priority LATE = new Priority(2);
    }

    private static final Log log = LogFactory.getLog(ShutdownManager.class);

    /**
     * The singleton.
     */
    private static ShutdownManager instance;

    private final SingleListenerFacade earlyListeners = new SingleListenerFacade(ShutdownEventListener.class);
    private final SingleListenerFacade middleListeners = new SingleListenerFacade(ShutdownEventListener.class);
    private final SingleListenerFacade lateListeners = new SingleListenerFacade(ShutdownEventListener.class);

    /**
     * Private to require singleton access.
     */
    private ShutdownManager() {
        /*
         * Create our shutdown hook thread, which will simply call shutdown()
         * when run.
         */
        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    log.trace("Shutdown hook thread started"); //$NON-NLS-1$
                    ShutdownManager.getInstance().shutdown();
                    log.trace("Shutdown hook thread finished"); //$NON-NLS-1$
                }
            });
        } catch (final IllegalStateException e) {
            /*
             * addShutdownHook throws this when we're currently shutting down.
             * We can ignore this because our callers have not assumed they are
             * registered, and they won't get the chance to proceed.
             */
            log.info("Can't add shutdown hook because we're already shutting down: not a big deal", e); //$NON-NLS-1$
        }
    }

    /**
     * Gets the single instance of ShutdownManager for any process.
     *
     * @return the ShutdownManager instance.
     */
    public synchronized static ShutdownManager getInstance() {
        if (instance == null) {
            instance = new ShutdownManager();
        }

        return instance;
    }

    /*
     * Fire Events
     */

    /**
     * Instructs all registered listeners to shutdown.
     * <p>
     * Using the shutdown manager for the first time causes it to establish a
     * link with the Java virtual machine that ensures it will be invoked during
     * JVM shutdown. However, when an application exits normally, the shutdown
     * hooks may not be called. Applications that use {@link ShutdownManager}
     * should call {@link #shutdown()} directly as part of their application
     * exit procedure to ensure all listeners are invoked before the JVM exits.
     */
    public synchronized void shutdown() {
        log.debug("shutdown"); //$NON-NLS-1$

        /*
         * This method is synchronized on this class instance so shutdown event
         * listeners can unregister themselves from the shutdown manager and
         * guarantee they won't be called again.
         */

        try {
            ((ShutdownEventListener) earlyListeners.getListener()).onShutdown();
        } catch (final IllegalStateException e) {
            /*
             * Suppress the error in case the build has been already unloaded
             * some bundles
             */
            log.warn(e.getMessage());
        }

        try {
            ((ShutdownEventListener) middleListeners.getListener()).onShutdown();
        } catch (final IllegalStateException e) {
            /*
             * Suppress the error in case the build has been already unloaded
             * some bundles
             */
            log.warn(e.getMessage());
        }

        try {
            ((ShutdownEventListener) lateListeners.getListener()).onShutdown();
        } catch (final IllegalStateException e) {
            /*
             * Suppress the error in case the build has been already unloaded
             * some bundles
             */
            log.warn(e.getMessage());
        }
    }

    /*
     * Add/Remove Listeners
     */

    /**
     * Add a listener for the event fired when core is being shut down.
     *
     * @param listener
     *        the listener to add (not null.
     * @param priority
     *        the priority of the listener (not null).
     */
    public void addShutdownEventListener(final ShutdownEventListener listener, final Priority priority) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$
        Check.notNull(priority, "priority"); //$NON-NLS-1$

        if (priority == Priority.EARLY) {
            earlyListeners.addListener(listener);
        } else if (priority == Priority.MIDDLE) {
            middleListeners.addListener(listener);
        } else if (priority == Priority.LATE) {
            lateListeners.addListener(listener);
        } else {
            final String messageFormat = Messages.getString("ShutdownManager.UnknownPriorityFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, priority.toString());
            throw new RuntimeException(message);
        }
    }

    /**
     * Remove a listener for the event fired when core is being shut down.
     *
     * @param listener
     *        the listener to remove (not null).
     * @param priority
     *        the priority of the listener to remove (not null). Must match the
     *        original priority supplied to be removed.
     *
     */
    public void removeShutdownEventListener(final ShutdownEventListener listener, final Priority priority) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$
        Check.notNull(priority, "priority"); //$NON-NLS-1$

        if (priority == Priority.EARLY) {
            earlyListeners.removeListener(listener);
        } else if (priority == Priority.MIDDLE) {
            middleListeners.removeListener(listener);
        } else if (priority == Priority.LATE) {
            lateListeners.removeListener(listener);
        } else {
            final String messageFormat = Messages.getString("ShutdownManager.UnknownPriorityFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, priority.toString());
            throw new RuntimeException(message);
        }
    }
}