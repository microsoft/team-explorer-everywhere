// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

/**
 * Exception thrown when a work item cannot be saved.
 *
 * @since TEE-SDK-10.1
 */
public class UnableToSaveException extends WorkItemException {
    private static final long serialVersionUID = -6410964736182848572L;

    public UnableToSaveException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public UnableToSaveException(final String message) {
        super(message);
    }

    public UnableToSaveException(final Throwable cause) {
        super(cause);
    }
}
