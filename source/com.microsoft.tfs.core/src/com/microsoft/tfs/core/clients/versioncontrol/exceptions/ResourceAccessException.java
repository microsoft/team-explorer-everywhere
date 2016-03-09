// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

public class ResourceAccessException extends VersionControlException {
    private static final long serialVersionUID = -5636924165320446517L;

    public ResourceAccessException() {
        super();
    }

    public ResourceAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ResourceAccessException(final String message) {
        super(message);
    }

    public ResourceAccessException(final Throwable cause) {
        super(cause);
    }
}
