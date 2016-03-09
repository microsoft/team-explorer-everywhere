// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers;

import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * Holds completion status information for an {@link Worker}, which is usually
 * run through a {@link CompletionService} which can return a {@link Future} to
 * access tasks that have been submitted to it. The {@link Future} can return
 * one of these status objects (which is the result of the {@link Worker}'s
 * computation).
 */
public final class WorkerStatus {
    /**
     * Describes the final state of an {@link Worker}.
     */
    public static class FinalState extends TypesafeEnum {
        private FinalState(final int value) {
            super(value);
        }

        /**
         * The worker performed its work and encountered no error or
         * cancellation.
         */
        public final static FinalState NORMAL = new FinalState(0);

        /**
         * The worker was canceled via {@link TaskMonitor} and may have
         * completed some work. Workers try very hard to leave working folders
         * in a consistent state when they are canceled (they will finish work
         * required to do this before completing).
         */
        public final static FinalState CANCELED = new FinalState(1);

        /**
         * The worker was interrupted by an error thrown by code inside the
         * worker.
         */
        public final static FinalState ERROR = new FinalState(2);
    }

    private final Worker worker;
    private final FinalState finalState;

    /**
     * Create a {@link WorkerStatus} describing the given final state.
     *
     * @param worker
     *        the worker that this status belongs to (must not be
     *        <code>null</code>)
     * @param finalState
     *        the final state of the worker class (must not be <code>null</code>
     *        )
     */
    public WorkerStatus(final Worker worker, final FinalState finalState) {
        Check.notNull(worker, "worker"); //$NON-NLS-1$
        Check.notNull(finalState, "finalState"); //$NON-NLS-1$

        this.worker = worker;
        this.finalState = finalState;
    }

    /**
     * @return the {@link Worker} that created this status.
     */
    public Worker getWorkerClass() {
        return worker;
    }

    /**
     * @return the final state of the worker. If the state is
     *         {@link FinalState#ERROR}, call {@link #getFatalError()} for the
     *         throwable.
     */
    public FinalState getFinalState() {
        return finalState;
    }
}
