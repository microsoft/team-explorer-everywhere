// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.IPreferenceStore;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * Persists the last-used profile, project collection and workspace information.
 */
public class UIConnectionPersistence {
    private static final Log log = LogFactory.getLog(UIConnectionPersistence.class);

    private static final Object lock = new Object();
    private static UIConnectionPersistence instance;

    private final IPreferenceStore preferenceStore;

    private UIConnectionPersistence(final IPreferenceStore preferenceStore) {
        Check.notNull(preferenceStore, "preferenceStore"); //$NON-NLS-1$

        this.preferenceStore = preferenceStore;
    }

    /**
     * Gets the {@link UIProfilePersistence} for the current product (Eclipse
     * Plug-in or Explorer.)
     *
     * @return The {@link UIProfilePersistence} for the current product.
     */
    public static UIConnectionPersistence getInstance() {
        synchronized (lock) {
            if (instance == null) {
                final IPreferenceStore preferenceStore =
                    TFSCommonUIClientPlugin.getDefault().getProductPlugin().getPreferenceStore();

                instance = new UIConnectionPersistence(preferenceStore);
            }

            return instance;
        }
    }

    /* Last-used server URI methods */

    /**
     * Clears the last-used profile data.
     */
    public void clearLastUsedServerURI() {
        preferenceStore.setToDefault(UIPreferenceConstants.LAST_SERVER_URI);
    }

