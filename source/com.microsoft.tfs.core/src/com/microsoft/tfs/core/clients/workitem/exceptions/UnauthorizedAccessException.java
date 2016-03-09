// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

/**
 * Exception thrown when the server denied access to a work item.
 *
 * @since TEE-SDK-10.1
 */
public class UnauthorizedAccessException extends WorkItemException {
    /*
     * Note: in .NET, UnauthorizedAccessException is part of the standard
     * library. For now, in our OM, we'll make it a subclass of ClientException.
     * Eventually we may want to re-use it outside of the WIT OM.
     */

    private static final long serialVersionUID = -6781905507269824434L;

    public UnauthorizedAccessException(final String message, final Throwable cause, final int errorId) {
        super(message, cause, errorId);
    }
}
