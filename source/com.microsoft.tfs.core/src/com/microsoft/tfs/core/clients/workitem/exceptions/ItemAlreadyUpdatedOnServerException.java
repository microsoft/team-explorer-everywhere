// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

import com.microsoft.tfs.core.Messages;

/**
 * Class for an item already updated on server error.
 *
 * @since TEE-SDK-10.1
 */
public class ItemAlreadyUpdatedOnServerException extends WorkItemException {
    private static final long serialVersionUID = 6163038050360854659L;

    /*
     * I18N
     */
    private static String MESSAGE = Messages.getString("ItemAlreadyUpdatedOnServerException.WorkItemVersionConflict"); //$NON-NLS-1$

    public ItemAlreadyUpdatedOnServerException(final Throwable cause) {
        super(MESSAGE, cause);
    }
}
