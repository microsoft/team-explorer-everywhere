// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;

public class RowSetParseException extends WorkItemException {
    private static final long serialVersionUID = -2121060304912328663L;

    public RowSetParseException(final String message) {
        super(message);
    }
}
