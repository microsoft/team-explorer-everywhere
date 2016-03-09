// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

/**
 * Class for an error when a field definition does not exist.
 *
 * @since TEE-SDK-10.1
 */
public class FieldDefinitionNotExistException extends WorkItemException {
    private static final long serialVersionUID = 8870643126863768746L;

    public FieldDefinitionNotExistException(final String message) {
        super(message, 0);
    }
}
