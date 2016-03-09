// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

import com.microsoft.tfs.core.Messages;

/**
 * Class for a duplicate batch read parameter error.
 *
 * @since TEE-SDK-10.1
 */
public class DuplicateBatchReadParameterException extends WorkItemException {
    private static final long serialVersionUID = 4565531657581054294L;

    public DuplicateBatchReadParameterException() {
        super(Messages.getString("DuplicateBatchReadParameterException.ItemAlreadyExists")); //$NON-NLS-1$
    }
}
