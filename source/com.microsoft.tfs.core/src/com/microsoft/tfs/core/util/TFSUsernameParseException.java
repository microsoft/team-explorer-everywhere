// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Thrown by {@link TFSUser} when the strings fail to parse correctly during
 * construction.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class TFSUsernameParseException extends TECoreException {
    public TFSUsernameParseException() {
        super();
    }

    public TFSUsernameParseException(final String message) {
        super(message);
    }

    public TFSUsernameParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TFSUsernameParseException(final Throwable cause) {
        super(cause);
    }
}
