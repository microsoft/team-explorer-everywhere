// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * {@link ProgressMonitorTaskMonitorAdapter} adapts an instance of
 * {@link IProgressMonitor} to the {@link TaskMonitor} interface. Most of the
 * {@link TaskMonitor} methods map in a very straightforward way to
 * {@link TaskMonitor}. Note that the {@link TaskMonitor#newSubTaskMonitor(int)}
 * method is satisfied by using the {@link SubProgressMonitor} class.
 *
 * @see TaskMonitor
 * @see IProgressMonitor
 * @see SubProgressMonitor
 */
public class ProgressMonitorTaskMonitorAdapter implements TaskMonitor {
    /**
     * The {@link IProgressMonitor} this adapter is wrapping (never
     * <code>null</code>).
     */
    private final IProgressMonitor progressMonitor;

    /**
     * Style bits that are passed to the {@link SubProgressMonitor} constructor
     * when a sub task monitor is requested.
     */
    private final int subProgressMonitorStyle;

    /**
     * Creates a new {@link ProgressMonitorTaskMonitorAdapter}, wrapping the
     * specified {@link IProgressMonitor}.
     *
     * @param progressMonitor
     *        an {@link IProgressMonitor} to wrap (must not be <code>null</code>
     *        )
     */
    public ProgressMonitorTaskMonitorAdapter(final IProgressMonitor progressMonitor) {
        this(progressMonitor, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
    }

    /**
     * Creates a new {@link ProgressMonitorTaskMonitorAdapter}, wrapping the
     * specified {@link IProgressMonitor}. If a sub-task monitor is requested by
     * calling the {@link #newSubTaskMonitor(int)} method, the
     * {@link SubProgressMonitor} created to satisfy the request will be passed
     * the specified style bits.
     *
     * @param progressMonitor
     *        an {@link IProgressMonitor} to wrap (must not be <code>null</code>
     *        )
     * @param subProgressMonitorStyle
     *        {@link SubProgressMonitor} style bits used to create sub-task
     *        monitors
     */
    public ProgressMonitorTaskMonitorAdapter(
        final IProgressMonitor progressMonitor,
        final int subProgressMonitorStyle) {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        this.progressMonitor = progressMonitor;
        this.subProgressMonitorStyle = subProgressMonitorStyle;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#begin(java.lang.String,
     * int)
     */
    @Override
    public void begin(final String taskName, final int totalWork) {
        progressMonitor.beginTask(taskName, totalWork);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.tasks.TaskMonitor#beginWithUnknownTotalWork(java
     * .lang.String)
     */
    @Override
    public void beginWithUnknownTotalWork(final String taskName) {
        progressMonitor.beginTask(taskName, IProgressMonitor.UNKNOWN);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#done()
     */
    @Override
    public void done() {
        progressMonitor.done();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#isCanceled()
     */
    @Override
    public boolean isCanceled() {
        return progressMonitor.isCanceled();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#setCanceled()
     */
    @Override
    public void setCanceled() {
        progressMonitor.setCanceled(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#newSubTaskMonitor(int)
     */
    @Override
    public TaskMonitor newSubTaskMonitor(final int amount) {
        final SubProgressMonitor subMonitor = new SubProgressMonitor(progressMonitor, amount, subProgressMonitorStyle);
        return new ProgressMonitorTaskMonitorAdapter(subMonitor);
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
        progressMonitor.subTask(description);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.tasks.TaskMonitor#setTaskName(java.lang.String)
     */
    @Override
    public void setTaskName(final String taskName) {
        progressMonitor.setTaskName(taskName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.tasks.TaskMonitor#worked(int)
     */
    @Override
    public void worked(final int amount) {
        progressMonitor.worked(amount);
    }
}
