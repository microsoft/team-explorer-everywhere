// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.tasks;

/**
 * <p>
 * A {@link TaskMonitor} provides access to several services useful for
 * implementing long-running tasks. First, a {@link TaskMonitor} allows a
 * long-running task to report on its progress: the total amount of work the
 * task will do, and how much of that work has been done so far. Secondly, a
 * {@link TaskMonitor} allows a long-running task to check whether cancelation
 * has been requested: if it has, the task can finish early.
 * </p>
 *
 * <p>
 * Both of these services are used to communicate across layers of an
 * application. Typically, a user interface layer will provide a
 * {@link TaskMonitor} and a lower layer will implement a long-running task and
 * consume the {@link TaskMonitor}. The user interface layer will receive
 * progress messages from the lower layer through the {@link TaskMonitor} and
 * display some sort of a UI around the progress. The lower layer will receive
 * cancelation messages from the user interface layer and respond to them
 * appropriately.
 * </p>
 *
 * <p>
 * The {@link TaskMonitorService} class is used to manage instances of
 * {@link TaskMonitor}. Typically, the user interface layer will use
 * {@link TaskMonitorService} to make a {@link TaskMonitor} instance available,
 * and the lower layer will use {@link TaskMonitorService} to obtain the
 * {@link TaskMonitor} instance to consume.
 * </p>
 *
 * <p>
 * Note that the cancelation mechanism provided by {@link TaskMonitor} is poll
 * based. In order to be cancel-aware, the long-running task must occasionally
 * call the {@link #isCanceled()} method and return early if <code>true</code>
 * is returned.
 * </p>
 *
 * <p>
 * There are two different {@link String} descriptions associated with a
 * long-running task being monitored: a <i>task name</i> and a <i>current work
 * description</i>. Think of these as a main header and a sub header. Many tasks
 * are based on iterating over a number of work items and doing something with
 * each work item. For these tasks, the task name would be something general
 * (for example "Downloading Files") and the current work description would be
 * changed with each work item that was processed (for example the name of an
 * individual file). Typically, the task name will be set once when either
 * {@link #begin(String, int)} or {@link #beginWithUnknownTotalWork(String)} is
 * called, and the current work description will be set many times (when
 * {@link #setCurrentWorkDescription(String)} is called).
 * </p>
 *
 * <p>
 * {@link TaskMonitor} supports the common pattern of sub-tasks that have their
 * own {@link TaskMonitor} instance. To obtain a new {@link TaskMonitor}
 * instance for a sub-task, call the {@link #newSubTaskMonitor(int)} method.
 * Note that if a {@link TaskMonitor} is canceled any sub-{@link TaskMonitor}s
 * will also be canceled. This is true whether the {@link TaskMonitor} was
 * canceled through the interface by calling {@link #setCanceled()} or by some
 * other means. Also, if a sub-{@link TaskMonitor} is canceled all
 * {@link TaskMonitor} instances in the sub-task tree up to the original parent
 * are canceled.
 * </p>
 *
 * @see TaskMonitorService
 */
public interface TaskMonitor {
    /**
     * <p>
     * Called to indicate the start of the task that this {@link TaskMonitor}
     * instance is monitoring. This method is called when the total number of
     * work units for the task are known.
     * </p>
     *
     * <p>
     * Either this method or the {@link #beginWithUnknownTotalWork(String)}
     * method must be called only once per {@link TaskMonitor} instance, and one
     * or the other must be called before other methods on the instance are
     * called.
     * </p>
     *
     * @param taskName
     *        the name of the task that is beginning, or <code>null</code> if
     *        there is no task name
     * @param totalWork
     *        the total amount of work units that this task will take (must be
     *        non-negative)
     */
    public void begin(String taskName, int totalWork);

