// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class ServerTypeSelectControl extends BaseControl {
    private static final Log log = LogFactory.getLog(ServerTypeSelectControl.class);

    private URI serverURI = null;
    private boolean serverReadonly = false;
    private boolean ignoreServerChangeEvents = false;
    private ICommandExecutor commandExecutor;
    private ICommandExecutor noErrorDialogCommandExecutor;
    private final ServerSelectControl serverControl;
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

        SelectionAdapter buttonsChanged = new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                onButtonsChanged();
            }
        };

        vstsButton = new Button(this, SWT.RADIO);
        vstsButton.setText(Messages.getString("ServerTypeSelectControl.BrowseVstsButtonText")); //$NON-NLS-1$
        vstsButton.setSelection(true);
        vstsButton.addSelectionListener(buttonsChanged);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(vstsButton);

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

        tfsButton = new Button(this, SWT.RADIO);
        tfsButton.setText(Messages.getString("ServerTypeSelectControl.ConnectButtonText")); //$NON-NLS-1$
        tfsButton.addSelectionListener(buttonsChanged);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().vIndent(getVerticalSpacing() * 2).applyTo(
            tfsButton);

        serverControl = new ServerSelectControl(this, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(layout).hIndent(getHorizontalSpacing() * 4).hFill().hGrab().vIndent(
            getVerticalSpacing()).applyTo(serverControl);

        if (serverURI != null) {
            serverControl.setServerURI(serverURI);
        } else if (serverControl.getServerURI() != null) {
            setServer(serverControl.getServerURI());
        }
        serverControl.setEnabled(!serverReadonly);
        serverControl.addServerSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                if (!ignoreServerChangeEvents) {
                    setServer(serverControl.getServerURI());
                }
            }
        });

        // fire the buttons changed event so that the initial state is updated
        onButtonsChanged();
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
        return this.serverURI;
    }

    public void setServer(final URI serverURI) {
        ignoreServerChangeEvents = true;
        serverControl.setServerURI(serverURI);

        // If we have an authority and it matches the one selected in the combo,
        // change the radio button
        if (serverURI != null && ServerURIUtils.equals(serverURI, serverControl.getServerURI())) {
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
            setServer(URIUtils.VSTS_ROOT_URL);
        } else {
            setServer(serverControl.getServerURI());
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
}
