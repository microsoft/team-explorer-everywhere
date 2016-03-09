// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import com.microsoft.tfs.core.exceptions.InputValidationException;

/**
 * Exception thrown when an item spec string cannot be parsed into an
 * {@link ItemSpec}.
 *
 * @since TEE-SDK-10.1
 */
public final class ItemSpecParseException extends InputValidationException {
    public ItemSpecParseException() {
        super();
    }

    public ItemSpecParseException(final String message) {
        super(message);
    }

    public ItemSpecParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ItemSpecParseException(final Throwable cause) {
        super(cause);
    }
}
