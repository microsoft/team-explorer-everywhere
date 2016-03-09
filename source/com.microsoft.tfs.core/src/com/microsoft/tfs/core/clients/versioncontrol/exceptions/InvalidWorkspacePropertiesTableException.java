// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

public class InvalidWorkspacePropertiesTableException extends VersionControlException {
    private static final long serialVersionUID = -3784653591449359662L;

    public InvalidWorkspacePropertiesTableException() {
        super();
    }

    public InvalidWorkspacePropertiesTableException(final Exception innerException) {
        super("", innerException); //$NON-NLS-1$
    }
}
