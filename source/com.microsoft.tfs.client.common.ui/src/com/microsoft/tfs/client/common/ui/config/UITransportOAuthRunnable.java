// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.config;

import java.net.URI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.credentials.CredentialsHelper;
import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsCompleteDialog;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsCompleteListener;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class UITransportOAuthRunnable extends UITransportAuthRunnable {
    private final URI serverURI;

    public UITransportOAuthRunnable(final URI serverURI) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        this.serverURI = serverURI;
    }

    @Override
    protected CredentialsCompleteDialog getCredentialsDialog() {
        final Shell shell = ShellUtils.getBestParent(ShellUtils.getWorkbenchShell());

        final CredentialsManager credentialsManager =
            EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

        final OAuthCredentialsDialog credentialsDialog = new OAuthCredentialsDialog(shell, serverURI);

        credentialsDialog.addCredentialsCompleteListener(new CredentialsCompleteListener() {
            @Override
            public void credentialsComplete() {
                if (credentialsDialog.getReturnCode() == IDialogConstants.OK_ID) {
                    credentialsManager.setCredentials(
                        new CachedCredentials(serverURI, credentialsDialog.getCredentials()));
                }
            }
        });

        return credentialsDialog;
    }

    private class OAuthCredentialsDialog extends CredentialsCompleteDialog {

        private final SingleListenerFacade credentialsCompleteListeners =
            new SingleListenerFacade(CredentialsCompleteListener.class);

        Credentials credentials;
        final Shell shell;

        public OAuthCredentialsDialog(final Shell parentShell, final URI serverURI) {
            super(parentShell);
            this.shell = parentShell;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int open() {
            credentials = CredentialsHelper.getOAuthCredentials(serverURI);

            if (credentials != null) {
                setReturnCode(IDialogConstants.OK_ID);
                ((CredentialsCompleteListener) credentialsCompleteListeners.getListener()).credentialsComplete();
                return IDialogConstants.OK_ID;
            }

            return IDialogConstants.CANCEL_ID;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Shell getShell() {
            return shell;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addCredentialsCompleteListener(CredentialsCompleteListener listener) {
            credentialsCompleteListeners.addListener(listener);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Credentials getCredentials() {
            return credentials;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String provideDialogTitle() {
            return null;
        }
    }
}
