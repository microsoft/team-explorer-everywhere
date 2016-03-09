// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import com.microsoft.tfs.core.exceptions.InputValidationException;

/**
 * Exception thrown when a workspace spec string cannot be parsed into a
 * {@link WorkspaceSpec}.
 *
 * @since TEE-SDK-10.1
 */
public final class WorkspaceSpecParseException extends InputValidationException {
    public WorkspaceSpecParseException() {
        super();
    }

    public WorkspaceSpecParseException(final String message) {
        super(message);
    }

    public WorkspaceSpecParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public WorkspaceSpecParseException(final Throwable cause) {
        super(cause);
    }
}
