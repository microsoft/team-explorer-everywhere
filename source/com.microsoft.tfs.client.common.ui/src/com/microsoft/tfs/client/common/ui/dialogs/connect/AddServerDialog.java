// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.connect;

import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.connect.AddServerControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.helpers.CredentialsHelper;
import com.microsoft.tfs.client.common.ui.tasks.ConnectToConfigurationServerTask;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListEntryType;

/**
 *
 *
 * @threadsafety unknown
 */
public class AddServerDialog extends BaseDialog {

    private static final int AUTH_BUTTON_ID = 3;
    private static final int CLEAR_BUTTON_ID = 4;

    private AddServerControl addServerControl;

    private ServerList serverList;

    private TFSConnection connection;
    private URI serverURI;

    public AddServerDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = getHorizontalMargin();
        gridLayout.marginHeight = getVerticalMargin();
        gridLayout.horizontalSpacing = getHorizontalSpacing();
        gridLayout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(gridLayout);

        addServerControl = new AddServerControl(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().grab().fill().applyTo(addServerControl);

        this.addButtonDescription(AUTH_BUTTON_ID, Messages.getString("AddServerDialog.AuthButtonText"), false); //$NON-NLS-1$
        this.addButtonDescription(CLEAR_BUTTON_ID, Messages.getString("AddServerDialog.ClearButtonText"), false); //$NON-NLS-1$

        setOptionResizableDirections(SWT.HORIZONTAL);
    }

    public void setServerList(final ServerList serverList) {
        this.serverList = serverList;
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
        final Button okButton = getButton(IDialogConstants.OK_ID);
        new ButtonValidatorBinding(okButton).bind(addServerControl);

        final Button authButton = getButton(AUTH_BUTTON_ID);
        new ButtonValidatorBinding(authButton).bind(addServerControl);

        final Button clearButton = getButton(CLEAR_BUTTON_ID);
        new ButtonValidatorBinding(clearButton).bind(addServerControl);
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (buttonId == AUTH_BUTTON_ID) {
            onAuthButtonSelected();
        } else if (buttonId == CLEAR_BUTTON_ID) {
            onClearButtonSelected();
        }
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("AddServerDialog.AddServerDialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void okPressed() {
        try {
            getButton(IDialogConstants.OK_ID).setEnabled(false);

            final ConnectToConfigurationServerTask connectTask =
                new ConnectToConfigurationServerTask(getShell(), addServerControl.getServerURI());

            final IStatus status = connectTask.run();

            if (!status.isOK()) {
                return;
            }

            connection = connectTask.getConnection();
            serverURI = connection.getBaseURI();

            if (serverList != null && serverList.contains(serverURI)) {
                MessageDialog.openError(
                    getShell(),
                    Messages.getString("AddServerDialog.ServerExistsErrorTitle"), //$NON-NLS-1$
                    MessageFormat.format(
                        Messages.getString("AddServerDialog.ServerExistsErrorFormat"), //$NON-NLS-1$
                        serverURI.getHost()));

                return;
            }
        } finally {
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        }

        super.okPressed();
    }

    private void onAuthButtonSelected() {
        final CredentialsManager credentialsManager = EclipseCredentialsManagerFactory.getCredentialsManager();
        final URI serverUrl = addServerControl.getServerURI();
        final CachedCredentials oldCachedCredentials = credentialsManager.getCredentials(serverUrl);

        final CredentialsDialog credentialsDialog = new CredentialsDialog(getShell(), serverUrl);

        if (oldCachedCredentials != null) {
            final Credentials savedCredentials = oldCachedCredentials.toCredentials();
            if (savedCredentials == null || !(savedCredentials instanceof CookieCredentials)) {
                credentialsDialog.setCredentials(savedCredentials);
            }
        }

        if (credentialsDialog.open() == IDialogConstants.OK_ID) {
            final Credentials credentials = credentialsDialog.getCredentials();

            final CachedCredentials newCachedCredentials = new CachedCredentials(serverUrl, credentials);
            credentialsManager.setCredentials(newCachedCredentials);
        }
    }

    private void onClearButtonSelected() {

        final URI serverUrl = addServerControl.getServerURI();
        final String title;
        final String message;

        title = Messages.getString(Messages.getString("AddServerDialog.ClearCredentialsTitleText")); //$NON-NLS-1$
        message = MessageFormat.format(
            Messages.getString(Messages.getString("AddServerDialog.ClearCredentialsMessageFormat")), //$NON-NLS-1$
            serverUrl);

        if (!MessageDialog.openQuestion(getShell(), title, message)) {
            return;
        }

        final CredentialsManager credentialsManager = EclipseCredentialsManagerFactory.getCredentialsManager();

        credentialsManager.removeCredentials(serverUrl);

        if (ServerURIUtils.isHosted(serverUrl)) {
            /*
             * Removing the token pair from the internal store. Next time we
             * connect to a VSTS account that does not have saved PAT, the user
             * will be prompted for credentials.
             */
            CredentialsHelper.removeOAuth2Token(true);
        }
    }

    public TFSConnection getConnection() {
        return connection;
    }

    public URI getServerURI() {
        return serverURI;
    }

    public ServerListConfigurationEntry getServerListEntry() {
        final ServerListEntryType type = (connection instanceof TFSConfigurationServer)
            ? ServerListEntryType.CONFIGURATION_SERVER : ServerListEntryType.TEAM_PROJECT_COLLECTION;

        return new ServerListConfigurationEntry(serverURI.getHost(), type, serverURI);
    }
}