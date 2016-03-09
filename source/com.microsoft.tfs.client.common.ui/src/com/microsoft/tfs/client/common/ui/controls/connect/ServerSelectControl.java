// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.URI;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.dialogs.connect.ServerListDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListManagerFactory;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class ServerSelectControl extends BaseControl {
    public static final String SERVERS_BUTTON_ID = "ServerSelectControl.serversButton"; //$NON-NLS-1$

    private final ServerListCombo serverListCombo;
    private final Button serversButton;

    private ServerList serverList;
    private URI preferredServerURI;

    private ServerListConfigurationEntry lastAddedServerListEntry;
    private TFSConnection lastAddedConnection;

    private final SingleListenerFacade serverSelectionChangedListeners =
        new SingleListenerFacade(ISelectionChangedListener.class);

    private boolean ignoreComboChangeEvents = false;
    private boolean internalComboChange = false;

    private boolean enabled = true;

    public ServerSelectControl(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = SWTUtil.gridLayout(this, 2, false, 0, 0);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        serverListCombo = new ServerListCombo(this, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(serverListCombo);
        serverListCombo.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                onServersComboSelectionChanged(event);
            }
        });

        serversButton = SWTUtil.createButton(this, Messages.getString("ServerSelectControl.ServersButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(serversButton, SERVERS_BUTTON_ID);
        serversButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                onServersButtonSelected(e);
            }
        });

        serverList =
            ServerListManagerFactory.getServerListProvider(DefaultPersistenceStoreProvider.INSTANCE).getServerList();
        populateServersCombo(true);
    }

    public void setServerURI(final URI serverURI) {
        preferredServerURI = serverURI;
        populateServersCombo(serverURI != null);
    }

    public URI getServerURI() {
        final ServerListConfigurationEntry selectedServerListEntry = serverListCombo.getSelectedServerListEntry();

        return selectedServerListEntry == null ? null : selectedServerListEntry.getURI();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        super.setEnabled(enabled);

        serverListCombo.setEnabled(enabled);
        serversButton.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean getEnabled() {
        return enabled;
    }

    protected void onServersButtonSelected(final SelectionEvent e) {
        final ServerListConfigurationEntry lastSelectedServerListEntry = serverListCombo.getSelectedServerListEntry();

        final ServerList serverListCopy = new ServerList();
        serverListCopy.addAll(serverList.getServers());

        final ServerListDialog dialog = new ServerListDialog(getShell(), serverListCopy);
        dialog.open();

        /*
         * Note: there is no OK/Cancel on ServerListDialog: all exit paths
         * should continue.
         */

        ServerListManagerFactory.getServerListProvider(DefaultPersistenceStoreProvider.INSTANCE).setServerList(
            serverListCopy);
        serverList = serverListCopy;

        populateServersCombo(false);

        lastAddedServerListEntry = dialog.getLastAddedServerListEntry();
        lastAddedConnection = dialog.getLastAddedConnection();

        if (lastAddedServerListEntry != null) {
            internalComboChange = true;

            try {
                serverListCombo.setSelectedServerListEntry(lastAddedServerListEntry);
            } finally {
                internalComboChange = false;
            }
        } else if (lastSelectedServerListEntry != null && serverList.contains(lastSelectedServerListEntry)) {
            ignoreComboChangeEvents = true;

            try {
                serverListCombo.setSelectedServerListEntry(lastSelectedServerListEntry);
            } finally {
                ignoreComboChangeEvents = false;
            }
        } else {
            serverListCombo.setSelectedServerListEntry(null);
        }
    }

    public TFSConnection getLastAddedConnection() {
        return lastAddedConnection;
    }

    public void addServerSelectionChangedListener(final ISelectionChangedListener listener) {
        serverSelectionChangedListeners.addListener(listener);
    }

    public void removeServerSectionChangedListener(final ISelectionChangedListener listener) {
        serverSelectionChangedListeners.removeListener(listener);
    }

    protected void onServersComboSelectionChanged(final SelectionChangedEvent event) {
        if (!internalComboChange) {
            lastAddedServerListEntry = null;
            lastAddedConnection = null;
        }

        if (!ignoreComboChangeEvents) {
            ((ISelectionChangedListener) serverSelectionChangedListeners.getListener()).selectionChanged(event);
        }
    }

    private void populateServersCombo(final boolean select) {
        serverListCombo.setServerList(serverList);

        if (serverList.getServers().size() > 0) {
            serverListCombo.setEnabled(enabled);
        } else {
            serverListCombo.setEnabled(false);
        }

        if (!select) {
            return;
        }

        URI selectedServerURI = null;

        if (preferredServerURI != null) {
            selectedServerURI = preferredServerURI;
        } else {
            selectedServerURI = UIConnectionPersistence.getInstance().getLastUsedServerURI();

            if (selectedServerURI != null || !serverList.contains(selectedServerURI)) {
                selectedServerURI = null;
            }
        }

        if (selectedServerURI != null) {
            serverListCombo.setSelectedServerURI(selectedServerURI);
            return;
        }

        final ServerListConfigurationEntry[] servers =
            serverList.getServers().toArray(new ServerListConfigurationEntry[serverList.getServers().size()]);

        if (servers.length == 1) {
            serverListCombo.setSelectedServerListEntry(servers[0]);
        }
    }
}