    /**
     * Sets the last-used server URI to the given URI.
     *
     * @param serverURI
     *        The {@link URI} to set as last-used (not <code>null</code>)
     */
    public void setLastUsedServerURI(final URI serverURI) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        preferenceStore.setValue(
            UIPreferenceConstants.LAST_SERVER_URI,
            URIUtils.removeTrailingSlash(serverURI).toString());
    }

    /**
     * Gets the last-used server {@link URI}
     *
     * @return The {@link URI} last connected to, or <code>null</code> if there
     *         is none.
     */
    public URI getLastUsedServerURI() {
        final String uri = preferenceStore.getString(UIPreferenceConstants.LAST_SERVER_URI);

        if (uri == null || uri.length() == 0) {
            return null;
        }

        try {
            return URIUtils.newURI(uri);
        } catch (final Exception e) {
            log.warn(MessageFormat.format("Invalid URI while retrieving last-used server: {0}", uri), e); //$NON-NLS-1$
            return null;
        }
    }

    /* Last-used project collection methods */

    public GUID getLastUsedProjectCollection(final TFSConfigurationServer configurationServer) {
        Check.notNull(configurationServer, "configurationServer"); //$NON-NLS-1$

        return getLastUsedProjectCollection(configurationServer.getBaseURI());
    }

    public GUID getLastUsedProjectCollection(final URI configurationServerURI) {
        Check.notNull(configurationServerURI, "configurationServerURI"); //$NON-NLS-1$

        final String lastCollectionKey = UIPreferenceConstants.LAST_PROJECT_COLLECTION_ID
            + UIPreferenceConstants.LAST_PROJECT_COLLECTION_SEPARATOR
            + configurationServerURI.toString();

        if (!preferenceStore.contains(lastCollectionKey)) {
            return null;
        }

        try {
            return new GUID(preferenceStore.getString(lastCollectionKey));
        } catch (final Exception e) {
            log.warn("Could not load last project collection preference", e); //$NON-NLS-1$
            return null;
        }
    }

    public void setLastUsedProjectCollection(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        /*
         * Only store the collection GUID on 2010+ servers.
         */
        if (connection.getConfigurationServer() != null) {
            setLastUsedProjectCollection(connection.getConfigurationServer().getBaseURI(), connection.getInstanceID());
            setLastUsedServerURI(connection.getConfigurationServer().getBaseURI());
        } else {
            setLastUsedServerURI(connection.getBaseURI());
        }
    }

    public void setLastUsedProjectCollection(final URI configurationServerURI, final GUID projectCollectionID) {
        Check.notNull(configurationServerURI, "configurationServerURI"); //$NON-NLS-1$
        Check.notNull(projectCollectionID, "projectCollectionID"); //$NON-NLS-1$

        final String lastCollectionKey = UIPreferenceConstants.LAST_PROJECT_COLLECTION_ID
            + UIPreferenceConstants.LAST_PROJECT_COLLECTION_SEPARATOR
            + configurationServerURI.toString();

        preferenceStore.setValue(lastCollectionKey, projectCollectionID.toString());
    }

    /* Last-used workspace methods */

    /**
     * Gets the last-used workspace for this {@link TFSTeamProjectCollection}.
     *
     * @param connection
     *        The {@link TFSTeamProjectCollection} to get the last-used
     *        workspace for (not <code>null</code>).
     * @return The workspace name or <code>null</code> if there is no last-used
     *         workspace
     */
    public String getLastUsedWorkspace(final TFSTeamProjectCollection connection) {
        return getLastUsedWorkspace(connection.getBaseURI());
    }

    /**
     * Gets the last-used workspace for the given
     * {@link TFSTeamProjectCollection} instance ID.
     *
     * @param connection
     *        The instance ID to get the last-used workspace for (not
     *        <code>null</code>).
     * @return The workspace name or <code>null</code> if there is no last-used
     *         workspace
     */
    public String getLastUsedWorkspace(final URI projectCollectionURI) {
        final String lastWorkspaceKey = UIPreferenceConstants.LAST_WORKSPACE_NAME
            + UIPreferenceConstants.LAST_WORKSPACE_SEPARATOR
            + projectCollectionURI.toString();

        if (!preferenceStore.contains(lastWorkspaceKey)) {
            return null;
        }

        return preferenceStore.getString(lastWorkspaceKey);
    }

    /**
     * Sets this as the last-used {@link Workspace} for the given workspace's
     * connection.
     *
     * @param workspace
     *        The {@link Workspace} that is last-used (not <code>null</code>).
     */
    public void setLastUsedWorkspace(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        setLastUsedWorkspace(workspace.getClient().getConnection().getBaseURI(), workspace.getName());
    }

    /**
     * Sets the given {@link Workspace} name as the last-used workspace for the
     * given {@link TFSTeamProjectCollection} instance ID.
     *
     * @param projectCollectionInstanceID
     *        The instance ID to set the last-used workspace for (not
     *        <code>null</code>)
     * @param workspaceName
     *        The workspace name that is last-used for the project collection
     *        (not <code>null</code>)
     */
    public void setLastUsedWorkspace(final URI projectCollectionURI, final String workspaceName) {
        Check.notNull(projectCollectionURI, "projectCollectionURI"); //$NON-NLS-1$
        Check.notNull(workspaceName, "workspaceName"); //$NON-NLS-1$

        final String lastWorkspaceKey = UIPreferenceConstants.LAST_WORKSPACE_NAME
            + UIPreferenceConstants.LAST_WORKSPACE_SEPARATOR
            + projectCollectionURI.toString();

        preferenceStore.setValue(lastWorkspaceKey, workspaceName);
    }

    /**
     * Clears the last-used workspace data for the given
     * {@link TFSTeamProjectCollection} instance ID.
     *
     * @param projectCollectionInstanceID
     *        The instance ID to clear the last-used workspace for (not
     *        <code>null</code>)
     */
    public void clearLastUsedWorkspace(final GUID projectCollectionInstanceID) {
        Check.notNull(projectCollectionInstanceID, "projectCollectionInstanceID"); //$NON-NLS-1$

        final String lastWorkspaceKey = UIPreferenceConstants.LAST_WORKSPACE_NAME
            + UIPreferenceConstants.LAST_WORKSPACE_SEPARATOR
            + projectCollectionInstanceID.getGUIDString();

        preferenceStore.setToDefault(lastWorkspaceKey);
    }
}
