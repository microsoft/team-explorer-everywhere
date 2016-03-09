// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * An exception thrown when an unshelve operation fails.
 *
 * @since TEE-SDK-10.1
 */
public final class UnshelveException extends VersionControlException {

    public UnshelveException() {
        super();
    }

    public UnshelveException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public UnshelveException(final String message) {
        super(message);
    }

    public UnshelveException(final Throwable cause) {
        super(cause);
    }
}
