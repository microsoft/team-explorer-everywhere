// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * <p>
 * A {@link FutureStatus} is a specialized {@link IStatus} returned by
 * non-blocking {@link ICommandExecutor}s. A {@link FutureStatus} represents the
 * outcome of an {@link ICommand} that may or may not have finished running.
 * </p>
 *
 * <p>
 * If the {@link ICommand} has not yet finished running,
 * {@link FutureStatus#isCompleted()} returns <code>false</code> and all of the
 * {@link IStatus} interface methods behave exactly like
 * {@link Status#OK_STATUS}. Once the command finishes running,
 * {@link FutureStatus#isCompleted()} returns <code>true</code> and all of the
 * {@link IStatus} interface methods behave exactly like the actual status
 * returned by the command.
 * </p>
 *
 * <p>
 * Additionally, {@link FutureStatus} exposes an <b>async object</b> to clients
 * (the {@link #getAsyncObject()} method). The type of this object is determined
 * by the {@link FutureStatus} implementation and may be <code>null</code>.
 * {@link FutureStatus} implementations use this object to make additional data
 * available to client code. For more information, see the comments on
 * {@link #getAsyncObject()} below.
 * </p>
 *
 * <p>
 * Note to implementors: consider subclassing {@link AbstractFutureStatus} if
 * you are implementing this interface.
 * </p>
 *
 * @see ICommand
 * @see ICommandExecutor
 * @see AbstractFutureStatus
 */
public interface FutureStatus extends IStatus {
    /**
     * @return <code>true</code> if the {@link ICommand} has finished running
     */
    public boolean isCompleted();

    /**
     * Called to block, returns the {@link IStatus} produced by running the
     * command. Equivalent to waiting until {@link FutureStatus#isCompleted()}
     * returns <code>true</code>.
     */
    public void join();

    /**
     * <p>
     * This method allows for clients to obtain a specific aynchronous object
     * that is related to this {@link FutureStatus}. If no such object exists
     * for this {@link FutureStatus}, this method returns <code>null</code>.
     * </p>
     *
     * <p>
     * For instance, the {@link FutureStatus} instances returned from
     * {@link JobCommandExecutor}s will return the {@link Job} that is running
     * the command. {@link FutureStatus} instances returned from
     * {@link ThreadCommandExecutor}s will return the {@link Thread} that is
     * running the command. Client code that uses these executors could cast the
     * value returned by this method to {@link Job} or {@link Thread}.
     * </p>
     *
     * @return an asynchronous {@link Object} as described above, or
     *         <code>null</code>
     */
    public Object getAsyncObject();
}
