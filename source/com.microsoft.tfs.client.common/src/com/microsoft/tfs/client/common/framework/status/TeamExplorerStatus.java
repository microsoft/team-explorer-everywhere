// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.status;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Exists as a bridge for command exceptions to be handled better in the UI. We
 * create Status objects from command exceptions where the status message is
 * identical to (or better than) the exception message. If you include the
 * exception in the status, then ErrorDialog will display both the status
 * message and the exception message, leading to redundant information. If you
 * do not include it, consumers of the Status cannot access the original
 * exception. Thus, we use this bridge which sets the status exception to null
 * (for the UI) but allows callers to access.
 *
 * TODO: this is a hack. We should revisit our {@link IStatus} story post-4.0.
 */
public class TeamExplorerStatus extends Status {
    private final Throwable exception;

    public TeamExplorerStatus(
        final int severity,
        final String pluginId,
        final int code,
        final String message,
        final Throwable exception) {
        super(severity, pluginId, code, message, null);

        this.exception = exception;
    }

    public Throwable getTeamExplorerException() {
        return exception;
    }

    /**
     * @return a {@link Status} object containing this object's information and
     *         having the exception in the normal place (accessible via
     *         {@link Status#getException()})
     */
    public Status toNormalStatus() {
        return new Status(getSeverity(), getPlugin(), getCode(), getMessage(), this.exception);
    }
}
