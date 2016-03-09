// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog.exceptions;

/**
 * Exception raised when a catalog service node is referenced which does not
 * exist.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogNodeDoesNotExistException extends CatalogException {
    public CatalogNodeDoesNotExistException() {
        super("Catalog node does not exist."); //$NON-NLS-1$
    }
}
