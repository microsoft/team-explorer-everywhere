// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent;

import java.text.MessageFormat;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;

/**
 * Controls access via sempahore to another Executor (pool of worker threads
 * that executes runnables via {@link #execute(Runnable)}). The maximum number
 * of tasks to process at once is controlled via the bounds passed at
 * construction. There's no reason to use a bounded other Executor because all
 * the throttling is done here.
 *
 * For details, see "Java Concurrency in Practice" (Brian Goetz), Listing 8.4.
 *
 * @threadsafety thread-safe
 */
public class BoundedExecutor implements Executor {
    private final static Log log = LogFactory.getLog(BoundedExecutor.class);

    private final Executor executor;
    private final Semaphore semaphore;
    private final int maxPermits;

    /**
     * Constructs a {@link BoundedExecutor} that throttles the task submission
     * rate to the given executor by blocking.
     *
     * @param executor
     *        the executor to hand tasks to (must not be <code>null</code>) This
     *        should be an unbounded pool, because bounding it doesn't make
     *        sense (this class does the throttling).
     * @param bound
     *        the maximum number of tasks to be executing via the given
     *        executor. Must be > 0.
     */
    public BoundedExecutor(final Executor executor, final int bound) {
        Check.notNull(executor, "executor"); //$NON-NLS-1$
        Check.isTrue(bound > 0, "bound > 0"); //$NON-NLS-1$

        this.executor = executor;
        semaphore = new Semaphore(bound);
        maxPermits = bound;

        log.trace(MessageFormat.format("constructed with bounds {0}", Integer.toString(bound))); //$NON-NLS-1$
    }

    /**
     * Executes the task to the configured executor immediately if the executor
     * is not too busy (is already executing the number of commands equal to the
     * configured bounds for this instance), otherwise block.
     *
     * @param command
     *        the command to submit (must not be <code>null</code>)
     * @throws InterruptedException
     *         if the thread is interrupted while waiting for the semaphore that
     *         controls access to this executor.
     */
    @Override
    public void execute(final Runnable command) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        try {
            acquire();
        } catch (final InterruptedException e) {
            log.warn("Interrupted waiting on semaphore; re-interrupting current thread", e); //$NON-NLS-1$
            // Re-interrupt the current thread.
            Thread.currentThread().interrupt();
        }

        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        command.run();
                    } finally {
                        release();
                    }
                }
            });
        } catch (final RejectedExecutionException e) {
            release();
        }
    }

    private void acquire() throws InterruptedException {
        semaphore.acquire();

        log.trace(MessageFormat.format(
            "semaphore acquired: {0}/{1} permits left", //$NON-NLS-1$
            Integer.toString(semaphore.availablePermits()),
            Integer.toString(maxPermits)));
    }

    private void release() {
        semaphore.release();

        log.trace(MessageFormat.format(
            "semaphore released: {0}/{1} permits left", //$NON-NLS-1$
            Integer.toString(semaphore.availablePermits()),
            Integer.toString(maxPermits)));
    }
}
