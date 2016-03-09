// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * An exception thrown when working folder mappings conflict internally (one
 * mapping is incompatible with another in the same set) or externally (the
 * mappings conflict with another set).
 *
 * @since TEE-SDK-11.0
 */
public final class MappingConflictException extends VersionControlException {
    public MappingConflictException() {
        super();
    }

    public MappingConflictException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MappingConflictException(final String message) {
        super(message);
    }

    public MappingConflictException(final Throwable cause) {
        super(cause);
    }
}
