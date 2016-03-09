// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * @since TEE-SDK-11.0
 */
public class WorkspaceNotFoundException extends WorkspaceException {
    public WorkspaceNotFoundException() {
        super();
    }

    public WorkspaceNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public WorkspaceNotFoundException(final String message) {
        super(message);
    }

    public WorkspaceNotFoundException(final Throwable cause) {
        super(cause);
    }
}
