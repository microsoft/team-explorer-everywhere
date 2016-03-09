// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.catalog.TeamProjectCollectionInfo;
import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.QueryActiveTeamProjectsCommand;
import com.microsoft.tfs.client.common.commands.QueryTeamProjectsCommand;
import com.microsoft.tfs.client.common.commands.configuration.QueryProjectCollectionsCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ThreadedCancellableCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.TeamProjectTable;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.client.common.ui.tasks.ConnectToConfigurationServerTask;
import com.microsoft.tfs.client.common.util.TeamContextCache;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class TeamProjectSelectControl extends BaseControl {
    private static final Log log = LogFactory.getLog(TeamProjectSelectControl.class);

    public static final String PROJECT_COLLECTION_TABLE_ID = "TeamProjectSelectControl.projectCollectionTable"; //$NON-NLS-1$
    public static final String PROJECT_TABLE_ID = "TeamProjectSelectControl.projectTable"; //$NON-NLS-1$

    public static final CodeMarker CODEMARKER_REFRESH_START =
        new CodeMarker("com.microsoft.tfs.client.common.ui.controls.profiles.TeamProjectSelectControl#refreshStart"); //$NON-NLS-1$
    public static final CodeMarker CODEMARKER_REFRESH_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.controls.profiles.TeamProjectSelectControl#refreshComplete"); //$NON-NLS-1$

    /*
     * Catch setURI() before we're painted so that we can update properly once
     * we are.
     */
    private URI initialServerURI = null;
    private Credentials initialCredentials = null;

    /*
     * This is the currently selected server URI and credentials.
     */
    private URI serverURI = null;
    private Credentials credentials = null;

    private boolean serverReadonly = false;
    private boolean ignoreServerChangeEvents = false;

    /**
     * This is the configuration server for the selected profile. May be null
     * for a 2008 server.
     */
    private TFSConfigurationServer configurationServer;

    /**
     * This is the list of all team project collections for the selected
     * configuration server. May be null for a 2008 server.
     */
    private TeamProjectCollectionInfo[] projectCollections = null;

    /**
     * This is the TFSTeamProjectCollection for the selected project collection.
     */
    private TFSTeamProjectCollection collection;
    private boolean collectionReadonly = false;

    /**
     * Cache the team projects for particular collections
     */
    private Map<TFSTeamProjectCollection, ProjectInfo[]> teamProjectsForCollection = null;

    /**
     * This is the projects for the selected project collection
     */
    private ProjectInfo[] teamProjects;

    /**
     * This is the selected team project
     */
    private ProjectInfo[] selectedProjects;

    /**
     * This is the last set of selected team projects from the last refresh.
     * This is used to fire events.
     */
    private ProjectInfo[] lastSelectedProjects;

    private ICommandExecutor commandExecutor;
    private ICommandExecutor noErrorDialogCommandExecutor;

    private final ServerSelectControl serverControl;
    private final ProjectCollectionTable projectCollectionTable;
    private final TeamProjectTable projectTable;
    private final CompatibilityLinkControl useDifferentCredentialsLinkControl;

    private boolean hasPainted = false;

    private final SingleListenerFacade listeners = new SingleListenerFacade(ProjectSelectionChangedListener.class);

    public TeamProjectSelectControl(
        final Composite parent,
        final int style,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags) {
        super(parent, style);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing() * 2;
        setLayout(layout);

        serverControl = new ServerSelectControl(this, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(serverControl);

        if (serverURI != null) {
            serverControl.setServerURI(serverURI);
        } else if (serverControl.getServerURI() != null) {
            setServer(serverControl.getServerURI(), null);
        }
        serverControl.setEnabled(!serverReadonly);
        serverControl.addServerSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                if (!ignoreServerChangeEvents) {
                    if (serverControl.getLastAddedConnection() != null) {
                        setConnectionInternal(serverControl.getLastAddedConnection());
                    } else {
                        setServerInternal(serverControl.getServerURI(), null);
                    }
                }
            }
        });

        final SashForm sashForm = new SashForm(this, SWT.HORIZONTAL);
        sashForm.setLayout(new FillLayout());
        GridDataBuilder.newInstance().hSpan(layout).fill().grab().applyTo(sashForm);

        final Composite leftSide = new Composite(sashForm, SWT.NONE);
        SWTUtil.gridLayout(leftSide, 1, false, 0, 0);

        final Composite rightSide = new Composite(sashForm, SWT.NONE);
        SWTUtil.gridLayout(rightSide, 1, false, 0, 0);

        SWTUtil.createLabel(leftSide, Messages.getString("TeamProjectSelectControl.CollectionsLabelText")); //$NON-NLS-1$
        projectCollectionTable = new ProjectCollectionTable(leftSide, SWT.NONE);
        AutomationIDHelper.setWidgetID(projectCollectionTable.getTable(), PROJECT_COLLECTION_TABLE_ID);
        projectCollectionTable.setEnabled(!collectionReadonly);
        GridDataBuilder.newInstance().grab().fill().applyTo(projectCollectionTable);

        projectCollectionTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                setTeamProjectCollectionInternal(projectCollectionTable.getSelectedProjectCollection());
            }
        });

        SWTUtil.createLabel(rightSide, Messages.getString("TeamProjectSelectControl.ProjectsLabelText")); //$NON-NLS-1$
        projectTable = new TeamProjectTable(rightSide, SWT.NONE, true);
        projectTable.setSourceControlCapabilityFlags(sourceControlCapabilityFlags);
        AutomationIDHelper.setWidgetID(projectTable.getTable(), PROJECT_TABLE_ID);
        GridDataBuilder.newInstance().grab().fill().applyTo(projectTable);

        projectTable.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(final CheckStateChangedEvent event) {
                setSelectedTeamProjectsInternal(projectTable.getCheckedProjects());
            }
        });

        sashForm.setWeights(new int[] {
            50,
            50
        });

        useDifferentCredentialsLinkControl = CompatibilityLinkFactory.createLink(this, SWT.NONE);
        useDifferentCredentialsLinkControl.setSimpleText(
            Messages.getString("TeamProjectSelectControl.UseDifferentCredentialsLabelText")); //$NON-NLS-1$
        useDifferentCredentialsLinkControl.getControl().setEnabled(false);
        useDifferentCredentialsLinkControl.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                useDifferentCredentials();
            }
        });
        GridDataBuilder.newInstance().hFill().hGrab().wHint(getMinimumMessageAreaWidth()).applyTo(
            useDifferentCredentialsLinkControl.getControl());

        /*
         * Hook up a paint listener to refresh the control as soon as we're
         * visible.
         */
        addPaintListener(new PaintListener() {
            @Override
            public void paintControl(final PaintEvent e) {
                /*
                 * paint() could end up getting called recursively, thus we
                 * could get called again before we remove this first-paint
                 * listener, so check.
                 */
                if (hasPainted) {
                    return;
                }

                final TeamProjectSelectControl control = TeamProjectSelectControl.this;
                final PaintListener paintListener = this;

                /*
                 * Post a new UI runnable. This works around a bug in Mac OS: we
                 * get a paint call before our actual shell is drawn. This is to
                 * delay the request.
                 */
                getShell().getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        /*
                         * double check: we could have had many paint calls by
                         * the time any of our runnables execute.
                         */
                        if (hasPainted) {
                            return;
                        }

                        hasPainted = true;
                        control.removePaintListener(paintListener);

                        if (initialServerURI != null) {
                            setServer(initialServerURI, initialCredentials);
                        } else {
                            refresh();
                        }
                    }
                });
            }
        });
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

    public void setProjects(final ProjectInfo[] projects) {
        projectTable.setProjects(projects);
    }

    public ProjectInfo[] getProjects() {
        return projectTable.getProjects();
    }

    public TFSTeamProjectCollection getTeamProjectCollection() {
        return collection;
    }

    public void setProjectCollectionReadonly(final boolean readonly) {
        collectionReadonly = readonly;

        if (projectCollectionTable != null && !projectCollectionTable.isDisposed()) {
            projectCollectionTable.setEnabled(!readonly);
        }
    }

    public ProjectInfo[] getSelectedProjects() {
        return projectTable.getCheckedProjects();
    }

    public void addProjectSelectionChangedListener(final ProjectSelectionChangedListener listener) {
        listeners.addListener(listener);
    }

    public void removeProjectSelectionChangedListener(final ProjectSelectionChangedListener listener) {
        listeners.removeListener(listener);
    }

    public void setServerURIReadonly(final boolean readonly) {
        serverReadonly = readonly;

        if (serverControl != null && !serverControl.isDisposed()) {
            serverControl.setEnabled(!serverReadonly);
        }
    }

    public void setServer(final URI serverURI, final Credentials credentials) {
        /*
         * If we haven't yet painted, store this away for paint time. This keeps
         * us from popping up dialogs before we've been displayed.
         */
        if (!hasPainted) {
            initialServerURI = serverURI;
            initialCredentials = credentials;
            return;
        }

        ignoreServerChangeEvents = true;
        serverControl.setServerURI(serverURI);
        ignoreServerChangeEvents = false;

        setServerInternal(serverURI, credentials);
    }

    private void setServerInternal(final URI serverURI, final Credentials credentials) {
        if (this.serverURI != null
            && this.serverURI.equals(serverURI)
            && this.credentials != null
            && this.credentials.equals(credentials)) {
            return;
        }

        this.serverURI = null;
        configurationServer = null;
        projectCollections = null;
        collection = null;
        teamProjectsForCollection = null;
        teamProjects = null;
        selectedProjects = null;

        projectCollectionTable.setProjectCollections(null);
        projectTable.setProjects(null);

        if (serverURI == null) {
            refresh();
            return;
        }

        this.serverURI = serverURI;
        this.credentials = credentials;

        refresh();
    }

    public void setConnection(TFSConnection connection) {
        TFSTeamProjectCollection collection = null;

        if (connection instanceof TFSTeamProjectCollection
            && ((TFSTeamProjectCollection) connection).getConfigurationServer() != null) {
            collection = (TFSTeamProjectCollection) connection;
            connection = collection.getConfigurationServer();
        }

        serverURI = connection.getBaseURI();

        ignoreServerChangeEvents = true;
        serverControl.setServerURI(connection.getBaseURI());
        ignoreServerChangeEvents = false;

        setConnectionInternal(connection);

        if (collection != null) {
            setTeamProjectCollectionInternal(collection);
        }
    }

    private void setConnectionInternal(final TFSConnection connection) {
        if (connection instanceof TFSConfigurationServer) {
            setConfigurationServer((TFSConfigurationServer) connection);
        } else if (connection instanceof TFSTeamProjectCollection) {
            /* Hack for 2008: build a fake project collection */
            final TeamProjectCollectionInfo[] legacyCollection = new TeamProjectCollectionInfo[] {
                new TeamProjectCollectionInfo(
                    GUID.EMPTY,
                    MessageFormat.format(
                        Messages.getString("TeamProjectSelectControl.DisplayUrlFormat"), //$NON-NLS-1$
                        connection.getName()),
                    MessageFormat.format(
                        Messages.getString("TeamProjectSelectControl.TfsInstanceFormat"), //$NON-NLS-1$
                        connection.getBaseURI()))
            };

            serverURI = connection.getBaseURI();
            credentials = connection.getCredentials();
            configurationServer = null;
            projectCollections = legacyCollection;
            teamProjectsForCollection = new HashMap();
            teamProjects = null;
            selectedProjects = null;

            projectCollectionTable.setProjectCollections(legacyCollection);

            setTeamProjectCollectionInternal((TFSTeamProjectCollection) connection);
        } else {
            throw new RuntimeException(Messages.getString("TeamProjectSelectControl.UnknownConnectionType")); //$NON-NLS-1$
        }
    }

    private void setConfigurationServer(final TFSConfigurationServer configurationServer) {
        if (this.configurationServer == configurationServer) {
            return;
        }

        this.configurationServer = configurationServer;
        serverURI = configurationServer.getBaseURI();
        credentials = configurationServer.getCredentials();
        projectCollections = null;
        collection = null;
        teamProjectsForCollection = new HashMap();
        teamProjects = null;
        selectedProjects = null;

        refresh();
    }

    private void setTeamProjectCollectionInternal(final TeamProjectCollectionInfo collectionInfo) {
        TFSTeamProjectCollection collection = null;

        if (configurationServer == null) {
            return;
        }

        if (collectionInfo != null) {
            collection = configurationServer.getTeamProjectCollection(collectionInfo.getIdentifier());
        }

        setTeamProjectCollectionInternal(collection);
    }

    private void setTeamProjectCollectionInternal(final TFSTeamProjectCollection collection) {
        if (this.collection == collection) {
            return;
        }

        this.collection = collection;
        teamProjects = null;
        selectedProjects = null;

        /* Persist this setting */
        setDefaultTeamProjectCollection(collection);

        refresh();
    }

    private void setSelectedTeamProjectsInternal(final ProjectInfo[] selectedProjects) {
        if (Arrays.equals(this.selectedProjects, selectedProjects)) {
            return;
        }

        this.selectedProjects = selectedProjects;

        refresh();
    }

    private void refresh() {
        if (!hasPainted) {
            return;
        }

        CodeMarkerDispatch.dispatch(CODEMARKER_REFRESH_START);

        /* Set active projects */
        if (selectedProjects == null && teamProjects != null && collection != null && projectCollections != null) {
            refreshSelectedTeamProjects();
        }
        /* Query team projects for this project collection */
        else if (teamProjects == null && collection != null && projectCollections != null) {
            refreshTeamProjects();
        }
        /* Query project collections for selected configuration server */
        else if (projectCollections == null && configurationServer != null) {
            refreshProjectCollections();
        }
        /* Build a connection for the selected profile */
        else if (collection == null && configurationServer == null && serverURI != null) {
            refreshConnection();
        }

        if (lastSelectedProjects == null || !Arrays.equals(lastSelectedProjects, selectedProjects)) {
            ((ProjectSelectionChangedListener) listeners.getListener()).onProjectSelectionChanged(
                new ProjectSelectionChangedEvent(selectedProjects));
            lastSelectedProjects = selectedProjects;
        }

        CodeMarkerDispatch.dispatch(CODEMARKER_REFRESH_COMPLETE);
    }

    private void refreshSelectedTeamProjects() {
        selectedProjects = null;
        projectTable.setCheckedProjects(null);

        if (teamProjects.length == 0) {
            return;
        }

        final QueryActiveTeamProjectsCommand queryCommand =
            new QueryActiveTeamProjectsCommand(collection, teamProjects);
        final IStatus status = getCommandExecutor().execute(queryCommand);

        if (!status.isOK()) {
            return;
        }

        selectedProjects = TeamContextCache.getInstance().getActiveTeamProjects(collection, teamProjects);
        projectTable.setCheckedProjects(selectedProjects);
        refresh();
    }

    private void refreshTeamProjects() {
        teamProjects = null;
        projectTable.setProjects(null);

        /* Look up in the cache */
        teamProjects = teamProjectsForCollection.get(collection);

        if (teamProjects != null) {
            projectTable.setProjects(teamProjects);

            refresh();
            return;
        }

        final QueryTeamProjectsCommand queryCommand = new QueryTeamProjectsCommand(collection);
        final IStatus status = getCommandExecutor().execute(new ThreadedCancellableCommand(queryCommand));

        if (!status.isOK()) {
            teamProjectsForCollection.put(collection, new ProjectInfo[0]);

            return;
        }

        teamProjects = queryCommand.getProjects();
        teamProjectsForCollection.put(collection, teamProjects);
        projectTable.setProjects(teamProjects);

        refresh();
    }

    private void refreshProjectCollections() {
        projectCollections = null;
        teamProjects = null;
        projectCollectionTable.setProjectCollections(null);
        projectTable.setProjects(null);

        final QueryProjectCollectionsCommand queryCommand = new QueryProjectCollectionsCommand(configurationServer);
        final IStatus status = getCommandExecutor().execute(new ThreadedCancellableCommand(queryCommand));

        if (!status.isOK()) {
            return;
        }

        projectCollections = queryCommand.getProjectCollections();

        projectCollectionTable.setProjectCollections(projectCollections);

        if (collection != null) {
            setProjectCollectionSelection(collection.getInstanceID());
        } else if (configurationServer != null) {
            setProjectCollectionSelection(getDefaultTeamProjectCollection(configurationServer));
        }

        useDifferentCredentialsLinkControl.getControl().setEnabled(configurationServer != null);

        refresh();
    }

    private void setProjectCollectionSelection(final GUID projectCollectionId) {
        if (projectCollections != null) {
            for (int i = 0; i < projectCollections.length; i++) {
                if (projectCollections[i].getIdentifier().equals(projectCollectionId)) {
                    projectCollectionTable.setSelectedProjectCollection(projectCollections[i]);
                    return;
                }
            }

            /*
             * Do not set a default project collection, require the user to
             * explicitly select one.
             */
        }
    }

    /**
     * Connect to server with the given profile, then refresh server
     * information.
     */
    private void refreshConnection() {
        collection = null;
        projectCollections = null;
        teamProjectsForCollection = null;
        teamProjects = null;
        projectCollectionTable.setProjectCollections(null);
        projectTable.setProjects(null);
        useDifferentCredentialsLinkControl.getControl().setEnabled(false);

        TFSConnection connection;

        final ConnectToConfigurationServerTask connectTask =
            new ConnectToConfigurationServerTask(getShell(), serverURI, credentials);
        connectTask.setCommandExecutor(getNoErrorDialogCommandExecutor());
        final IStatus status = connectTask.run();

        if (status.isOK()) {
            connection = connectTask.getConnection();

            setConnectionInternal(connection);
        } else {
            /* Connection cancelled */
            serverURI = null;
            credentials = null;
            configurationServer = null;

            try {
                serverControl.setServerURI(null);
            } finally {
                ignoreServerChangeEvents = false;
            }

            /*
             * Connection canceled. Fire Refresh complete event since refresh()
             * will not be called and no more work will be done here.
             */
            CodeMarkerDispatch.dispatch(CODEMARKER_REFRESH_COMPLETE);
        }
    }

    /**
     * Opens UI to get new credentials for the specified profile.
     *
     * @param profile
     *        the profile to get new credentials for (may be <code>null</code>)
     */
    private void useDifferentCredentials() {
        if (serverURI == null) {
            return;
        }

        /*
         * For on-premises servers, simply use empty UsernamePasswordCredentials
         * (to force a username/password dialog.) For hosted servers, use
         * default NT credentials at all (to avoid the username/password
         * dialog.)
         */
        final Credentials credentials =
            (configurationServer != null && configurationServer.getCredentials() instanceof CookieCredentials)
                ? new DefaultNTCredentials() : new UsernamePasswordCredentials("", null); //$NON-NLS-1$

        Browser.clearSessions();
        if (configurationServer != null) {
            configurationServer.getHTTPClient().getState().clearCookies();
            configurationServer.getHTTPClient().getState().clearCredentials();
            configurationServer.getCredentialsHolder().set(credentials);
        }
        setServer(serverURI, credentials);
    }

    /**
     * Sets this to be the default project collection for the given
     * configuration server.
     *
     * @param collection
     *        The default collection to set
     */
    private void setDefaultTeamProjectCollection(final TFSTeamProjectCollection collection) {
        if (collection == null || collection.getConfigurationServer() == null) {
            return;
        }

        try {
            UIConnectionPersistence.getInstance().setLastUsedProjectCollection(collection);
        } catch (final Exception e) {
            log.info("Could not set default team project collection: ", e); //$NON-NLS-1$
        }
    }

    /**
     * Gets the last used team project collection for the given
     * {@link TFSConfigurationServer} or <code>null</code> if none is available.
     *
     * @param configurationServer
     *        The connected configuration server
     * @return The GUID of the last used project collection
     */
    private GUID getDefaultTeamProjectCollection(final TFSConfigurationServer configurationServer) {
        if (configurationServer == null) {
            return null;
        }

        return UIConnectionPersistence.getInstance().getLastUsedProjectCollection(configurationServer);
    }

    public interface ProjectSelectionChangedListener {
        public void onProjectSelectionChanged(ProjectSelectionChangedEvent event);
    }

    public final class ProjectSelectionChangedEvent {
        private final ProjectInfo[] selectedProjects;

        private ProjectSelectionChangedEvent(final ProjectInfo[] selectedProjects) {
            this.selectedProjects = selectedProjects;
        }

        public ProjectInfo[] getSelectedProjects() {
            return selectedProjects;
        }
    }
}
