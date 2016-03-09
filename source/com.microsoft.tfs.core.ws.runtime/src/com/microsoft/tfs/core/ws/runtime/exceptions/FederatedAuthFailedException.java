// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.ws.runtime.Messages;

@SuppressWarnings("serial")
public class FederatedAuthFailedException extends TransportException {
    final String serverURL;

    public FederatedAuthFailedException(final String serverError, final String serverURL) {
        super(serverError);
        this.serverURL = serverURL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        final StringBuilder sb = new StringBuilder();
        final String clientErrorMessageWrapperFormat =
            Messages.getString("FederatedAuthFailedException.ClientErrorMessageFormat"); //$NON-NLS-1$

        sb.append(MessageFormat.format(clientErrorMessageWrapperFormat, serverURL));
        sb.append("\r\n\r\n"); //$NON-NLS-1$
        sb.append(super.getMessage());

        return sb.toString();
    }

}
