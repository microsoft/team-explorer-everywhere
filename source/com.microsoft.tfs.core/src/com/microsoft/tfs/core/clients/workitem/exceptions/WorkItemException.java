// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

import com.microsoft.tfs.core.exceptions.TEClientException;

/**
 * Base class for work item tracking client exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class WorkItemException extends TEClientException {
    private static final long serialVersionUID = 1739693918191197964L;

    public WorkItemException() {
        super();
    }

    public WorkItemException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public WorkItemException(final String message) {
        super(message);
    }

    public WorkItemException(final Throwable cause) {
        super(cause);
    }

    private int errorID;

    public WorkItemException(final String message, final Throwable cause, final int errorID) {
        super(message, cause);
        this.errorID = errorID;
    }

    public WorkItemException(final String message, final int errorId) {
        super(message);
        this.errorID = errorId;
    }

    public int getErrorID() {
        return errorID;
    }
}
