// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * An exception thrown when an item cannot be found in your workspace or in the
 * server.
 *
 * @since TEE-SDK-11.0
 */
public final class ItemNotFoundException extends VersionControlException {
    public ItemNotFoundException() {
        super();
    }

    public ItemNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ItemNotFoundException(final String message) {
        super(message);
    }

    public ItemNotFoundException(final Throwable cause) {
        super(cause);
    }
}
