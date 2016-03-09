// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;

public class LookupFailedException extends WorkItemException {
    public LookupFailedException(final String message) {
        super(message);
    }
}
