// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.connectwizard;

import java.net.URI;
import java.util.Locale;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.helpers.CredentialsHelper;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ConnectHelpers;
import com.microsoft.tfs.client.common.ui.wizard.common.WizardCrossCollectionProjectSelectionPage;
import com.microsoft.tfs.client.common.ui.wizard.common.WizardCrossCollectionSelectionPage;
import com.microsoft.tfs.client.common.ui.wizard.common.WizardServerSelectionPage;
import com.microsoft.tfs.client.common.ui.wizard.eula.AbstractEULAWizard;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.util.ServerURIComparator;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListCollectionEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListEntryType;
import com.microsoft.tfs.core.util.serverlist.ServerListManagerFactory;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

/**
 * An abstract wizard that implements TFS connections. Should be extended by
 * plugin Import and Share wizards and the common Add Team Project wizard.
 *
 * @threadsafety unknown
 */
public abstract class ConnectWizard extends AbstractEULAWizard {
    private final SourceControlCapabilityFlags sourceControlCapabilityFlags;
    private final int selectionType;
    private WizardServerSelectionPage serverSelectionPage;
    private WizardCrossCollectionSelectionPage selectionPage;

    public static final int PROJECT_SELECTION = 1;
    public static final int SERVERONLY_SELECTION = 2;

    public static final String SELECTED_TEAM_PROJECTS = "ConnectWizard.selectedTeamProjects"; //$NON-NLS-1$

    protected ConnectWizard(
        final String windowTitle,
        final ImageDescriptor defaultPageImageDescriptor,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags) {
        this(windowTitle, defaultPageImageDescriptor, null, sourceControlCapabilityFlags, PROJECT_SELECTION);
    }

    protected ConnectWizard(
        final String windowTitle,
        final ImageDescriptor defaultPageImageDescriptor,
        final String dialogSettingsKey,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags,
        final int selectionType) {
        super(windowTitle, defaultPageImageDescriptor, dialogSettingsKey);
        this.sourceControlCapabilityFlags = sourceControlCapabilityFlags;
        this.selectionType = selectionType;
    }

    protected void addConnectionPages() {
        addEULAPages();

        serverSelectionPage = new WizardServerSelectionPage();
        addPage(serverSelectionPage);

        // Attempt to add a selection page
        selectionPage = getSelectionPage();
        if (selectionPage != null) {
            addPage(selectionPage);
        }
    }

    /**
     * This method should be overridden by subclasses that wish to provide their
     * own selection page.
     */
    protected WizardCrossCollectionSelectionPage getSelectionPage() {
        final WizardCrossCollectionSelectionPage selectionPage =
            selectionType == ConnectWizard.PROJECT_SELECTION ? new WizardCrossCollectionProjectSelectionPage() : null;
        return selectionPage;
    }

    protected String getSelectionPageName() {
        if (selectionPage != null) {
            return selectionPage.getName();
        }

        return StringUtil.EMPTY;
    }

    public SourceControlCapabilityFlags getSourceControlCapabilityFlags() {
        return sourceControlCapabilityFlags;
    }

    /**
     * Will be called when a repository becomes available after being blocked
     * during initial connection. Subclasses may override.
     *
     * @param repository
     */
    protected void setRepository(final TFSRepository repository) {
    }

    protected TFSRepository initConnectionPages() {
        initEULAPages();

        /* See if we have a connection */
        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

        initConnectionPages(repository);

        return repository;
    }

    protected final void initConnectionPages(final TFSRepository repository) {
        if (repository != null) {
            final Workspace workspace = repository.getWorkspace();
            final TFSTeamProjectCollection connection = workspace.getClient().getConnection();
            setPageData(URI.class, connection.getConfigurationServer().getBaseURI());
            setPageData(TFSConnection.class, connection);
            setPageData(TFSTeamProjectCollection.class, connection);
            setPageData(TFSRepository.class, repository);
            setPageData(Workspace.class, workspace);
            ensureLastUsedServerInList(connection.getConfigurationServer().getBaseURI());
        }
        /* See if we have any profiles */
        else if (UIConnectionPersistence.getInstance().getLastUsedServerURI() != null) {
            setPageData(URI.class, UIConnectionPersistence.getInstance().getLastUsedServerURI());
            removePageData(TFSConnection.class);
            removePageData(TFSTeamProjectCollection.class);
            removePageData(TFSRepository.class);
            removePageData(Workspace.class);
            ensureLastUsedServerInList();
        } else {
            removePageData(URI.class);
            removePageData(TFSConnection.class);
            removePageData(TFSTeamProjectCollection.class);
        }
    }

    protected boolean enableNextConnectionPage(final IWizardPage currentPage) {
        return !sourceControlCapabilityFlags.isEmpty();
    }

    @Override
    public IWizardPage getPreviousPage(final IWizardPage page) {
        // If the current page is the Project page, make sure the user can
        // go back to the server type selection page
        if (page != null && selectionPage != null && page.getName().equals(selectionPage.getName())) {
            final IWizardPage previousPage = getPage(WizardServerSelectionPage.PAGE_NAME);
            return previousPage;
        }

        return super.getPreviousPage(page);
    }

    @Override
    public void previousPageSet(final IWizardPage currentPage, final IWizardPage previousPage) {
        // if the current page is the selection page, we may want to "fix" the
        // previous page
        if (currentPage != null && selectionPage != null && currentPage.getName().equals(selectionPage.getName())) {
            // Check if the previous page is something other than the server
            // selection page
            if (previousPage != null && !previousPage.getName().equals(WizardServerSelectionPage.PAGE_NAME)) {
                // If so, propagate the previous page to the server selection
                // page and set the selection page to point to the server
                // selection page
                serverSelectionPage.setPreviousPage(previousPage);
                selectionPage.setPreviousPage(serverSelectionPage);
            }
        }
    }

