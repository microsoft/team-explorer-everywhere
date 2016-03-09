// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthFailedException;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFault;

/**
 * A {@link TFSAccessException} indicates that the server accepted the
 * credentials used (and thus differs from a {@link TFSUnauthorizedException})
 * but that the credentials used do not have permission to connect to the
 * server.
 *
 * @since TEE-SDK-10.1
 */
public class TFSAccessException extends TECoreException {
    public TFSAccessException(final SOAPFault e) {
        super(buildMessage(e), e);
    }

    public TFSAccessException(final FederatedAuthFailedException e) {
        super(buildMessage(e));
    }

    private static String buildMessage(final SOAPFault e) {
        if (e != null) {
            return (e).getLocalizedMessage();
        }

        return Messages.getString("TFSAccessException.PermissionDeniedMessage"); //$NON-NLS-1$
    }

    private static String buildMessage(final FederatedAuthFailedException e) {
        return Messages.getString("TFSAccessException.PermissionDeniedMessage") //$NON-NLS-1$
            + "\r\n\r\n" //$NON-NLS-1$
            + e.getMessage();
    }
}
