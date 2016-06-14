// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.config;

import java.net.URI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsCompleteDialog;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsCompleteListener;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.credentials.CredentialsManagerFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;
import com.microsoft.tfs.util.Check;

public class UITransportUsernamePasswordAuthRunnable extends UITransportAuthRunnable {
    private final URI serverURI;
    private final Credentials credentials;
    private final UnauthorizedException exception;

    public UITransportUsernamePasswordAuthRunnable(final URI serverURI, final Credentials credentials) {
        this(serverURI, credentials, null);
    }

    public UITransportUsernamePasswordAuthRunnable(
        final URI serverURI,
        final Credentials credentials,
        final UnauthorizedException exception) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.credentials = credentials;
        this.exception = exception;
    }

    @Override
    protected CredentialsCompleteDialog getCredentialsDialog() {
        final Shell shell = ShellUtils.getBestParent(ShellUtils.getWorkbenchShell());

        final CredentialsManager credentialsManager =
            CredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

        final CredentialsDialog credentialsDialog = new CredentialsDialog(shell, serverURI);
        credentialsDialog.setCredentials(credentials);

        if (exception != null) {
            credentialsDialog.setErrorMessage(exception.getLocalizedMessage());
        }

        if (credentialsManager.canWrite()) {
            credentialsDialog.setAllowSavePassword(true);

            credentialsDialog.addCredentialsCompleteListener(new CredentialsCompleteListener() {
                @Override
                public void credentialsComplete() {
                    if (credentialsDialog.getReturnCode() == IDialogConstants.OK_ID
                        && credentialsDialog.isSavePasswordChecked()) {
                        credentialsManager.setCredentials(
                            new CachedCredentials(serverURI, credentialsDialog.getCredentials()));
                    }
                }
            });
        }

        return credentialsDialog;
    }
}
