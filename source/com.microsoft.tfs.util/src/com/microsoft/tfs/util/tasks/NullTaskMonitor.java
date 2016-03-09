// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.tasks;

/**
 * <p>
 * {@link NullTaskMonitor} is a do-nothing implementation of the
 * {@link TaskMonitor} interface. Since {@link TaskMonitorService} never returns
 * a <code>null</code> {@link TaskMonitor} when one is requested, it returns a
 * {@link NullTaskMonitor} if a {@link TaskMonitor} has not been set on the
 * {@link TaskMonitorService}.
 * </p>
 *
 * <p>
 * {@link NullTaskMonitor} is stateless and a singleton instance is shared for
 * all uses. {@link NullTaskMonitor}'s implementation of the
 * {@link TaskMonitor#isCanceled()} method always returns <code>false</code>.
 * Calling {@link #setCanceled()} does not affect the result of
 * {@link #isCanceled()}.
 * </p>
 */
public class NullTaskMonitor implements TaskMonitor {
    /**
     * The singleton instance of {@link NullTaskMonitor}. Note that
     * {@link NullTaskMonitor} is stateless - this instance can be shared for
     * all uses.
     */
    public static final NullTaskMonitor INSTANCE = new NullTaskMonitor();

    /**
     * Private constructor to enforce the singleton.
     */
    private NullTaskMonitor() {

    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#begin(java.lang.String,
     * int)
     */
    @Override
    public void begin(final String taskName, final int totalWork) {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#beginUnknownSize(java.lang.
     * String )
     */
    @Override
    public void beginWithUnknownTotalWork(final String taskName) {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#done()
     */
    @Override
    public void done() {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#isCanceled()
     */
    @Override
    public boolean isCanceled() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#setCanceled()
     */
    @Override
    public void setCanceled() {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#newSubTaskMonitor(int)
     */
    @Override
    public TaskMonitor newSubTaskMonitor(final int amount) {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.tasks.TaskMonitor#setCurrentWorkDescription(java
     * .lang.String)
     */
    @Override
    public void setCurrentWorkDescription(final String description) {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#worked(int)
     */
    @Override
    public void worked(final int amount) {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.tasks.TaskMonitor#setTaskName(java.lang.String)
     */
    @Override
    public void setTaskName(final String taskName) {
    }
}
