// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link MultiCommandHelper} is a helper class used to help compose together
 * multiple {@link ICommand}s or subtasks as part of a single command.
 * </p>
 *
 * <p>
 * {@link MultiCommandHelper} manages an {@link IProgressMonitor} instance that
 * is associated with the main task. When a subtask or subcommand is begun, a
 * new {@link SubProgressMonitor} is created to track the progress of the
 * subtask. When subcommands are run, their results ({@link IStatus} objects)
 * are added to a {@link MultiStatus} managed by this {@link MultiCommandHelper}
 * .
 * </p>
 *
 * <p>
 * A {@link MultiCommandHelper} instance has a <i>scale factor</i> - an integer
 * that ticks are scaled by to give better progress monitoring. The scale factor
 * can be specified at construction time, or the default (
 * {@link #DEFAULT_SCALE_FACTOR}) can be used. When work units are specified to
 * a {@link MultiCommandHelper} method, the work units are scaled by the scale
 * factor before being passed to an {@link IProgressMonitor} method. For
 * example, assume the scale factor is 100. Calling
 * {@link #beginMainTask(String, int)} with 5 total work units will trigger a
 * call to {@link IProgressMonitor#beginTask(String, int)} with 5 * 100 = 500
 * total work units. Calling {@link #beginSubtask(String, int)} with 1 work unit
 * will create a {@link SubProgressMonitor} that takes 1 * 100 = 100 ticks off
 * of the parent {@link IProgressMonitor}. If this scaling is not desired, pass
 * <code>1</code> for the scale factor to effectively disable internal scaling.
 * </p>
 *
 * <p>
 * When running sub-commands, a <i>continuable severities</i> parameter controls
 * what happens when the command finishes. Continuable severities can be
 * specified for each sub command. When constructing a
 * {@link MultiCommandHelper}, default continuable severities are specified
 * (defaults to {@link #DEFAULT_CONTINUABLE_SEVERITIES}). If a subcommand
 * returns with a non-continuable {@link IStatus}, {@link MultiCommandHelper}
 * throws a {@link CoreException} to stop the main task execution.
 * </p>
 *
 * <p>
 * Typically, clients of this class are themselves {@link ICommand}s. In their
 * {@link ICommand#run(IProgressMonitor)} method, they instantiate an instance
 * of {@link MultiCommandHelper}. That instance can be used to begin subtasks or
 * run subcommands. To begin a subtask, the client calls
 * {@link #beginSubtask(String)} or one of its overloads. This method returns an
 * {@link IProgressMonitor} for the subtask to use to check for cancellation and
 * report progress. To run a subcommand, the client code creates the command to
 * be run and calls {@link #runSubCommand(ICommand)} or one of its overloads.
 * See the documentation on these methods below for more information.
 * </p>
 *
 * <p>
 * For an example usage of {@link MultiCommandHelper}, see {@link CommandList}.
 * This command holds a list of subcommands and uses a
 * {@link MultiCommandHelper} to run them in sequence.
 * </p>
 *
 * @see ICommand
 * @see IProgressMonitor
 * @see SubProgressMonitor
 * @see MultiStatus
 * @see CommandList
 */
public class MultiCommandHelper {
    /**
     * The default scale factor used by {@link MultiCommandHelper}s.
     */
    private static final int DEFAULT_SCALE_FACTOR = 100;

    /**
     * The default continuable severities used by {@link MultiCommandHelper}s.
     * By default, only the {@link IStatus#OK} severity is considered
     * continuable.
     */
    private static final int[] DEFAULT_CONTINUABLE_SEVERITIES = new int[] {
        IStatus.OK
    };

    /**
     * The main task {@link IProgressMonitor} managed by this
     * {@link MultiCommandHelper} (never <code>null</code>).
     */
    private final IProgressMonitor progressMonitor;

    /**
     * A list of all statuses that subcommands have returned. Used to compute
     * resultant status.
     */
    private final List statuses = new ArrayList();
    private IStatus nonContinuableStatus;

    /**
     * The scale factor used when scaling work units.
     */
    private final int scaleFactor;

    /**
     * The default continuable severities that will be used if continuable
     * severities are not specified when running a subcommand (never
     * <code>null</code>).
     */
    private final int[] defaultContinuableSeverities;

    /**
     * We can rollback previously executed commands when one fails.
     */
    private boolean rollback = false;

    /**
     * The list of commands we have successfully executed (which would be rolled
     * back.)
     */
    private final List successfulCommands = new ArrayList();

    /**
     * Creates a new {@link MultiCommandHelper} with a default scale factor and
     * continuable severities. To begin the main task, call
     * {@link #beginMainTask(String, int)}.
     *
     * @param progressMonitor
     *        the main task progress monitor (must not be <code>null</code>)
     */
    public MultiCommandHelper(final IProgressMonitor progressMonitor) {
        this(progressMonitor, DEFAULT_SCALE_FACTOR, null);
    }

    /**
     * Creates a new {@link MultiCommandHelper} with the specified scale factor
     * and default continuable severities. To begin the main task, call
     * {@link #beginMainTask(String, int)}.
     *
     * @param progressMonitor
     *        the main task progress monitor (must not be <code>null</code>)
     * @param scaleFactor
     *        the scale factor that this {@link MultiCommandHelper} will use -
     *        if <code>0</code> or less, <code>1</code> will be used instead
     */
    public MultiCommandHelper(final IProgressMonitor progressMonitor, final int scaleFactor) {
        this(progressMonitor, scaleFactor, null);
    }

    /**
     * Creates a new {@link MultiCommandHelper} with the specified continuable
     * severities and a default scale factor. To begin the main task, call
     * {@link #beginMainTask(String, int)}.
     *
     * @param progressMonitor
     *        the main task progress monitor (must not be <code>null</code>)
     * @param continuableSeverities
     *        the default continuable severities, or <code>null</code> to use
     *        {@link #DEFAULT_CONTINUABLE_SEVERITIES}
     */
    public MultiCommandHelper(final IProgressMonitor progressMonitor, final int[] continuableSeverities) {
        this(progressMonitor, DEFAULT_SCALE_FACTOR, continuableSeverities);
    }

    /**
     * Creates a new {@link MultiCommandHelper}. To begin the main task, call
     * {@link #beginMainTask(String, int)}.
     *
     * @param progressMonitor
     *        the main task progress monitor (must not be <code>null</code>)
     * @param scaleFactor
     *        the scale factor that this {@link MultiCommandHelper} will use -
     *        if <code>0</code> or less, <code>1</code> will be used instead
     * @param continuableSeverities
     *        the default continuable severities, or <code>null</code> to use
     *        {@link #DEFAULT_CONTINUABLE_SEVERITIES}
     */
    public MultiCommandHelper(final IProgressMonitor progressMonitor, int scaleFactor, int[] continuableSeverities) {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        this.progressMonitor = progressMonitor;

        if (scaleFactor <= 0) {
            scaleFactor = 1;
        }

        this.scaleFactor = scaleFactor;

        if (continuableSeverities == null) {
            continuableSeverities = DEFAULT_CONTINUABLE_SEVERITIES;
        }

        defaultContinuableSeverities = continuableSeverities;
    }

    /**
     * Configures whether we "rollback" previously executed commands when one
     * fails.
     *
     * @param rollback
     *        true to rollback previous commands when one fails, false to ignore
     */
    public void setRollback(final boolean rollback) {
        this.rollback = rollback;
    }

    /**
     * Queries whether we "rollback" previously executed commands when one
     * fails.
     *
     * @return true if we rollback successful commands when one fails, false if
     *         we ignore them
     */
    public boolean getRollback() {
        return rollback;
    }

    /**
     * Obtains the scale factor being used by this {@link MultiCommandHelper}.
     * See the class documentation for an explanation of the scale factor.
     *
     * @return the scale factor of this {@link MultiCommandHelper}
     */
    public int getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Calls {@link IProgressMonitor#beginTask(String, int)} on the main task
     * progress monitor. The specified work units are scaled by the scale factor
     * ({@link #getScaleFactor()}) unless {@link IProgressMonitor#UNKNOWN} is
     * specified. If <code>totalWork</code> is specified as <code>0</code> or
     * less, {@link IProgressMonitor#UNKNOWN} is assumed.
     *
     * @param mainTaskName
     *        the main task name (must not be <code>null</code>)
     * @param totalWork
     *        the total work (will be scaled by the scale factor), or
     *        {@link IProgressMonitor#UNKNOWN}
     */
    public void beginMainTask(final String mainTaskName, int totalWork) {
        Check.notNull(mainTaskName, "mainTaskName"); //$NON-NLS-1$

        if (totalWork <= 0) {
            totalWork = IProgressMonitor.UNKNOWN;
        } else {
            totalWork *= scaleFactor;
        }

        progressMonitor.beginTask(mainTaskName, totalWork);
    }

    /**
     * Adds an {@link IStatus} into the {@link MultiStatus} held by this
     * {@link MultiCommandHelper}. This method is typically called when a
     * subtask has completed and produces an {@link IStatus}. This method does
     * <b>not</b> need to be called when using the
     * {@link #runSubCommand(ICommand)} methods - when running a subcommand, the
     * subcommand's status is automatically merged in.
     *
     * @param statusToMerge
     *        an {@link IStatus} to merge (must not be <code>null</code>)
     */
    public void addSubStatus(final IStatus statusToAdd) {
        Check.notNull(statusToAdd, "statusToAdd"); //$NON-NLS-1$

        statuses.add(statusToAdd);
    }

    /**
     * Obtains the best status held by this {@link MultiCommandHelper}. This is
     * defined as:
     *
     * If execution was cancelled, {@link Status#CANCEL_STATUS} is returned.
     *
     * If one of the subcommands returned a status whose severity was
     * non-continuable, then that status is returned. (Ie, this is the status
     * that stopped multi command execution.)
     *
     * If all statuses were OK, we return {@link Status#OK_STATUS}.
     *
     * Finally, any non-OK statuses will be returned as a MultiStatus.
     *
     * @return an {@link IStatus} as described above (never <code>null</code> )
     */
    public IStatus getStatus(final String errorMessage) {
        if (isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        if (nonContinuableStatus != null) {
            return nonContinuableStatus;
        }

        final List interestingStatuses = new ArrayList();
        for (final Iterator i = statuses.iterator(); i.hasNext();) {
            final IStatus status = (IStatus) i.next();

            if (!status.isOK()) {
                interestingStatuses.add(status);
            }
        }

        if (interestingStatuses.size() == 0) {
            return Status.OK_STATUS;
        }

        return new MultiStatus(
            TFSCommonClientPlugin.PLUGIN_ID,
            0,
            (IStatus[]) interestingStatuses.toArray(new IStatus[interestingStatuses.size()]),
            errorMessage,
            null);
    }

    /**
     * @return <code>true</code> if the main task {@link IProgressMonitor}
     *         passed in this {@link MultiCommandHelper}'s constructor is
     *         canceled
     */
    public boolean isCanceled() {
        return progressMonitor.isCanceled();
    }

    /**
     * <p>
     * Obtains a sub-{@link IProgressMonitor} to be given to a subtask. A new
     * {@link SubProgressMonitor} is created, and
     * {@link IProgressMonitor#subTask(String)} is called on the main progress
     * monitor with the given name.
     * </p>
     *
     * <p>
     * This convenience method assumes that the sub task will take 1 work unit
     * (scaled by the scale factor) off of the main task monitor.
     * </p>
     *
     * <p>
     * If the subtask is implemented by an {@link ICommand}, use the
     * {@link #runSubCommand(ICommand)} method or one of its overloads instead.
     * </p>
     *
     * @param name
     *        the name to use when calling
     *        {@link IProgressMonitor#subTask(String)} on the main progress
     *        monitor, or <code>null</code> to not call
     *        {@link IProgressMonitor#subTask(String)}
     * @return a new {@link IProgressMonitor} as described above (never
     *         <code>null</code>)
     */
    public IProgressMonitor beginSubtask(final String name) {
        return beginSubtask(name, 1);
    }

    /**
     * <p>
     * Obtains a sub-{@link IProgressMonitor} to be given to a subtask. A new
     * {@link SubProgressMonitor} is created, and
     * {@link IProgressMonitor#subTask(String)} is called on the main progress
     * monitor with the given name.
     * </p>
     *
     * <p>
     * The specified work units (scaled by the scale factor) are taken off of
     * the main task monitor by the subtask.
     * </p>
     *
     * <p>
     * If the subtask is implemented by an {@link ICommand}, use the
     * {@link #runSubCommand(ICommand)} method or one of its overloads instead.
     * </p>
     *
     * @param name
     *        the name to use when calling
     *        {@link IProgressMonitor#subTask(String)} on the main progress
     *        monitor, or <code>null</code> to not call
     *        {@link IProgressMonitor#subTask(String)}
     * @param numTicks
     *        the number of ticks the sub progress monitor returned by this
     *        method should take off of the main progress monitor (will be
     *        scaled by the scale factor)
     * @return a new {@link IProgressMonitor} as described above (never
     *         <code>null</code>)
     */
    public IProgressMonitor beginSubtask(final String name, final int numTicks) {
        final SubProgressMonitor subMonitor = new SubProgressMonitor(progressMonitor, numTicks * scaleFactor);

        if (name != null) {
            progressMonitor.subTask(name);
        }

        return subMonitor;
    }

    /**
     * <p>
     * Runs a supplied {@link ICommand} as a subcommand. The command will be run
     * with an {@link IProgressMonitor} that is a submonitor of the main
     * progress monitor held by this class.
     * </p>
     *
     * <p>
     * This convenience method assumes that the subcommand will take 1 work unit
     * (scaled by the scale factor) off of the main task monitor.
     * </p>
     *
     * @param command
     *        the {@link ICommand} to run as a subcommand (must not be
     *        <code>null</code>)
     * @param numTicks
     *        the number of ticks (scaled by the scale factor) off of the main
     *        {@link IProgressMonitor} the subcommand should use
     * @return an {@link IStatus} (never <code>null</code>)
     */
    public IStatus runSubCommand(final ICommand command) throws Exception {
        return runSubCommand(command, 1);
    }

    /**
     * <p>
     * Runs a supplied {@link ICommand} as a subcommand. The command will be run
     * with an {@link IProgressMonitor} that is a submonitor of the main
     * progress monitor held by this class.
     * </p>
     *
     * <p>
     * The specified work units (scaled by the scale factor) are taken off of
     * the main task monitor by the subcommand.
     * </p>
     *
     * @param command
     *        the {@link ICommand} to run as a subcommand (must not be
     *        <code>null</code>)
     * @param numTicks
     *        the number of ticks (scaled by the scale factor) off of the main
     *        {@link IProgressMonitor} the subcommand should use
     * @return an {@link IStatus} (never <code>null</code>)
     */
    public IStatus runSubCommand(final ICommand command, final int numTicks) throws Exception {
        return runSubCommand(command, numTicks, true, null);
    }

    /**
     * <p>
     * Runs a supplied {@link ICommand} as a subcommand. The command will be run
     * with an {@link IProgressMonitor} that is a submonitor of the main
     * progress monitor held by this class.
     * </p>
     *
     * <p>
     * The specified work units (scaled by the scale factor) are taken off of
     * the main task monitor by the subcommand.
     * </p>
     *
     * <p>
     * The continuable severities parameter, if specified, controls what happens
     * when the command finishes. See the class documentation for an explanation
     * of continuable severities.
     * </p>
     *
     * @param command
     *        the {@link ICommand} to run as a subcommand (must not be
     *        <code>null</code>)
     * @param numTicks
     *        the number of ticks (scaled by the scale factor) off of the main
     *        {@link IProgressMonitor} the subcommand should use
     * @param checkForCancelation
     *        if <code>true</code>, checks the main progress monitor for
     *        cancellation before running the subcommand
     * @param continuableSeverities
     *        controls the return of this method as described above
     * @return an {@link IStatus} (never <code>null</code>)
     */
    public IStatus runSubCommand(
        final ICommand command,
        final int numTicks,
        final boolean checkForCancelation,
        int[] continuableSeverities) throws CoreException {
        Check.notNull(command, "command"); //$NON-NLS-1$

        if (continuableSeverities == null) {
            continuableSeverities = defaultContinuableSeverities;
        }

        IStatus status;

        if (checkForCancelation && isCanceled()) {
            status = Status.CANCEL_STATUS;
        } else {
            final IProgressMonitor subMonitor = beginSubtask(command.getName(), numTicks);

            try {
                status = new CommandExecutor(subMonitor).execute(command);
            } finally {
                subMonitor.done();
            }
        }

        addSubStatus(status);

        if (isContinuable(status, continuableSeverities)) {
            successfulCommands.add(command);
            return status;
        } else {
            /* record the status that caused us to fail */
            nonContinuableStatus = status;

            if (rollback) {
                /* TODO: use a better title here? */
                final IProgressMonitor subMonitor =
                    beginSubtask(
                        Messages.getString("MultiCommandHelper.ProgressMonitorText"), //$NON-NLS-1$
                        successfulCommands.size());
                rollback(subMonitor);
            }

            throw new CoreException(status);
        }
    }

    private void rollback(final IProgressMonitor monitor) {
        if (!rollback) {
            return;
        }

        // Roll back backwards
        for (int i = successfulCommands.size() - 1; i >= 0; i--) {
            final Command command = (Command) successfulCommands.get(i);

            if (!(command instanceof UndoableCommand)) {
                continue;
            }

            final UndoableCommand undoableCommand = (UndoableCommand) command;

            try {
                undoableCommand.rollback(monitor);
            } catch (final Exception e) {
                // suppress
            }
        }
    }

    /**
     * @return <code>true</code> if the specified status's severity is contained
     *         in the severities array
     */
    private static boolean isContinuable(final IStatus status, final int[] severities) {
        final int severity = status.getSeverity();
        for (int i = 0; i < severities.length; i++) {
            if (severities[i] == severity) {
                return true;
            }
        }
        return false;
    }
}