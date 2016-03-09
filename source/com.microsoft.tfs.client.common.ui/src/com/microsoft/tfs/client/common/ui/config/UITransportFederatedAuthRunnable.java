// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.config;

import java.net.URI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.connect.ACSCredentialsDialog;
import com.microsoft.tfs.client.common.ui.dialogs.connect.ACSCredentialsDialog.VSTSCredentialsDialog;
import com.microsoft.tfs.client.common.ui.dialogs.connect.ACSCredentialsDialogD11;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsCompleteDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.util.Check;

public class UITransportFederatedAuthRunnable extends UITransportAuthRunnable {
    private final URI serverURI;
    private final FederatedAuthException exception;

    public UITransportFederatedAuthRunnable(final URI serverURI, final FederatedAuthException exception) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(exception, "exception"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.exception = exception;
    }

    public UITransportFederatedAuthRunnable() {
        // A special case for connecting to the entire VSTS
        this.serverURI = null;
        this.exception = null;
    }

    public boolean isAvailable() {
        return ACSCredentialsDialog.isAvailable();
    }

    @Override
    protected CredentialsCompleteDialog getCredentialsDialog() {
        final Shell shell = ShellUtils.getBestParent(ShellUtils.getWorkbenchShell());

        if (!ACSCredentialsDialog.isAvailable() || (exception != null && exception.getAuthenticationURL() == null)) {
            MessageDialog.openError(
                shell,
                Messages.getString("UITransportAuthHandler.FederatedAuthenticationNotSupportedTitle"), //$NON-NLS-1$
                Messages.getString("UITransportAuthHandler.FederatedAuthenticationNotSupportedMessage")); //$NON-NLS-1$
            return null;
        }

        if (serverURI == null) {
            return new VSTSCredentialsDialog(shell);
        } else if (EnvironmentVariables.getBoolean(EnvironmentVariables.USE_LEGACY_MSA, false)) {
            return new ACSCredentialsDialogD11(
                shell,
                serverURI,
                URIUtils.newURI(exception.getAuthenticationURL()),
                exception);
        } else {
            return new ACSCredentialsDialog(
                shell,
                serverURI,
                URIUtils.newURI(exception.getAuthenticationURL()),
                exception);
        }
    }
}
