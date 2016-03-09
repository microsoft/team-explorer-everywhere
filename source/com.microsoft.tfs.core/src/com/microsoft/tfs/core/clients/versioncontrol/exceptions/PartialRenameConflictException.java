// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

public class PartialRenameConflictException extends VersionControlException {
    private static final long serialVersionUID = 4313557195380720542L;

    public PartialRenameConflictException(final String message) {
        super(message);
    }
}
