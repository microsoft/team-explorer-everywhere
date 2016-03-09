// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.exceptions;

import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Base class for all location service exceptions.
 */
public class LocationException extends TECoreException {
    public LocationException() {
        super();
    }

    public LocationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public LocationException(final String message) {
        super(message);
    }

    public LocationException(final Throwable cause) {
        super(cause);
    }
}
