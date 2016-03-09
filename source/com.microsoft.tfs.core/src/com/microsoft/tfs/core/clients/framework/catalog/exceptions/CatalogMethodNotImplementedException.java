// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog.exceptions;

import java.text.MessageFormat;

/**
 * Exception raised when an unimplemented catalog service method is called.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogMethodNotImplementedException extends CatalogException {
    public CatalogMethodNotImplementedException(final String methodName) {
        super(MessageFormat.format("Catalog service method '{0}' is not implemented.", methodName)); //$NON-NLS-1$
    }
}
