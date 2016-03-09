// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * An exception thrown when a path is not mapped to a working folder.
 *
 * @since TEE-SDK-11.0
 */
public final class ItemNotMappedException extends MappingException {
    public ItemNotMappedException() {
        super();
    }

    public ItemNotMappedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ItemNotMappedException(final String message) {
        super(message);
    }

    public ItemNotMappedException(final Throwable cause) {
        super(cause);
    }
}
