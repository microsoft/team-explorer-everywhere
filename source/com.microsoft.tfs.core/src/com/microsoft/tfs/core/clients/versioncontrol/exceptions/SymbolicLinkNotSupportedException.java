// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * An exception thrown when a change is pended on a symbolic link.
 *
 * @since TEE-SDK-11.0
 */
public final class SymbolicLinkNotSupportedException extends VersionControlException {
    public SymbolicLinkNotSupportedException() {
        super();
    }

    public SymbolicLinkNotSupportedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SymbolicLinkNotSupportedException(final String message) {
        super(message);
    }

    public SymbolicLinkNotSupportedException(final Throwable cause) {
        super(cause);
    }
}
