// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.dialogs.connect.AddServerDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.CredentialsHelper;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class ServerListControl extends BaseControl {
    public static final String SERVERS_TABLE_ID = "ServerListControl.profilesTable"; //$NON-NLS-1$
    public static final String ADD_BUTTON_ID = "ServerListControl.addButton"; //$NON-NLS-1$
    public static final String DELETE_BUTTON_ID = "ServerListControl.deleteButton"; //$NON-NLS-1$
    public static final String CLEAR_BUTTON_ID = "ServerListControl.clearButton"; //$NON-NLS-1$

    private final ServerListTable serverListTable;

    private ServerList serverList;

    private ServerListConfigurationEntry lastAddedServerListEntry;
    private TFSConnection lastAddedConnection;

    private final SingleListenerFacade doubleClickListeners = new SingleListenerFacade(IDoubleClickListener.class);

    public ServerListControl(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        serverListTable = new ServerListTable(this, SWT.FULL_SELECTION | SWT.MULTI);
        AutomationIDHelper.setWidgetID(serverListTable.getTable(), SERVERS_TABLE_ID);
        GridDataBuilder.newInstance().grab().fill().vSpan(5).applyTo(serverListTable);

        serverListTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                ServerListControl.this.onTableDoubleClick(event);
            }
        });

        final Button addButton = SWTUtil.createButton(this, Messages.getString("ServerListControl.AddButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(addButton, ADD_BUTTON_ID);
        GridDataBuilder.newInstance().hFill().wButtonHint(addButton).applyTo(addButton);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                ServerListControl.this.onAddButtonSelected(e);
            }
        });

        final Button deleteButton =
            SWTUtil.createButton(this, Messages.getString("ServerListControl.DeleteButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(deleteButton, DELETE_BUTTON_ID);
        GridDataBuilder.newInstance().hFill().wButtonHint(deleteButton).applyTo(deleteButton);
        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                ServerListControl.this.onDeleteButtonSelected(e);
            }
        });
        new ButtonValidatorBinding(deleteButton).bind(serverListTable.getSelectionValidator());

        final Button clearButton = SWTUtil.createButton(this, Messages.getString("ServerListControl.ClearButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(deleteButton, CLEAR_BUTTON_ID);
        GridDataBuilder.newInstance().hFill().wButtonHint(clearButton).applyTo(clearButton);
        clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                ServerListControl.this.onClearButtonSelected(e);
            }
        });
        new ButtonValidatorBinding(clearButton).bind(serverListTable.getSelectionValidator());

        serverListTable.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final ServerListConfigurationEntry[] selectedEntries = serverListTable.getSelectedServerListEntries();
                boolean currentServerSelected = false;
                for (final ServerListConfigurationEntry selectedEntry : selectedEntries) {
                    if (selectedEntry.getURI().equals(UIConnectionPersistence.getInstance().getLastUsedServerURI())) {
                        currentServerSelected = true;
                        break;
                    }
                }

                // Disable the delete button in case the currently used server
                // is selected
                deleteButton.setEnabled(!currentServerSelected);

            }
        });
    }

    public void setServerList(final ServerList serverList) {
        this.serverList = serverList;
        refreshTable();
    }

    public ServerListConfigurationEntry[] getSelectedServerListEntries() {
        return serverListTable.getSelectedServerListEntries();
    }

    public ServerListConfigurationEntry getLastAddedServerListEntry() {
        return lastAddedServerListEntry;
    }

    public TFSConnection getLastAddedConnection() {
        return lastAddedConnection;
    }

    private void refreshTable() {
        final ServerListConfigurationEntry[] serverListEntries =
            serverList.getServers().toArray(new ServerListConfigurationEntry[serverList.getServers().size()]);
        serverListTable.setServerListEntries(serverListEntries);
    }

    private void onAddButtonSelected(final SelectionEvent e) {
        final AddServerDialog dialog = new AddServerDialog(getShell());
        dialog.setServerList(serverList);

        if (dialog.open() == IDialogConstants.OK_ID) {
            lastAddedServerListEntry = dialog.getServerListEntry();
            lastAddedConnection = dialog.getConnection();

            serverList.add(lastAddedServerListEntry);
            refreshTable();
            serverListTable.setFocus();
        }
    }

    private void onDeleteButtonSelected(final SelectionEvent e) {
        final ServerListConfigurationEntry[] serverListEntries = serverListTable.getSelectedServerListEntries();

        final String title;
        final String message;

        if (serverListEntries.length == 1) {
            title = Messages.getString("ServerListControl.RemoveServerTitle"); //$NON-NLS-1$
            message = MessageFormat.format(
                Messages.getString("ServerListControl.RemoveServerPromptFormat"), //$NON-NLS-1$
                serverListEntries[0].getName());
        } else {
            title = Messages.getString("ServerListControl.RemoveServersTitle"); //$NON-NLS-1$
            message = Messages.getString("ServerListControl.RemoveServersPrompt"); //$NON-NLS-1$
        }

        if (MessageDialog.openQuestion(getShell(), title, message) == false) {
            return;
        }

        removeCredentials(serverListEntries);

        for (int i = 0; i < serverListEntries.length; i++) {
            final ServerListConfigurationEntry serverListEntry = serverListEntries[i];

            serverList.remove(serverListEntry);

            if (lastAddedServerListEntry == serverListEntry) {
                lastAddedServerListEntry = null;
                lastAddedConnection = null;
            }
        }

        refreshTable();
        serverListTable.setFocus();
    }

    private void onClearButtonSelected(final SelectionEvent e) {
        final ServerListConfigurationEntry[] serverListEntries = serverListTable.getSelectedServerListEntries();

        final String title;
        final String message;

        if (serverListEntries.length == 1) {
            title = Messages.getString("ServerListControl.ClearServersTitle"); //$NON-NLS-1$
            message = MessageFormat.format(
                Messages.getString("ServerListControl.ClearServerPromptFormat"), //$NON-NLS-1$
                serverListEntries[0].getName());
        } else {
            title = Messages.getString("ServerListControl.ClearServersTitle"); //$NON-NLS-1$
            message = Messages.getString("ServerListControl.ClearServersPrompt"); //$NON-NLS-1$
        }

        if (!MessageDialog.openQuestion(getShell(), title, message)) {
            return;
        }

        removeCredentials(serverListEntries);

        refreshTable();
        serverListTable.setFocus();
    }

    private void removeCredentials(final ServerListConfigurationEntry[] serverListEntries) {

        final CredentialsManager teeCredentialsProvider =
            EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);
        final CredentialsManager gitCredentialsProvider = EclipseCredentialsManagerFactory.getGitCredentialsManager();

        boolean removeOAuth2Token = false;
        for (int i = 0; i < serverListEntries.length; i++) {
            final ServerListConfigurationEntry serverListEntry = serverListEntries[i];
            final URI url = serverListEntry.getURI();

            teeCredentialsProvider.removeCredentials(url);
            gitCredentialsProvider.removeCredentials(url);

            // Remove from OAuth2 access token from the internal store
            removeOAuth2Token = removeOAuth2Token || ServerURIUtils.isHosted(url);
        }

        if (removeOAuth2Token) {
            /*
             * Removing the token pair from the internal store. Next time we
             * connect to a VSTS account that does not have saved PAT, the user
             * will be prompted for credentials.
             */
            CredentialsHelper.removeOAuth2Token(true);
        }
    }

    private void onTableDoubleClick(final DoubleClickEvent event) {
        ((IDoubleClickListener) doubleClickListeners.getListener()).doubleClick(event);
    }

    public void addDoubleClickListener(final IDoubleClickListener listener) {
        doubleClickListeners.addListener(listener);
    }

    public void removeDoubleClickListener(final IDoubleClickListener listener) {
        doubleClickListeners.removeListener(listener);
    }
}
