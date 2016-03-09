// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

public class InvalidPendingChangeTableException extends VersionControlException {
    private static final long serialVersionUID = -3065374203267638696L;

    public InvalidPendingChangeTableException() {
        super();
    }

    public InvalidPendingChangeTableException(final Exception innerException) {
        super("", innerException); //$NON-NLS-1$
    }
}
