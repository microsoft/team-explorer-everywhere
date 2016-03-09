// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;

/**
 * <p>
 * An Exception thrown to indicate invalid query text (WIQL) was passed to a
 * method that required valid query text.
 * </p>
 * <p>
 * This exception is public (part of the work item OM).
 * </p>
 * <p>
 * The message of this exception will be a detailed error message indicating
 * what was wrong with the query text. The untouched invalid query text is
 * available by calling {@link #getInvalidQueryText()}.
 * </p>
 * <p>
 * Clients should not expect there to be a "cause" exception available from an
 * instance of {@link InvalidQueryTextException}. There may be such a cause, and
 * such a cause if it exists should be logged in any error logs, but the OM does
 * not make any guarantees about the existence of type of such a cause.
 * </p>
 *
 * @since TEE-SDK-10.1
 */
public class InvalidQueryTextException extends WorkItemException {
    private static final long serialVersionUID = -1955341983994066806L;

    private final String invalidQueryText;

    public InvalidQueryTextException(final String message, final String invalidQueryText, final Throwable cause) {
        super(message, cause);
        this.invalidQueryText = invalidQueryText;
    }

    public String getInvalidQueryText() {
        return invalidQueryText;
    }
}
