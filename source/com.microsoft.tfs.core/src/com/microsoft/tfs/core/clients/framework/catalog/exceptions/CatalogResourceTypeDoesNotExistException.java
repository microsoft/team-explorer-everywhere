// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.util.GUID;

/**
 * Exception raised when a catalog service resource type is referenced which
 * does not exist.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogResourceTypeDoesNotExistException extends CatalogException {
    public CatalogResourceTypeDoesNotExistException(final GUID resourceTypeIdentifier) {
        super(MessageFormat.format("Catalog resource '{0}' does not exist.", resourceTypeIdentifier.toString())); //$NON-NLS-1$
    }
}
