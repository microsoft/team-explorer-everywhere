// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;

/**
 * Exception thrown when a string cannot be parsed into a {@link WIQDocument}.
 *
 * @since TEE-SDK-10.1
 */
public class WIQDocumentParseException extends WorkItemException {
    private static final long serialVersionUID = -4849711943358745321L;

    public WIQDocumentParseException(final Throwable cause) {
        super(cause);
    }

    public WIQDocumentParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public WIQDocumentParseException(final String message) {
        super(message);
    }

}
