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

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.connect.AddServerControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.tasks.ConnectToConfigurationServerTask;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListEntryType;

/**
 *
 *
 * @threadsafety unknown
 */
public class AddServerDialog extends BaseDialog {
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