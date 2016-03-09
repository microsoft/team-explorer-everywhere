// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

public class WorkspaceVersionTableException extends VersionControlException {
    private static final long serialVersionUID = -371772430674169119L;

    public WorkspaceVersionTableException(final String message) {
        super(message, null);
    }

    public WorkspaceVersionTableException(final Exception innerException) {
        super(null, innerException);
    }

    public WorkspaceVersionTableException(final String message, final Exception exception) {
        super(message, exception);
    }
}