    protected IWizardPage getNextConnectionPage() {
        final IWizardPage nextLicensePage = getNextEULAPage();

        if (nextLicensePage != null) {
            return nextLicensePage;
        }

        // Make sure we have a list of collections for the following pages
        // to use
        if (!hasPageData(TFSConnection[].class) && hasPageData(TFSConnection.class)) {
            setPageData(TFSConnection[].class, new TFSConnection[] {
                (TFSConnection) getPageData(TFSConnection.class)
            });
        }

        // Return the ServerSelectionPage or the
        // CrossCollectionSelectionPage based on what is set
        if (!hasPageData(URI.class) || !hasPageData(TFSConnection[].class)) {
            return getPage(WizardServerSelectionPage.PAGE_NAME);
        }

        if (selectionPage != null
            && (!hasPageData(TFSTeamProjectCollection.class) || !hasPageData(ConnectWizard.SELECTED_TEAM_PROJECTS))) {
            return getPage(selectionPage.getName());
        }

        return null;
    }

    protected TFSServer finishConnection() {
        /*
         * Register this workspace with the RepositoryManager, and register
         * these projects with the ItemCache
         */
        final TFSTeamProjectCollection connection =
            (TFSTeamProjectCollection) getPageData(TFSTeamProjectCollection.class);
        final TFSServer server =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getOrCreateServer(connection);

        /* Set connection profile as last-used profile */
        UIConnectionPersistence.getInstance().setLastUsedProjectCollection(connection);

        /* Set this collection into the server list. */
        updateServerList(connection);

        /* Configure the wizard page data */

        setPageData(TFSServer.class, server);

        if (hasPageData(SELECTED_TEAM_PROJECTS)) {
            final ProjectInfo[] selectedProjects = (ProjectInfo[]) getPageData(SELECTED_TEAM_PROJECTS);
            server.getProjectCache().setActiveTeamProjects(selectedProjects);
        }

        final ProjectInfo currentTeamProject = server.getProjectCache().getCurrentTeamProject();
        if (currentTeamProject != null) {
            ConnectHelpers.showHideViews(currentTeamProject.getSourceControlCapabilityFlags());
        } else {
            ConnectHelpers.showHideViews(null);
        }

        /* Create PAT for EGit access to VSTS if needed */
        CredentialsHelper.refreshCredentialsForGit(connection);

        return server;
    }

    /**
     * Adds the given TFSTeamProjectCollection to the list of connections
     * beneath the configuration server.
     *
     * @param connection
     *        The {@link TFSTeamProjectCollection} to store
     */
    private void updateServerList(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        final ServerList serverList =
            ServerListManagerFactory.getServerListProvider(DefaultPersistenceStoreProvider.INSTANCE).getServerList();

        if (serverList == null) {
            return;
        }

        final TFSConnection configurationServer =
            connection.getConfigurationServer() != null ? connection.getConfigurationServer() : connection;

        final URI collectionURI = ServerURIUtils.normalizeURI(connection.getBaseURI(), true);
        final URI configurationURI = ServerURIUtils.normalizeURI(configurationServer.getBaseURI(), true);

        if (serverList != null && serverList.contains(configurationURI)) {
            final ServerListConfigurationEntry serverListEntry = serverList.getServer(configurationURI);

            for (final ServerListCollectionEntry collectionEntry : serverListEntry.getCollections()) {
                /*
                 * We knew about this collection previously.
                 *
                 * Note: merely connecting to the instance does not clear the
                 * Offline flag in Visual Studio.
                 */
                if (ServerURIComparator.INSTANCE.compare(collectionEntry.getURI(), collectionURI) == 0) {
                    return;
                }
            }

            /* 2010: display collection name, 2008: display host name */
            final String collectionName =
                (connection.getCatalogNode() != null && connection.getCatalogNode().getResource() != null)
                    ? connection.getCatalogNode().getResource().getDisplayName()
                    : connection.getBaseURI().getHost().toLowerCase(Locale.ENGLISH);

            serverListEntry.getCollections().add(
                new ServerListCollectionEntry(
                    collectionName,
                    ServerListEntryType.TEAM_PROJECT_COLLECTION,
                    collectionURI));

            ServerListManagerFactory.getServerListProvider(DefaultPersistenceStoreProvider.INSTANCE).setServerList(
                serverList);
        }
    }

    private void ensureLastUsedServerInList() {
        ensureLastUsedServerInList(null);
    }

    /**
     * adds the currently connected server to the server list used in populating
     * the dialog in case it is not there
     *
     *
     * @param serverURI
     *        the currently connected server URI
     */
    private void ensureLastUsedServerInList(URI serverURI) {
        if (serverURI == null) {
            serverURI = UIConnectionPersistence.getInstance().getLastUsedServerURI();
        }

        final ServerList serversList =
            ServerListManagerFactory.getServerListProvider(DefaultPersistenceStoreProvider.INSTANCE).getServerList();
        // add it back to the servers list if it is not there
        if (!serversList.contains(serverURI)) {
            final ServerListConfigurationEntry configEntry = new ServerListConfigurationEntry(
                serverURI.getHost(),
                ServerListEntryType.TEAM_PROJECT_COLLECTION,
                serverURI);
            serversList.add(configEntry);
            ServerListManagerFactory.getServerListProvider(DefaultPersistenceStoreProvider.INSTANCE).setServerList(
                serversList);
        }
    }

    protected TFSRepository finishWorkspace(final Workspace workspace) {
        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().setDefaultRepository(
                workspace);

        setPageData(TFSRepository.class, repository);

        /* Set this workspace as last-used workspace */
        UIConnectionPersistence.getInstance().setLastUsedWorkspace(workspace);

        return repository;
    }
}
