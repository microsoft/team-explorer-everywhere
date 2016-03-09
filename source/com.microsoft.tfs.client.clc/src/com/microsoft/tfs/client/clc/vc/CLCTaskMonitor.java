// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc;

import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * A {@link TaskMonitor} that really doesn't monitor progress, but provides
 * cancelation. This is used during CLC get operations so control-c (JVM
 * shutdown) can be synchronized and the get cleanly canceled.
 * <p>
 * Cancelation state is shared among all {@link CLCTaskMonitor}s in a chain
 * (those derived via {@link #newSubTaskMonitor(int)}) so that cancelation at
 * any level is visible with {@link #isCanceled()} at all levels.
 */
public class CLCTaskMonitor implements TaskMonitor {
    private final AtomicBoolean canceled;

    public CLCTaskMonitor() {
        this(null);
    }

    /**
     * For use by {@link #newSubTaskMonitor(int)} to share cancelation state.
     *
     * @param canceled
     *        the cancelation state to share with a parent monitor, or
     *        <code>null</code> if this is the root monitor (not a sub monitor)
     */
    private CLCTaskMonitor(final AtomicBoolean canceled) {
        if (canceled == null) {
            this.canceled = new AtomicBoolean(false);
        } else {
            this.canceled = canceled;
        }
    }

    @Override
    public void begin(final String taskName, final int totalWork) {
    }

    @Override
    public void beginWithUnknownTotalWork(final String taskName) {
    }

    @Override
    public void done() {
    }

    @Override
    public boolean isCanceled() {
        return canceled.get();
    }

    @Override
    public TaskMonitor newSubTaskMonitor(final int amount) {
        // Share cancelation state
        return new CLCTaskMonitor(canceled);
    }

    @Override
    public void setCanceled() {
        canceled.set(true);
    }

    @Override
    public void setCurrentWorkDescription(final String description) {
    }

    @Override
    public void setTaskName(final String taskName) {
    }

    @Override
    public void worked(final int amount) {
    }
}
