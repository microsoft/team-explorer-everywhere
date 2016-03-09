// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * Exception thrown when an item is to be merged with an external merge tool but
 * no tool is configured for the item's extension (or other identifying
 * characteristic).
 *
 * @since TEE-SDK-10.1
 */
public class MergeToolNotConfiguredException extends VersionControlException {
    public MergeToolNotConfiguredException() {
        super();
    }

    public MergeToolNotConfiguredException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MergeToolNotConfiguredException(final String message) {
        super(message);
    }

    public MergeToolNotConfiguredException(final Throwable cause) {
        super(cause);
    }
}
