// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * @since TEE-SDK-11.0
 */
public abstract class WorkspaceException extends VersionControlException {
    public WorkspaceException() {
        super();
    }

    public WorkspaceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public WorkspaceException(final String message) {
        super(message);
    }

    public WorkspaceException(final Throwable cause) {
        super(cause);
    }
}
