// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link ExtendedStatus} wraps an {@link IStatus} and extends it with the
 * addition of a set of flags. The flags can be used to express information
 * specifying how the status should be handled.
 * </p>
 *
 * <p>
 * Code that produces an {@link IStatus} and wants control over how the status
 * should be handled should wrap its {@link IStatus} in an
 * {@link ExtendedStatus} and set the appropriate flags. Code that handles an
 * {@link IStatus} should check to see if the status is an instance of
 * {@link ExtendedStatus} and respect the flags if it is.
 * </p>
 *
 * <p>
 * {@link ExtendedStatus} flags are defined by static constants defined on this
 * class. The flags can be bitwise ORed together.
 * </p>
 */
public class ExtendedStatus implements IStatus {
    /**
     * A constant indicating no flags are set.
     */
    public static final int NONE = 0;

    /**
     * A flag that indicates the status should be logged to the Eclipse Platform
     * log.
     */
    public static final int LOG_TO_PLATFORM_LOG = 1 << 0;

    /**
     * A flag that indicates the status should be logged to a private log - for
     * example, a log4j log kept by a specific plugin.
     */
    public static final int LOG_TO_PRIVATE_LOG = 1 << 1;

    /**
     * A flag that indicates the status should be shown in the console.
     */
    public static final int SHOW_MESSAGE_IN_CONSOLE = 1 << 2;

    /**
     * A flag that indicates the status should be shown in a dialog. For
     * example, if an {@link ExtendedStatus} has {@link IStatus#ERROR} severity
     * and this flag is set, it could be shown in an {@link ErrorDialog}.
     */
    public static final int SHOW_MESSAGE_IN_DIALOG = 1 << 3;

    /**
     * A flag that indicates that the status message was derived directly from
     * the exception. This prevents redundancy in error dialogs -- it will only
     * display the message, not the exception's message. This is for UI only,
     * sources that use the exception (for example, logging the stacktrace)
     * should ignore this.
     */
    public static final int MESSAGE_FROM_EXCEPTION = 1 << 4;

    /**
     * Combines together all of the <code>SHOW*</code> flags. Indicates that the
     * status should be shown wherever possible (in a dialog, in the console,
     * ...).
     */
    public static final int SHOW = SHOW_MESSAGE_IN_CONSOLE | SHOW_MESSAGE_IN_DIALOG;

    /**
     * Combines together all of the <code>LOG*</code> flags. Indicates that the
     * status should be logged wherever possible (in the Platform log, in a
     * private log, ...).
     */
    public static final int LOG = LOG_TO_PLATFORM_LOG | LOG_TO_PRIVATE_LOG;

    private final IStatus status;
    private final int flags;

    /**
     * Creates a new {@link ExtendedStatus} with the specified flags that wraps
     * the supplied {@link IStatus}. All of the {@link IStatus} interface
     * methods will be answered by delegating to the wrapped status.
     *
     * @param status
     *        the {@link IStatus} to wrap (must not be <code>null<code>)
     * @param flags
     *        the extended status flags, or {@link #NONE} if there are none
     */
    public ExtendedStatus(final IStatus status, final int flags) {
        Check.notNull(status, "status"); //$NON-NLS-1$
        this.status = status;
        this.flags = flags;
    }

    /**
     * @param mask
     *        a bitmask of extended status flags
     * @return <code>true</code> if this {@link ExtendedStatus}</code> has all
     *         of the specified flags set
     */
    public boolean hasFlags(final int mask) {
        return (flags & mask) > 0;
    }

    /**
     * @return the extended status flags of this {@link ExtendedStatus}, or
     *         {@link #NONE} if this {@link ExtendedStatus} has no flags set
     */
    public int getFlags() {
        return flags;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getChildren()
     */
    @Override
    public IStatus[] getChildren() {
        return status.getChildren();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getCode()
     */
    @Override
    public int getCode() {
        return status.getCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getException()
     */
    @Override
    public Throwable getException() {
        return status.getException();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getMessage()
     */
    @Override
    public String getMessage() {
        return status.getMessage();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getPlugin()
     */
    @Override
    public String getPlugin() {
        return status.getPlugin();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#getSeverity()
     */
    @Override
    public int getSeverity() {
        return status.getSeverity();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#isMultiStatus()
     */
    @Override
    public boolean isMultiStatus() {
        return status.isMultiStatus();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#isOK()
     */
    @Override
    public boolean isOK() {
        return status.isOK();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IStatus#matches(int)
     */
    @Override
    public boolean matches(final int severityMask) {
        return status.matches(severityMask);
    }
}
