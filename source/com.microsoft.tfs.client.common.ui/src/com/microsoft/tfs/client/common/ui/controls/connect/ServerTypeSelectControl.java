// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.dialogs.connect.ServerListDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListManagerFactory;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class ServerTypeSelectControl extends BaseControl {
    private static final Log log = LogFactory.getLog(ServerTypeSelectControl.class);

    private URI serverURI = null;
    private boolean ignoreServerChangeEvents = false;
    private ICommandExecutor commandExecutor;
    private ICommandExecutor noErrorDialogCommandExecutor;
    private Combo serverCombo;
    private ContentProposalAdapter serverComboAdapter;
    private PropsalProvider proposalProvider;
    private final Button vstsButton;
    private final Button tfsButton;
    private final Composite serverControl;
    private final SingleListenerFacade listeners = new SingleListenerFacade(ServerTypeSelectionChangedListener.class);

    public ServerTypeSelectControl(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = 0;
        setLayout(layout);

        final SelectionAdapter buttonsChanged = new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                onButtonsChanged();
            }
        };

        vstsButton = createTeamServicesButton(layout, buttonsChanged);
        tfsButton = createTeamFoundationServerButton(layout, buttonsChanged);
        serverControl = setupServerCombo(layout);

        populateServersCombo();

        if (serverURI != null) {
            setServerOnCombo(serverURI);
        } else {
            setServer(getServerFromCombo());
        }

        // fire the buttons changed event so that the initial state is updated
        onButtonsChanged();
    }

    private Button createTeamFoundationServerButton(final GridLayout layout, final SelectionAdapter buttonsChanged) {
        final Button button = new Button(this, SWT.RADIO);
        button.setText(Messages.getString("ServerTypeSelectControl.ConnectButtonText")); //$NON-NLS-1$
        button.addSelectionListener(buttonsChanged);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().vIndent(getVerticalSpacing() * 2).applyTo(button);

        return button;
    }

    private Button createTeamServicesButton(final GridLayout layout, final SelectionAdapter buttonsChanged) {
        final Button button = new Button(this, SWT.RADIO);
        button.setText(Messages.getString("ServerTypeSelectControl.BrowseVstsButtonText")); //$NON-NLS-1$
        button.setSelection(true);
        button.addSelectionListener(buttonsChanged);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(button);

        final Label vstsLabel = SWTUtil.createLabel(
            this,
            SWT.WRAP,
            Messages.getString("ServerTypeSelectControl.BrowseVstsExplanatoryText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hIndent(getHorizontalSpacing() * 4).vIndent(
            getVerticalSpacing()).wHint(getMinimumMessageAreaWidth()).hFill().hGrab().applyTo(vstsLabel);

        final CompatibilityLinkControl createAccountLink = CompatibilityLinkFactory.createLink(this, SWT.NONE);
        createAccountLink.setSimpleText(Messages.getString("ServerTypeSelectControl.CreateAccountLinkText")); //$NON-NLS-1$
        createAccountLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                // Use external dialog since the process may take a while
                BrowserFacade.launchURL(
                    URIUtils.newURI("https://go.microsoft.com/fwlink/?LinkId=307137&wt.mc_id=o~msft~java~eclipse"), //$NON-NLS-1$
                    null,
                    null,
                    null,
                    LaunchMode.EXTERNAL);
            }
        });
        GridDataBuilder.newInstance().hFill().hGrab().hIndent(getHorizontalSpacing() * 4).wHint(
            getMinimumMessageAreaWidth()).applyTo(createAccountLink.getControl());

        final CompatibilityLinkControl learnMoreLink = CompatibilityLinkFactory.createLink(this, SWT.NONE);
        learnMoreLink.setSimpleText(Messages.getString("ServerTypeSelectControl.LearnMoreLinkText")); //$NON-NLS-1$
        learnMoreLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                try {
                    // Use external dialog since the process may take a while
                    BrowserFacade.launchURL(
                        new URI("http://java.visualstudio.com"), //$NON-NLS-1$
                        null,
                        null,
                        null,
                        LaunchMode.EXTERNAL);
                } catch (final URISyntaxException ex) {
                    log.error("Learn more link failed", ex); //$NON-NLS-1$
                }
            }
        });
        GridDataBuilder.newInstance().hFill().hGrab().hIndent(getHorizontalSpacing() * 4).wHint(
            getMinimumMessageAreaWidth()).applyTo(learnMoreLink.getControl());

        return button;
    }

    private Composite setupServerCombo(final GridLayout parentLayout) {
        final Composite container = new Composite(this, SWT.NULL);
        final GridLayout layout = SWTUtil.gridLayout(container, 2, false, 0, 0);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        serverCombo = new Combo(container, SWT.NONE);
        // setup Auto Complete for the combo box
        proposalProvider = new PropsalProvider();
        final ComboContentAdapter comboContentAdapter = new ComboContentAdapter();
        serverComboAdapter = new ContentProposalAdapter(serverCombo, comboContentAdapter, proposalProvider, null, null);
        serverComboAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(serverCombo);
        serverCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                if (!ignoreServerChangeEvents) {
                    setServer(getServerFromCombo(), false);
                }
            }
        });

        final Button serversButton =
            SWTUtil.createButton(container, Messages.getString("ServerSelectControl.ServersButtonText")); //$NON-NLS-1$
        serversButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                onServersButtonSelected();
            }
        });

        GridDataBuilder.newInstance().hSpan(parentLayout).hIndent(getHorizontalSpacing() * 4).hFill().hGrab().vIndent(
            getVerticalSpacing()).applyTo(container);

        return container;
    }

    private void populateServersCombo() {
        final ServerList serverList =
            ServerListManagerFactory.getServerListProvider(DefaultPersistenceStoreProvider.INSTANCE).getServerList();

        int i = 0;
        final Set<ServerListConfigurationEntry> servers = serverList.getServers();
        final String[] list = new String[servers.size()];
        for (final ServerListConfigurationEntry entry : servers) {
            list[i++] = entry.getURI().toString();
        }
        Arrays.sort(list);
        serverCombo.setItems(list);
        proposalProvider.setProposals(list);

        if (list.length == 1) {
            serverCombo.select(0);
        }
    }

    protected void onServersButtonSelected() {
        final URI lastSelectedServer = getServerFromCombo();
        final ServerList serverList =
            ServerListManagerFactory.getServerListProvider(DefaultPersistenceStoreProvider.INSTANCE).getServerList();

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

        populateServersCombo();

        final ServerListConfigurationEntry lastAddedServerListEntry = dialog.getLastAddedServerListEntry();

        if (lastAddedServerListEntry != null) {
            setServerOnCombo(lastAddedServerListEntry.getURI());
        } else {
            setServerOnCombo(lastSelectedServer);
        }
    }

    private void setServerOnCombo(final URI server) {
        if (server != null) {
            serverCombo.setText(server.toString());
        } else {
            serverCombo.setText(""); //$NON-NLS-1$
        }
    }

    private URI getServerFromCombo() {
        final String server = serverCombo.getText();
        if (server != null && server.length() > 0) {
            return URI.create(server.trim());
        }

        return null;
    }

    public void addListener(final ServerTypeSelectionChangedListener listener) {
        listeners.addListener(listener);
    }

    public void removeListener(final ServerTypeSelectionChangedListener listener) {
        listeners.removeListener(listener);
    }

    public void setCommandExecutor(final ICommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public ICommandExecutor getCommandExecutor() {
        if (commandExecutor != null) {
            return commandExecutor;
        }

        return UICommandExecutorFactory.newUICommandExecutor(getShell());
    }

    public void setNoErrorDialogCommandExecutor(final ICommandExecutor noErrorDialogCommandExecutor) {
        this.noErrorDialogCommandExecutor = noErrorDialogCommandExecutor;
    }

    public ICommandExecutor getNoErrorDialogCommandExecutor() {
        if (noErrorDialogCommandExecutor != null) {
            return noErrorDialogCommandExecutor;
        }

        final ICommandExecutor noErrorDialogCommandExecutor = UICommandExecutorFactory.newUICommandExecutor(getShell());
        noErrorDialogCommandExecutor.setCommandFinishedCallback(
            UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        return noErrorDialogCommandExecutor;
    }

    public URI getServer() {
        if (this.serverURI == null) {
            return URIUtils.VSTS_ROOT_URL;
        }
        return this.serverURI;
    }

    public void setServer(final URI serverURI) {
        setServer(serverURI, true);
    }

    private void setServer(URI serverURI, final boolean setCombo) {
        ignoreServerChangeEvents = true;
        if (serverURI != null && URIUtils.VSTS_ROOT_URL.equals(serverURI)) {
            // setServer was called with the VSTS root url, so replace that with
            // null
            serverURI = null;
        }

        if (setCombo) {
            setServerOnCombo(serverURI);
        }

        // If we have an authority and it matches the one selected in the combo,
        // change the radio button
        if (serverURI != null && ServerURIUtils.equals(serverURI, getServerFromCombo())) {
            setVstsSelection(false);
        }
        ignoreServerChangeEvents = false;

        setServerInternal(serverURI);

        // If VSTS is still selected then reset the URL back to VSTS
        if (isVstsSelected()) {
            setServerInternal(URIUtils.VSTS_ROOT_URL);
        }
    }

    private void onButtonsChanged() {
        if (vstsButton.getSelection()) {
            setServer(null);
        } else {
            setServer(getServerFromCombo());
        }
    }

    private void setServerInternal(final URI serverURI) {
        if (this.serverURI != null && this.serverURI.equals(serverURI)) {
            return;
        }

        this.serverURI = serverURI;

        ((ServerTypeSelectionChangedListener) listeners.getListener()).onServerTypeSelectionChanged(
            new ServerTypeSelectionChangedEvent(this.serverURI));
    }

    public boolean isVstsSelected() {
        return vstsButton.getSelection();
    }

    public void setVstsSelection(final boolean selection) {
        vstsButton.setSelection(selection);
        tfsButton.setSelection(!selection);
    }

    public interface ServerTypeSelectionChangedListener {
        public void onServerTypeSelectionChanged(ServerTypeSelectionChangedEvent event);
    }

    public final class ServerTypeSelectionChangedEvent {
        private final URI serverURI;

        private ServerTypeSelectionChangedEvent(final URI serverURI) {
            this.serverURI = serverURI;
        }

        public URI getServerURI() {
            return serverURI;
        }
    }

    private class PropsalProvider implements IContentProposalProvider {
        private String[] proposals = new String[0];

        public void setProposals(final String[] proposals) {
            if (proposals == null) {
                this.proposals = new String[0];
            } else {
                this.proposals = proposals;
            }
        }

        @Override
        public IContentProposal[] getProposals(final String contents, final int position) {
            // Work-around to make sure that the popup doesn't open when the
            // user has opened the dropdown of the combo. By returning an empty
            // list, the popup won't be shown.
            if (ignoreServerChangeEvents || serverCombo.getListVisible()) {
                return new IContentProposal[0];
            }

            final List<IContentProposal> list = new ArrayList<IContentProposal>(proposals.length);
            if (contents == null || contents.length() == 0) {
                for (int i = 0; i < proposals.length; i++) {
                    list.add(new ContentProposal(proposals[i]));
                }
            } else {
                final String search = contents.toLowerCase().trim();
                for (int i = 0; i < proposals.length; i++) {
                    if (proposals[i] != null && proposals[i].toLowerCase().contains(search)) {
                        list.add(new ContentProposal(proposals[i]));
                    }
                }
            }

            return (IContentProposal[]) list.toArray(new IContentProposal[list.size()]);
        }
    }
}
