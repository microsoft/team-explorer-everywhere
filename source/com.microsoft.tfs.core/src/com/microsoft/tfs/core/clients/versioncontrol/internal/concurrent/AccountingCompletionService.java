// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extends {@link ExecutorCompletionService} to count the number of tasks that
 * have been submitted so they can be waited on in bulk. Each wait call (
 * {@link #waitForCompletions()} and friends) resets the count so it can be
 * called again later.
 * <p>
 * {@link #take()}, {@link #poll()}, and
 * {@link #poll(long, java.util.concurrent.TimeUnit)} throw
 * {@link IllegalStateException} in this class; you can only use
 * {@link #waitForCompletions()}.
 *
 * @threadsafety thread-safe
 */
public class AccountingCompletionService<V> extends ExecutorCompletionService<V> {
    private final static Log log = LogFactory.getLog(AccountingCompletionService.class);

    /**
     * Keeps a count of the submissions that have not been waited on with
     * {@link #waitForCompletions(ResultProcessor, ExecutionExceptionHandler)}.
     */
    private long unwaitedSubmissions = 0;
    private final Object unwaitedSubmissionsLock = new Object();

    public AccountingCompletionService(final Executor executor, final BlockingQueue<Future<V>> completionQueue) {
        super(executor, completionQueue);
    }

    public AccountingCompletionService(final Executor executor) {
        super(executor);
    }

    @Override
    public Future<V> submit(final Callable<V> task) {
        synchronized (unwaitedSubmissionsLock) {
            final Future<V> ret = super.submit(task);
            unwaitedSubmissions++;
            return ret;
        }
    }

    @Override
    public Future<V> submit(final Runnable task, final V result) {
        synchronized (unwaitedSubmissionsLock) {
            final Future<V> ret = super.submit(task, result);
            unwaitedSubmissions++;
            return ret;
        }
    }

    /**
     * Not available. Use {@link #waitForCompletions()} instead.
     */
    @Override
    public Future<V> take() throws InterruptedException {
        throw new IllegalStateException();
    }

    /**
     * Not available. Use {@link #waitForCompletions()} instead.
     */
    @Override
    public Future<V> poll() {
        throw new IllegalStateException();
    }

    /**
     * Not available. Use {@link #waitForCompletions()} instead.
     */
    @Override
    public Future<V> poll(final long timeout, final TimeUnit unit) throws InterruptedException {
        throw new IllegalStateException();
    }

    /**
     * @equivalence waitForCompletions(null)
     */
    public void waitForCompletions() {
        // Locks for us
        waitForCompletions(null);
    }

    /**
     * @equivalence waitForCompletions(resultProcessor, null)
     */
    public void waitForCompletions(final ResultProcessor<V> resultProcessor) {
        // Locks for us
        waitForCompletions(resultProcessor, null);
    }

    /**
     * Waits for all the tasks previously submitted to this completion service
     * to finish by calling {@link CompletionService#take()} once for each
     * submitted task. Each tasks result is passed to an optional
     * {@link ResultProcessor} for processing.
     * <p>
     * Thread interruption stops the wait.
     *
     * @param resultProcessor
     *        an object to process the task results (may be <code>null</code>)
     * @param exceptionHandler
     *        an object to handle {@link ExecutionException}s produced by tasks
     *        (may be <code>null</code>)
     */
    public void waitForCompletions(
        final ResultProcessor<V> resultProcessor,
        final ExecutionExceptionHandler exceptionHandler) {
        long submittedCountCopy;

        synchronized (unwaitedSubmissionsLock) {
            submittedCountCopy = unwaitedSubmissions;
            unwaitedSubmissions = 0;
        }

        try {
            for (int i = 0; i < submittedCountCopy; i++) {
                final Future<V> f = super.take();
                try {
                    if (resultProcessor != null) {
                        resultProcessor.processResult(f.get());
                    }
                } catch (final ExecutionException e) {
                    log.debug("Execution exception", e); //$NON-NLS-1$

                    if (exceptionHandler != null) {
                        exceptionHandler.handleException(e);
                    }
                }
            }
        } catch (final InterruptedException e) {
            log.debug("Interrupted waiting for completion service take", e); //$NON-NLS-1$

            Thread.currentThread().interrupt();
        }
    }

    public static interface ResultProcessor<V> {
        /**
         * Process one result obtained by {@link CompletionService#take()}.
         *
         * @param result
         *        the result, which may be <code>null</code>
         */
        void processResult(final V result);
    }

    public static interface ExecutionExceptionHandler {
        /**
         * Handles one {@link ExecutionException} obtained when calling
         * {@link Future#get()}. The implementation may rethrow the exception as
         * a runtime exception to stop processing in
         * {@link AccountingCompletionService#waitForCompletions(ResultProcessor, ExecutionExceptionHandler)}
         * .
         *
         * @param e
         *        the exception (must not be <code>null</code>)
         */
        void handleException(final ExecutionException e);
    }
}
