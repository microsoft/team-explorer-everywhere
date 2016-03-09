// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * <p>
 * This class provides an abstract base implementation of {@link FutureStatus}.
 * It is suitable for subclassing for most implementations of
 * {@link FutureStatus}.
 * </p>
 *
 * <p>
 * Most implementations of {@link FutureStatus} are based on delegating the
 * {@link IStatus} interface methods to an intermediate status instance while
 * the {@link ICommand} is running, and then delegating those methods to the
 * actual status returned by the command once it has finished. This is the
 * algorithm encapsulated in this base class.
 * </p>
 *
 * <p>
 * Subclasses must provide implementations of two abstract methods. The
 * {@link FutureStatus#isCompleted()} method should answer <code>true</code> if
 * the command has finished running. The {@link #getCompletedStatus()} should
 * return the {@link IStatus} produced by running the command, and will only be
 * called once {@link FutureStatus#isCompleted()} returns <code>true</code>.
 * </p>
 *
 * <p>
 * Subclasses pass an async object, if there is one, to the constructor of this
 * class. This object is then made available through the
 * {@link #getAsyncObject()} method.
 * </p>
 *
 * @see FutureStatus
 */
public abstract class AbstractFutureStatus implements FutureStatus {
    private static final IStatus WAITING_FOR_COMPLETION_STATUS = Status.OK_STATUS;

    private final Object asyncObject;

    /**
     * Called to obtain the {@link IStatus} produced by running the command.
     * This method is only called once {@link FutureStatus#isCompleted()}
     * returns <code>true</code>.
     *
     * @return the {@link IStatus} described above
     */
    protected abstract IStatus getCompletedStatus();

    /**
     * Called to block, returns the {@link IStatus} produced by running the
     * command. Equivalent to waiting until {@link FutureStatus#isCompleted()}
     * returns <code>true</code>.
     */
    @Override
    public abstract void join();

    /**
     * Creates a new {@link AbstractFutureStatus}. The {@link #getAsyncObject()}
     * method will be answered by returning the specified async object, or
     * <code>null</code> if the parameter is <code>null</code>.
     *
     * @param asyncObject
     *        the async object for this {@link FutureStatus}, or
     *        <code>null</code> if there is none
     */
    protected AbstractFutureStatus(final Object asyncObject) {
        this.asyncObject = asyncObject;
    }

    private IStatus getWrappedStatus() {
        return isCompleted() ? getCompletedStatus() : WAITING_FOR_COMPLETION_STATUS;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.ui.shared.command.FutureStatus#
     * getAsyncObject ()
     */
    @Override
    public Object getAsyncObject() {
        return asyncObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getChildren()
     */
    @Override
    public IStatus[] getChildren() {
        return getWrappedStatus().getChildren();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getCode()
     */
    @Override
    public int getCode() {
        return getWrappedStatus().getCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getException()
     */
    @Override
    public Throwable getException() {
        return getWrappedStatus().getException();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getMessage()
     */
    @Override
    public String getMessage() {
        return getWrappedStatus().getMessage();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getPlugin()
     */
    @Override
    public String getPlugin() {
        return getWrappedStatus().getPlugin();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getSeverity()
     */
    @Override
    public int getSeverity() {
        return getWrappedStatus().getSeverity();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#isMultiStatus()
     */
    @Override
    public boolean isMultiStatus() {
        return getWrappedStatus().isMultiStatus();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#isOK()
     */
    @Override
    public boolean isOK() {
        return getWrappedStatus().isOK();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#matches(int)
     */
    @Override
    public boolean matches(final int severityMask) {
        return getWrappedStatus().matches(severityMask);
    }
}
