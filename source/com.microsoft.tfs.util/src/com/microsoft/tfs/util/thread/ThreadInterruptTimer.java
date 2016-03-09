// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.thread;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;

/**
 * {@link ThreadInterruptTimer} is a {@link Thread} that can interrupt another
 * thread after a period of time has elapsed without a specific kind of
 * communication to the instance.
 * <p>
 * To use it, one (original) thread creates an instance of
 * {@link ThreadInterruptTimer} passing it a timeout and a reference it its own
 * thread, then starts it. The original thread periodically calls
 * {@link ThreadInterruptTimer#reset()} to reset the internal timer (to postpone
 * interruption). If the original thread does not call {@link #reset()} for a
 * time exceeding the timeout given on construction, the
 * {@link ThreadInterruptTimer} instance invokes {@link Thread#interrupt()} once
 * on the original thread and completes.
 * <p>
 * The timeout check is done using a low-resolution sleep loop, accurate to
 * better than {@link #TIMEOUT_CHECK_PERIOD_SECONDS} seconds.
 */
public class ThreadInterruptTimer extends Thread {
    private static final Log log = LogFactory.getLog(ThreadInterruptTimer.class);

    /**
     * {@link ThreadInterruptTimer} sleeps this many seconds after computing and
     * checking its internal idle value. Interruption can be delayed as much as
     * this value (in seconds) from the actual desired timeout.
     */
    public final static int TIMEOUT_CHECK_PERIOD_SECONDS = 2;

    /**
     * The last time we were poked to keep awake (in milliseconds since epoch).
     */
    private long lastReset;

    /**
     * How many seconds we can be idle (time since we were reset) before
     * triggering shutdown.
     */
    private int interruptTimeoutSeconds;

    /**
     * The thread we interrupt when the idle period is exceeded.
     */
    final Thread threadToInterrupt;

    /**
     * Create a {@link ThreadInterruptTimer} with the given idle timeout in
     * seconds that interrupts the given thread when the timeout is reached.
     *
     * @param interruptTimeoutSeconds
     *        time in seconds after which a {@link ThreadInterruptTimer} that
     *        has not been {@link #reset()} since its creation or the last
     *        {@link #reset()} will interrupt the given thread. See the class's
     *        Javadoc for timer accuracy notes. Must be positive.
     * @param threadToInterrupt
     *        the thread to interrupt (via {@link Thread#interrupt()} after the
     *        given timeout. Not null.
     */
    public ThreadInterruptTimer(final int interruptTimeoutSeconds, final Thread threadToInterrupt) {
        super("Thread Interrupt Timer"); //$NON-NLS-1$

        Check.isTrue(interruptTimeoutSeconds > 0, "interruptTimoutSeconds must be positive"); //$NON-NLS-1$
        Check.notNull(threadToInterrupt, "threadToInterrupt"); //$NON-NLS-1$

        synchronized (this) {
            this.interruptTimeoutSeconds = interruptTimeoutSeconds;
            this.threadToInterrupt = threadToInterrupt;
        }
    }

    /**
     * Resets the timer's internal idle counter, postponing thread interruption
     * until the timeout is reached again.
     */
    public void reset() {
        final long now = System.currentTimeMillis();

        final String messageFormat = "Resetting idle at {0}"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, now);
        log.trace(message);

        synchronized (this) {
            lastReset = now;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        reset();

        while (true) {
            long lastResetCopy;
            long interruptTimeoutSecondsCopy;

            synchronized (this) {
                lastResetCopy = lastReset;
                interruptTimeoutSecondsCopy = interruptTimeoutSeconds;
            }

            final long idleMilliseconds = System.currentTimeMillis() - lastResetCopy;

            if (idleMilliseconds > interruptTimeoutSecondsCopy * 1000) {
                break;
            }

            final String messageFormat = "Idle for an acceptable {0} milliseconds out of {1}"; //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, idleMilliseconds, interruptTimeoutSecondsCopy * 1000);
            log.trace(message);

            try {
                Thread.sleep(TIMEOUT_CHECK_PERIOD_SECONDS * 1000);
            } catch (final InterruptedException e) {
                break;
            }
        }

        /*
         * To avoid deadlock, we don't hold a lock on this object while
         * interrupting. The implementation of interrupt on the other thread may
         * call back into this class (though there's probably no reason to).
         */
        Thread interruptThisThread;
        synchronized (this) {
            interruptThisThread = threadToInterrupt;
        }

        final String messageFormat = "Timeout period of {0} seconds exceeded, interrupting {1}"; //$NON-NLS-1$
        final String message =
            MessageFormat.format(messageFormat, interruptTimeoutSeconds, interruptThisThread.toString());
        log.debug(message);

        interruptThisThread.interrupt();
    }
}
