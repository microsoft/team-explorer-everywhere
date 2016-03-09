// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * An exception thrown when a shelve operation fails.
 *
 * @since TEE-SDK-10.1
 */
public final class ShelveException extends VersionControlException {

    public ShelveException() {
        super();
    }

    public ShelveException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ShelveException(final String message) {
        super(message);
    }

    public ShelveException(final Throwable cause) {
        super(cause);
    }
}
