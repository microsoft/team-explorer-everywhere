// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * @since TEE-SDK-11.0
 */
public abstract class MappingException extends VersionControlException {
    public MappingException() {
        super();
    }

    public MappingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MappingException(final String message) {
        super(message);
    }

    public MappingException(final Throwable cause) {
        super(cause);
    }
}
