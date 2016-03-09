// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

public class RepositoryPathTooLongException extends VersionControlException {
    private static final long serialVersionUID = 548641583443402368L;

    public RepositoryPathTooLongException(final String message) {
        super(message);
    }
}
