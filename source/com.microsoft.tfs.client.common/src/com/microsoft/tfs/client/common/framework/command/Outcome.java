// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * An {@link Outcome} represents the outcome of running or executing an
 * {@link ICommand}.
 * </p>
 *
 * <p>
 * There are two components to an {@link Outcome}:
 * <ul>
 * <li>an {@link IStatus} that represents the success or failure of the command,
 * along with any other status-related information such as an error message or
 * code</li>
 * <li>an optional result {@link Object} that represents result data produced by
 * running the command</li>
 * </ul>
 * The result is optional and may be <code>null</code>. The use of the result
 * must be specified by the command that produces the outcome. Generally, if a
 * command makes use of the result, clients of the command will expect to find a
 * certain type of object as the result if the status indicates a successful
 * outcome. If a command chooses not to make use of the result mechanism, it can
 * ignore it and return result data to clients in another way.
 * </p>
 *
 * <p>
 * Using an {@link Outcome} object allows commands to express everything about
 * the outcome of their run in a single object. This makes it easy to write
 * stateless methods that return all important data in a single {@link Outcome}
 * object.
 * </p>
 *
 * <p>
 * {@link Outcome}s are immutable. In addition, this class is not designed to be
 * subclassed.
 * </p>
 */
public final class Outcome {
    /**
     * A static {@link Outcome} that can be used to represent a successful
     * outcome with no result data. This outcome is equivalent to a status of
     * {@link Status#OK_STATUS}.
     */
    public static final Outcome OK_OUTCOME = new Outcome(Status.OK_STATUS);

    /**
     * A static {@link Outcome} that can be used to represent an outcome of
     * cancellation with no result data. This outcome is equivalent to a status
     * of {@link Status#CANCEL_STATUS}.
     */
    public static final Outcome CANCEL_OUTCOME = new Outcome(Status.CANCEL_STATUS);

    private final IStatus status;
    private final Object result;

    /**
     * Creates a new {@link Outcome} with a status of {@link Status#OK_STATUS}
     * and the specified result.
     *
     * @param result
     *        the result data (may be <code>null</code>)
     */
    public Outcome(final Object result) {
        this(Status.OK_STATUS, result);
    }

    /**
     * Creates a new {@link Outcome} with the specified {@link IStatus} and no
     * result.
     *
     * @param status
     *        an {@link IStatus} to use for this outcome (must not be
     *        <code>null</code>)
     */
    public Outcome(final IStatus status) {
        this(status, null);
    }

    /**
     * Creates a new {@link Outcome} with a specified {@link IStatus} and result
     * object.
     *
     * @param status
     *        an {@link IStatus} to use for this outcome (must not be
     *        <code>null</code>)
     * @param result
     *        the result data for this outcome (may be <code>null</code>)
     */
    public Outcome(final IStatus status, final Object result) {
        Check.notNull(status, "status"); //$NON-NLS-1$

        this.status = status;
        this.result = result;
    }

    /**
     * @return the status represented by this outcome as an {@link IStatus}
     *         object (never <code>null</code>)
     */
    public IStatus getStatus() {
        return status;
    }

    /**
     * @return the result {@link Object} for this outcome, or <code>null</code>
     *         if this outcome does not include result data
     */
    public Object getResult() {
        return result;
    }
}
