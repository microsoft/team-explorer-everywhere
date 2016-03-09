// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.process;

import com.microsoft.tfs.util.process.ProcessRunner.ProcessRunnerState;

/**
 * Handles process runner state change events fired by {@link ProcessRunner}.
 */
public interface ProcessFinishedHandler {
    /**
     * Invoked by {@link ProcessRunner} only when its process state has reached
     * {@link ProcessRunnerState#EXEC_FAILED} because
     * {@link Runtime#exec(String[])} failed. Not invoked if the runner reaches
     * any other terminal state.
     * <p>
     * Because {@link ProcessRunnerState#EXEC_FAILED} is a terminal state, the
     * runner's state is guaranteed not to change before or while this method is
     * invoked.
     *
     * @param runner
     *        the runner in the state {@link ProcessRunnerState#EXEC_FAILED}
     *        because {@link Runtime#exec(String[])} failed.
     */
    public void processExecFailed(ProcessRunner runner);

    /**
     * Invoked by {@link ProcessRunner} only when its process state has reached
     * {@link ProcessRunnerState#COMPLETED} after normal process termination.
     * Not invoked if the runner reaches any other terminal state.
     * <p>
     * Because {@link ProcessRunnerState#COMPLETED} is a terminal state, the
     * runner's state is guaranteed not to change before or while this method is
     * invoked.
     *
     * @param runner
     *        the runner in the state {@link ProcessRunnerState#COMPLETED} after
     *        normal process termination.
     */
    public void processCompleted(ProcessRunner runner);

    /**
     * Invoked by {@link ProcessRunner} only when its process state has reached
     * {@link ProcessRunnerState#INTERRUPTED} after a call to
     * {@link ProcessRunner#interrupt()}. Not invoked if the runner reaches any
     * other terminal state.
     * <p>
     * Because {@link ProcessRunnerState#INTERRUPTED} is a terminal state, the
     * runner's state is guaranteed not to change before or while this method is
     * invoked.
     *
     * @param runner
     *        the runner in the state {@link ProcessRunnerState#INTERRUPTED}
     *        after a call to {@link ProcessRunner#interrupt()}.
     */
    public void processInterrupted(ProcessRunner runner);
}
