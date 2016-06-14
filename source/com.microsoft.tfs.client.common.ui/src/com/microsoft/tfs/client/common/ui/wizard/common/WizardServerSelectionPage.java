// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.common;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.credentials.CredentialsHelper;
import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.config.UIClientConnectionAdvisor;
import com.microsoft.tfs.client.common.ui.config.UITransportAuthRunnable;
import com.microsoft.tfs.client.common.ui.config.UITransportFederatedAuthRunnable;
import com.microsoft.tfs.client.common.ui.controls.connect.ServerTypeSelectControl;
import com.microsoft.tfs.client.common.ui.controls.connect.ServerTypeSelectControl.ServerTypeSelectionChangedEvent;
import com.microsoft.tfs.client.common.ui.controls.connect.ServerTypeSelectControl.ServerTypeSelectionChangedListener;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizard;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.tasks.ConnectToConfigurationServerTask;
import com.microsoft.tfs.client.common.ui.wizard.connectwizard.ConnectWizard;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.JwtCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Platform;
import com.microsoft.visualstudio.services.account.AccountHttpClient;
import com.microsoft.visualstudio.services.account.model.Account;
import com.microsoft.visualstudio.services.account.model.Profile;
import com.microsoft.vss.client.core.model.VssResourceNotFoundException;

