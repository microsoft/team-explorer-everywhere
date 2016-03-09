// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * An exception thrown when a path is cloaked.
 *
 * @since TEE-SDK-11.0
 */
public final class ItemCloakedException extends MappingException {
    public ItemCloakedException() {
        super();
    }

    public ItemCloakedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ItemCloakedException(final String message) {
        super(message);
    }

    public ItemCloakedException(final Throwable cause) {
        super(cause);
    }
}