    /**
     * <p>
     * Called to indicate the start of the task that this {@link TaskMonitor}
     * instance is monitoring. This method is called when the total number of
     * work units for the task is not known.
     * </p>
     *
     * <p>
     * Either this method or the {@link #begin(String, int)} method must be
     * called only once per {@link TaskMonitor} instance, and one or the other
     * must be called before other methods on the instance are called.
     * </p>
     *
     * @param taskName
     *        the name of the task that is beginning, or <code>null</code> if
     *        there is no task name
     */
    public void beginWithUnknownTotalWork(String taskName);

    /**
     * <p>
     * Called to indicate the end of the task that this {@link TaskMonitor}
     * instance is monitoring. This method <b>must</b> be called, even if the
     * task aborts with an error or is canceled.
     * </p>
     *
     * <p>
     * It is safe to call this method multiple times.
     * </p>
     */
    public void done();

    /**
     * <p>
     * Called to test whether cancelation has been requested for the task that
     * this {@link TaskMonitor} instance is monitoring.
     * </p>
     *
     * <p>
     * To be cancelation-aware, the task should poll this method at regular
     * intervals. If <code>true</code> is returned, the task should return early
     * as quickly as possible.
     * </p>
     *
     * @return <code>true</code> if cancelation has been requested for this task
     */
    public boolean isCanceled();

    /**
     * <p>
     * Sets the cancel state to true for the task this {@link TaskMonitor} is
     * monitoring.
     * </p>
     */
    public void setCanceled();

    /**
     * Called by the task that this {@link TaskMonitor} is monitoring to report
     * progress. This method is typically called multiple times during the task.
     * Each time the number of work units that have been completed since the
     * last call to this method are passed. In other words, the number of work
     * units reported to this method must be non-cumulative.
     *
     * @param amount
     *        a non-negative, non-cumulative amount of work units
     */
    public void worked(int amount);

    /**
     * Called to set the task name of the task being monitored by this
     * {@link TaskMonitor}. Normally there is no reason to call this method,
     * since the task name is set by either the {@link #begin(String, int)} or
     * {@link #beginWithUnknownTotalWork(String)} methods and generally remains
     * constant throughout the lifetime of the task.
     *
     * @param taskName
     *        the task name as described above, or <code>null</code> to clear
     *        the task name
     */
    public void setTaskName(String taskName);

    /**
     * <p>
     * Called to set a more detailed description of the current work being done
     * than the task name alone gives. The current work description is an
     * additional description above and beyond the task name that is specified
     * in the {@link #begin(String, int)},
     * {@link #beginWithUnknownTotalWork(String)}, and/or
     * {@link #setTaskName(String)} methods.
     * </p>
     *
     * <p>
     * Many UI implementations of {@link TaskMonitor} will render the task name
     * as a header and the current work description as a sub-header. Other UI
     * implementations may choose to ignore the current work description or to
     * let it override the task name.
     * </p>
     *
     * @param description
     *        the current work description as described above, or
     *        <code>null</code> to clear the current work description
     */
    public void setCurrentWorkDescription(String description);

    /**
     * <p>
     * Called to obtain a new {@link TaskMonitor} instance to be used in a
     * sub-task of the task being monitored by this {@link TaskMonitor}
     * instance.
     * </p>
     *
     * <p>
     * Note that every call to this method results in a new {@link TaskMonitor}
     * instance, and all of the {@link TaskMonitor} rules apply to each
     * instance. Specifically, one of the <code>begin</code> methods should be
     * called on the new instance, and the {@link #done()} method must be called
     * on the new instance.
     * </p>
     *
     * <p>
     * Each sub-{@link TaskMonitor} uses up sub number of work units of its
     * parent {@link TaskMonitor}. This number is specified as the
     * <code>amount</code> argument to this method. The sub-{@link TaskMonitor}
     * will have its own number of total work units (specified by the
     * {@link #begin(String, int)} method) and internally sub-task progress will
     * be scaled into main-task progress.
     * </p>
     *
     * @param amount
     *        the amount of work units of the main task that the sub-task will
     *        use up (must be non-negative)
     * @return a new {@link TaskMonitor} instance to be used for a sub-task as
     *         described above (never <code>null</code>)
     */
    public TaskMonitor newSubTaskMonitor(int amount);
}