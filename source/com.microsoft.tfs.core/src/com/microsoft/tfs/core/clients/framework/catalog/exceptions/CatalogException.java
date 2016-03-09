// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog.exceptions;

import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Base class for all catalog service exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogException extends TECoreException {
    public CatalogException() {
        super();
    }

    public CatalogException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CatalogException(final String message) {
        super(message);
    }

    public CatalogException(final Throwable cause) {
        super(cause);
    }
}
