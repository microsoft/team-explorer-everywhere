// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * An exception thrown when an item's content has been destroyed.
 *
 * @since TEE-SDK-11.0
 */
public final class DestroyedContentUnavailableException extends VersionControlException {
    public DestroyedContentUnavailableException() {
        super();
    }

    public DestroyedContentUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DestroyedContentUnavailableException(final String message) {
        super(message);
    }

    public DestroyedContentUnavailableException(final Throwable cause) {
        super(cause);
    }
}
