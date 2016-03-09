// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

public class AutoMergeDisallowedException extends VersionControlException {
    private static final long serialVersionUID = 7393524571483565883L;

    public AutoMergeDisallowedException(final String message) {
        super(message);
    }

    public AutoMergeDisallowedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
