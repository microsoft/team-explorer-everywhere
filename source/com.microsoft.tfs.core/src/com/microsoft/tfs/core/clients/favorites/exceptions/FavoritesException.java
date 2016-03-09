// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.favorites.exceptions;

import com.microsoft.tfs.core.exceptions.TECoreException;

public class FavoritesException extends TECoreException {
    public FavoritesException() {
        super();
    }

    public FavoritesException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FavoritesException(final String message) {
        super(message);
    }

    public FavoritesException(final Throwable cause) {
        super(cause);
    }
}
