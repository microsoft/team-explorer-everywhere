// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

public class InvalidQueuedEditsTableException extends VersionControlException {
    private static final long serialVersionUID = 2708831911802892483L;

    public InvalidQueuedEditsTableException() {
        super();
    }

    public InvalidQueuedEditsTableException(final Exception innerException) {
        super("", innerException); //$NON-NLS-1$
    }
}
