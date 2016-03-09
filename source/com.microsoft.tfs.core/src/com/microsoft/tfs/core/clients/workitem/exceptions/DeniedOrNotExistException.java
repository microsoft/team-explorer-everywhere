// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

import com.microsoft.tfs.core.Messages;

/**
 * Class to describe access denied or item does not exist exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class DeniedOrNotExistException extends WorkItemException {
    private static final long serialVersionUID = -1030777834581783157L;

    public DeniedOrNotExistException() {
        /*
         * I18N
         */
        this(Messages.getString("DeniedOrNotExistException.DoesNotExistOrAcessDenied")); //$NON-NLS-1$
    }

    public DeniedOrNotExistException(final String message) {
        super(message, 0x927df);
    }
}
