// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs.version;

import com.microsoft.tfs.core.exceptions.InputValidationException;

/**
 * Exception thrown when a version spec string cannot be parsed into a known
 * {@link VersionSpec} object.
 *
 * @since TEE-SDK-10.1
 */
public final class VersionSpecParseException extends InputValidationException {
    public VersionSpecParseException() {
        super();
    }

    public VersionSpecParseException(final String message) {
        super(message);
    }

    public VersionSpecParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VersionSpecParseException(final Throwable cause) {
        super(cause);
    }

}
