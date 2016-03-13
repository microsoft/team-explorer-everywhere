// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
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

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.dialogs.connect.ServerListDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListManagerFactory;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class ServerTypeSelectControl extends BaseControl {
    private static final Log log = LogFactory.getLog(ServerTypeSelectControl.class);

    private URI serverURI = null;
    private boolean ignoreServerChangeEvents = false;
    private Combo serverCombo;
    private ProposalProvider proposalProvider;
    private final Button vstsButton;
    private final Button tfsButton;
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
        setupServerCombo(layout);

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

    private void setupServerCombo(final GridLayout parentLayout) {
        // Set up a new container to avoid problems with column spacing
        final Composite container = new Composite(this, SWT.NULL);
        final GridLayout layout = SWTUtil.gridLayout(container, 2, false, 0, 0);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        // Add the combo box
        serverCombo = new Combo(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(serverCombo);
        serverCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                if (!ignoreServerChangeEvents) {
                    setVstsSelection(false);
                    setServerInternal(getServerFromCombo());
                }
            }
        });

        // Setup Auto Complete for the combo box
        proposalProvider = new ProposalProvider();
        final ComboContentAdapter comboContentAdapter = new ComboContentAdapter();
        final ContentProposalAdapter serverComboAdapter =
            new ContentProposalAdapter(serverCombo, comboContentAdapter, proposalProvider, null, null);
        serverComboAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

        // Add the servers button
        final Button serversButton =
            SWTUtil.createButton(container, Messages.getString("ServerSelectControl.ServersButtonText")); //$NON-NLS-1$
        serversButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                onServersButtonSelected();
            }
        });

        // Add the container to the parent layout
        GridDataBuilder.newInstance().hSpan(parentLayout).hIndent(getHorizontalSpacing() * 4).hFill().hGrab().vIndent(
            getVerticalSpacing()).applyTo(container);
    }

    private void populateServersCombo() {
        final ServerList serverList =
            ServerListManagerFactory.getServerListProvider(DefaultPersistenceStoreProvider.INSTANCE).getServerList();

        final Set<ServerListConfigurationEntry> servers = serverList.getServers();
        final List<String> list = new ArrayList<String>(servers.size());
        for (final ServerListConfigurationEntry entry : servers) {
            list.add(entry.getURI().toString());
        }
        Collections.sort(list, Collator.getInstance());
        serverCombo.setItems(list.toArray(new String[list.size()]));
        proposalProvider.setProposals(list);
    }

    private void onServersButtonSelected() {
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
        final ServerListConfigurationEntry[] selectedServers = dialog.getSelectedServerListEntries();

        if (lastAddedServerListEntry != null) {
            setServerOnCombo(lastAddedServerListEntry.getURI());
        } else if (selectedServers != null && selectedServers.length > 0) {
            setServerOnCombo(selectedServers[0].getURI());
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
        if (!StringUtil.isNullOrEmpty(server)) {
            return URIUtils.newURI(server.trim());
        }

        return null;
    }

    public void addListener(final ServerTypeSelectionChangedListener listener) {
        listeners.addListener(listener);
    }

    public void removeListener(final ServerTypeSelectionChangedListener listener) {
        listeners.removeListener(listener);
    }

    public URI getServer() {
        return this.serverURI;
    }

    public void setServer(final URI serverURI) {
        ignoreServerChangeEvents = true;
        if (serverURI == null || URIUtils.VSTS_ROOT_URL.equals(serverURI)) {
            setVstsSelection(true);
            setServerOnCombo(null);
            setServerInternal(URIUtils.VSTS_ROOT_URL);
        } else {
            setVstsSelection(false);
            setServerOnCombo(serverURI);
            setServerInternal(serverURI);
        }
        ignoreServerChangeEvents = false;
    }

    private void setServerInternal(final URI serverURI) {
        if (this.serverURI != null && this.serverURI.equals(serverURI)) {
            return;
        }

        this.serverURI = serverURI;

        ((ServerTypeSelectionChangedListener) listeners.getListener()).onServerTypeSelectionChanged(
            new ServerTypeSelectionChangedEvent(this.serverURI));
    }

    private void onButtonsChanged() {
        ignoreServerChangeEvents = true;
        if (vstsButton.getSelection()) {
            setServerOnCombo(null);
            setServerInternal(URIUtils.VSTS_ROOT_URL);
        } else {
            setServerInternal(getServerFromCombo());
        }
        ignoreServerChangeEvents = false;
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

    private class ProposalProvider implements IContentProposalProvider {
        private final List<String> proposals = new ArrayList<String>();

        public void setProposals(final List<String> proposals) {
            this.proposals.clear();
            if (proposals != null) {
                for (final String p : proposals) {
                    if (!StringUtil.isNullOrEmpty(p)) {
                        this.proposals.add(p);
                    }
                }
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

            final List<IContentProposal> list = new ArrayList<IContentProposal>(proposals.size());
            if (StringUtil.isNullOrEmpty(contents)) {
                for (int i = 0; i < proposals.size(); i++) {
                    list.add(new ContentProposal(proposals.get(i)));
                }
            } else {
                final String search = contents.toLowerCase().trim();
                for (int i = 0; i < proposals.size(); i++) {
                    final String p = proposals.get(i);
                    if (StringUtil.containsIgnoreCase(p, search)) {
                        list.add(new ContentProposal(p));
                    }
                }
            }

            return list.toArray(new IContentProposal[list.size()]);
        }

        private class ContentProposal implements IContentProposal {
            private final String content;

            public ContentProposal(final String content) {
                this.content = content;
            }

            @Override
            public String getContent() {
                return content;
            }

            @Override
            public int getCursorPosition() {
                return 0;
            }

            @Override
            public String getLabel() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

        }
    }
}