public class WizardServerSelectionPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "WizardServerSelectionPage"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(WizardServerSelectionPage.class);

    private static final int MAX_CREDENTIALS_RETRIES = 3;

    private ServerTypeSelectControl serverTypeSelectControl;

    public WizardServerSelectionPage() {
        super(
            PAGE_NAME,
            Messages.getString("WizardServerSelectionPage.PageTitle"), //$NON-NLS-1$
            Messages.getString("WizardServerSelectionPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final Composite container = new Composite(parent, SWT.NULL);
        setControl(container);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        container.setLayout(layout);

        serverTypeSelectControl = new ServerTypeSelectControl(container, SWT.NONE);
        GridDataBuilder.newInstance().grab().fill().applyTo(serverTypeSelectControl);

        serverTypeSelectControl.addListener(new ServerTypeSelectionChangedListener() {
            @Override
            public void onServerTypeSelectionChanged(final ServerTypeSelectionChangedEvent event) {
                final URI serverURI = serverTypeSelectControl.getServer();
                if (serverURI != null) {
                    setPageData(serverURI);
                } else {
                    removePageData();
                }

                setPageComplete(serverURI != null);
            }
        });

        setPageComplete(serverTypeSelectControl.getServer() != null);
    }

    @Override
    protected boolean onPageFinished() {
        if (serverTypeSelectControl.isVstsSelected()) {

            final AtomicReference<AccountHttpClient> restClientHolder = new AtomicReference<AccountHttpClient>(null);
            final AtomicReference<Credentials> credentialsHolder = new AtomicReference<Credentials>(null);
            final Profile profile = getUserProfile(restClientHolder, credentialsHolder);

            final List<TFSConnection> configurationServers =
                getConfigurationServers(profile, credentialsHolder.get(), restClientHolder.get());
            setPageData(configurationServers);

            return configurationServers.size() > 0;
        } else {
            final URI serverURI = serverTypeSelectControl.getServer();

            if (serverURI == null) {
                return false;
            }

            final TFSConnection connection = openAccount(serverURI, null);
            setPageData(connection);

            return connection != null;
        }
    }

    private Profile getUserProfile(
        final AtomicReference<AccountHttpClient> restClientHolder,
        final AtomicReference<Credentials> credentialsHolder) {
        Profile profile = null;

        for (int retriesLeft = MAX_CREDENTIALS_RETRIES; retriesLeft > 0; retriesLeft--) {
            final Credentials vstsCredentials = getVstsRootCredentials(retriesLeft == MAX_CREDENTIALS_RETRIES);

            if (vstsCredentials == null) {
                log.info(" Credentials dialog has been cancelled by the user."); //$NON-NLS-1$
                break;
            }

            final TFSConnection vstsConnection =
                new TFSTeamProjectCollection(URIUtils.VSTS_ROOT_URL, vstsCredentials, new UIClientConnectionAdvisor());
            final AccountHttpClient accountClient = new AccountHttpClient(vstsConnection);

            try {
                profile = accountClient.getMyProfile();
                restClientHolder.set(accountClient);
                credentialsHolder.set(vstsCredentials);
                break;
            } catch (final Exception e) {
                if (retriesLeft > 1 && (e instanceof VssResourceNotFoundException)) {
                    updateCredentials(URIUtils.VSTS_ROOT_URL, null);
                } else {
                    log.error("Error connecting to VSTS", e); //$NON-NLS-1$

                    final String title = Messages.getString("WizardServerSelectionPage.ErrorConnectingVstsText"); //$NON-NLS-1$

                    final String message = (e instanceof VssResourceNotFoundException)
                        ? Messages.getString("WizardServerSelectionPage.VstsAuthFailedText") : e.getLocalizedMessage(); //$NON-NLS-1$

                    ErrorDialog.openError(
                        getShell(),
                        title,
                        null,
                        new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, e));
                    break;
                }
            }
        }

        return profile;
    }

    private List<TFSConnection> getConfigurationServers(
        final Profile profile,
        final Credentials credentials,
        final AccountHttpClient accountClient) {
        final List<TFSConnection> configurationServers = new ArrayList<TFSConnection>(100);

        if (profile != null) {
            log.info("Profile ID = " + profile.getId()); //$NON-NLS-1$

            final List<Account> accounts = accountClient.getAccounts(profile.getId());

            for (final Account account : accounts) {
                log.debug("Account name = " + account.getAccountName()); //$NON-NLS-1$
                log.debug("Account URI  = " + account.getAccountUri()); //$NON-NLS-1$

                final String accountURI = "https://" + account.getAccountName() + ".visualstudio.com"; //$NON-NLS-1$ //$NON-NLS-2$

                try {
                    final URI uri = URIUtils.newURI(accountURI);
                    final TFSConnection configurationServer =
                        openAccount(uri, CredentialsHelper.getOAuthCredentials(uri));
                    if (configurationServer != null) {
                        configurationServers.add(configurationServer);
                    }
                } catch (final Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

        }

        return configurationServers;
    }

    private Credentials getVstsRootCredentials(final boolean tryCurrentCredentials) {
        final Credentials vstsCredentials;

        if (EnvironmentVariables.getBoolean(EnvironmentVariables.USE_OAUTH_LIBRARY, false)) {

            vstsCredentials = CredentialsHelper.getOAuthCredentials(null);

            if (vstsCredentials != null && (vstsCredentials instanceof JwtCredentials)) {
                return vstsCredentials;
            } else {
                return null;
            }
        }

        final Credentials currentCredentials = tryCurrentCredentials ? getCurrentCredentials() : null;

        if (currentCredentials == null || !(currentCredentials instanceof CookieCredentials)) {
            final UITransportAuthRunnable dialogRunnable = new UITransportFederatedAuthRunnable();

            log.debug("Prompt for credentials"); //$NON-NLS-1$
            UIHelpers.runOnUIThread(getShell(), false, dialogRunnable);

            vstsCredentials = dialogRunnable.getCredentials();

            log.debug("The dialog returned cedentials: " //$NON-NLS-1$
                + (vstsCredentials == null ? "null" : vstsCredentials.getClass().getName())); //$NON-NLS-1$

            return vstsCredentials;
        } else {
            return ((CookieCredentials) currentCredentials).setDomain(URIUtils.VSTS_ROOT_URL.getHost());
        }
    }

    private Credentials getCurrentCredentials() {
        final ExtendedWizard wizard = getExtendedWizard();
        final TFSConnection connection;
        if (wizard.hasPageData(TFSConnection.class)) {
            connection = (TFSConnection) wizard.getPageData(TFSConnection.class);
        } else if (wizard.hasPageData(TFSConnection[].class)) {
            final TFSConnection[] connections = (TFSConnection[]) wizard.getPageData(TFSConnection[].class);
            connection = connections.length > 0 ? connections[0] : null;
        } else {
            connection = null;
        }

        if (connection != null) {
            return connection.getCredentials();
        } else {
            return null;
        }
    }

    private Credentials getAccountCredentials(final URI accountUrl, final Credentials proposedCredentials) {
        if (proposedCredentials == null) {
            final CredentialsManager credentialsManager =
                EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);
            final CachedCredentials cachedCredentials = credentialsManager.getCredentials(accountUrl);

            if (cachedCredentials != null) {
                return cachedCredentials.toCredentials();
            } else {
                /*
                 * For on-premises servers, simply use empty
                 * UsernamePasswordCredentials (to force a username/password
                 * dialog.) For hosted servers, use default NT credentials at
                 * all (to avoid the username/password dialog.)
                 */
                return ServerURIUtils.isHosted(accountUrl) || Platform.isCurrentPlatform(Platform.WINDOWS)
                    ? new DefaultNTCredentials() : new UsernamePasswordCredentials("", null); //$NON-NLS-1$
            }
        } else if (proposedCredentials instanceof CookieCredentials) {
            return ((CookieCredentials) proposedCredentials).setDomain(accountUrl.getHost());
        } else {
            return proposedCredentials;
        }
    }

    private void updateCredentials(final URI accountUrl, final Credentials credentials) {
        final CredentialsManager credentialsManager =
            EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

        if (credentials != null && !(credentials instanceof DefaultNTCredentials)) {
            log.debug("Save the new Cookie Credentials in the Eclipse secure storage for future sessions."); //$NON-NLS-1$
            credentialsManager.setCredentials(new CachedCredentials(accountUrl, credentials));
        } else {
            credentialsManager.removeCredentials(accountUrl);
        }
    }

    private TFSConnection openAccount(final URI accountUrl, final Credentials credentials) {

        final Credentials accountCredentials = getAccountCredentials(accountUrl, credentials);

        final ICommandExecutor noErrorDialogCommandExecutor = getCommandExecutor();
        noErrorDialogCommandExecutor.setCommandFinishedCallback(
            UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        final ConnectToConfigurationServerTask connectTask =
            new ConnectToConfigurationServerTask(getShell(), accountUrl, accountCredentials);
        connectTask.setCommandExecutor(noErrorDialogCommandExecutor);
        final IStatus status = connectTask.run();

        final TFSConnection connection;
        if (status.isOK()) {
            connection = connectTask.getConnection();
            updateCredentials(accountUrl, connection.getCredentials());
        } else {
            /* Connection cancelled */
            connection = null;
        }

        return connection;
    }

    private void setPageData(final URI serverUri) {
        removePageData();
        getExtendedWizard().setPageData(URI.class, serverUri);
    }

    private void setPageData(final List<TFSConnection> connections) {
        removePageData();
        getExtendedWizard().setPageData(URI.class, serverTypeSelectControl.getServer());
        getExtendedWizard().setPageData(
            TFSConnection[].class,
            connections.toArray(new TFSConnection[connections.size()]));
    }

    private void setPageData(final TFSConnection connection) {
        removePageData();
        if (connection != null) {
            getExtendedWizard().setPageData(URI.class, connection.getBaseURI());
            getExtendedWizard().setPageData(TFSConnection[].class, new TFSConnection[] {
                connection
            });
        } else {
            getExtendedWizard().setPageData(URI.class, serverTypeSelectControl.getServer());
        }
    }

    private void removePageData() {
        getExtendedWizard().removePageData(URI.class);
        getExtendedWizard().removePageData(TFSConnection[].class);

        // We need to remove the SELECTED_TEAM_PROJECTS data to make sure that
        // the selection page will be shown next
        getExtendedWizard().removePageData(ConnectWizard.SELECTED_TEAM_PROJECTS);
    }

    @Override
    protected void refresh() {
        if (serverTypeSelectControl == null) {
            return;
        }

        final URI serverURI =
            getExtendedWizard().hasPageData(URI.class) ? (URI) getExtendedWizard().getPageData(URI.class) : null;

        serverTypeSelectControl.setServer(serverURI);

        // This is the first connection page, so after refreshing we need to
        // remove related page data. This is important to avoid problems when
        // the user "backs" into this page.
        removePageData();
    }
}
