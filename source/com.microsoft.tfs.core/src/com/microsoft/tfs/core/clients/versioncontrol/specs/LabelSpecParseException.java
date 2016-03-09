// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import com.microsoft.tfs.core.exceptions.InputValidationException;

/**
 * Exception thrown when a label spec string cannot be parsed into a
 * {@link LabelSpec}.
 *
 * @since TEE-SDK-10.1
 */
public final class LabelSpecParseException extends InputValidationException {
    public LabelSpecParseException() {
        super();
    }

    public LabelSpecParseException(final String message) {
        super(message);
    }

    public LabelSpecParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public LabelSpecParseException(final Throwable cause) {
        super(cause);
    }
}
