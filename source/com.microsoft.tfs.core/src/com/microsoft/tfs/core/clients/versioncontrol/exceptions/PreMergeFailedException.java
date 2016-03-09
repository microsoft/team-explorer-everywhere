// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * Exception thrown when the pre-merge process fails. The pre-merge process can
 * fail because file encodings are incompatible (one or more files is a "binary"
 * file).
 *
 * @since TEE-SDK-10.1
 */
public class PreMergeFailedException extends VersionControlException {
    public PreMergeFailedException() {
        super();
    }

    public PreMergeFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PreMergeFailedException(final String message) {
        super(message);
    }

    public PreMergeFailedException(final Throwable cause) {
        super(cause);
    }
}
