// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.config;

import java.net.URI;

import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsCompleteDialog;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;
import com.microsoft.tfs.util.Check;

public class UITransportFederatedFallbackAuthRunnable extends UITransportAuthRunnable {
    private final URI serverURI;
    private final Credentials credentials;
    private final FederatedAuthException exception;

    public UITransportFederatedFallbackAuthRunnable(
        final URI serverURI,
        final Credentials credentials,
        final FederatedAuthException exception) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$
        Check.notNull(exception, "exception"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.credentials = credentials;
        this.exception = exception;
    }

    @Override
    protected CredentialsCompleteDialog getCredentialsDialog() {
        final UITransportFederatedAuthRunnable subRunnable = new UITransportFederatedAuthRunnable(serverURI, exception);

        if (subRunnable.isAvailable()) {
            return subRunnable.getCredentialsDialog();
        }

        final UnauthorizedException unauthorizedException =
            new UnauthorizedException(exception.getServerURI(), credentials);

        return new UITransportUsernamePasswordAuthRunnable(
            serverURI,
            credentials,
            unauthorizedException).getCredentialsDialog();
    }
}
