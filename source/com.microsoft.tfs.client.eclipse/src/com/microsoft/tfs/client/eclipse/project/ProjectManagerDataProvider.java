// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import java.net.URI;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.core.resources.IProject;

import com.microsoft.tfs.client.common.config.CommonClientConnectionAdvisor;
import com.microsoft.tfs.client.common.connectionconflict.ConnectionConflictHandler;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.connectionconflict.EclipseConnectionConflictHandler;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.util.Check;

/**
 * This is the base class for ProjectManagerProviders: extensions which provide
 * authentication data to the ProjectManager. This allows a UI plugin to provide
 * a way to prompt users for necessary authentication data.
 *
 * This concrete base class does nothing.
 *
 * @threadsafety thread safe
 */
public class ProjectManagerDataProvider {
    private final ConnectionConflictHandler connectionConflictHandler = new EclipseConnectionConflictHandler();

    /**
     * Returns a ConnectionAdvisor to use to build HTTP connections. Subclasses
     * may override to provide platform-specific settings, for example.
     *
     * @return A ConnectionAdvisor to use, not null
     */
    public ConnectionAdvisor getConnectionAdvisor() {
        return new CommonClientConnectionAdvisor(Locale.getDefault(), TimeZone.getDefault());
    }

    /**
     * Returns the connection conflict handler, capable of determining whether
     * new connections should proceed when existing default connections (server
     * or repository) exist.
     *
     * @return The {@link ConnectionConflictHandler} for this project manager
     *         (not <code>null</code>)
     */
    public ConnectionConflictHandler getConnectionConflictHandler() {
        return connectionConflictHandler;
    }

    /**
     * Given an {@link IProject}, will prompt the user to connect to the server
     * containing an existing workspace for that project, returning the
     * {@link TFSTeamProjectCollection}.
     *
     * @param project
     *        The project that is being connected
     * @return A {@link TFSTeamProjectCollection} that (hopefully) contains a
     *         workspace containing a map to the project location.
     */
    public TFSTeamProjectCollection promptForConnection(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        return null;
    }

    /**
     * Given a null or incomplete profile, will prompt the user to complete the
     * details. Uses extension points to find a contributor who can provide
     * profile details (normally the UI Plug-in.)
     *
     * @param cachedWorkspace
     *        The CachedWorkspace with a null or incomplete profile
     * @return A set of {@link Credentials} to connect this CachedWorkspace, or
     *         null if none could be obtained or the user canceled.
     */
    public Credentials getCredentials(final WorkspaceInfo cachedWorkspace, final Credentials initialCredentials) {
        Check.notNull(cachedWorkspace, "cachedWorkspace"); //$NON-NLS-1$

        return null;
    }

    /**
     * Given an incorrect set of credentials, will prompt the user to complete
     * the details. Uses extension points to find a contributor who can provide
     * profile details (normally the UI Plug-in.)
     *
     * @param serverURI
     *        The server URI connecting to (never <code>null</code>)
     * @param failedCredentials
     *        The credentials that failed
     * @param errorMessage
     *        An error message suitable for display to the user (or
     *        <code>null</code>)
     * @return The new {@link Credentials} credentials to attempt, or
     *         <code>null</code> if the user canceled
     */
    public Credentials getCredentials(
        final URI serverURI,
        final Credentials failedCredentials,
        final String errorMessage) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        return null;
    }

    /**
     * Given a profile and a failure that occurred when connecting it, will
     * prompt the user to update the details. Uses extension points to find a
     * contributor who can provide profile details (normally the UI Plug-in.)
     *
     * @param profile
     *        The profile that failed
     * @param failure
     *        The failure that occurred
     * @return The profile to connect with, or null if the user canceled.
     * @param cachedWorkspace
     *        The CachedWorkspace with a null or incomplete profile
     * @return A Profile to connect this CachedWorkspace, or null if none could
     *         be obtained or the user canceled.
     */
    public TFSTeamProjectCollection promptForConnection(
        final URI serverURI,
        final Credentials credentials,
        final String errorMessage) {
        return null;
    }

    /**
     * Should we automatically reconnect projects ("return online") when a
     * {@link TFSRepository} is available.
     *
     * @return <code>true</code> to automatically reconnect projects,
     *         <code>false</code> otherwise
     */
    public boolean shouldReconnectProjects() {
        return false;
    }

    /**
     * Notifies the data provider that the following
     * {@link TFSTeamProjectCollection} is connected and available.
     *
     * @param connection
     *        The {@link TFSTeamProjectCollection}s that was connected (not
     *        <code>null</code>)
     */
    public void notifyConnectionEstablished(final TFSTeamProjectCollection connection) {
    }

    /**
     * Notifies the data provider that the following {@link IProject}s have been
     * automatically reconnected because a {@link TFSRepository} is available
     * for them.
     *
     * @param projects
     *        The {@link IProject}s that were connected (not <code>null</code>)
     */
    public void notifyProjectsReconnected(final TFSRepository repository, final IProject[] projects) {
    }

    /**
     * Provides users functionality to save changes before disconnecting.
     * Typically used when disconnecting projects and bringing the server
     * connection offline. Clients should implement prompt for save
     * functionality on open work item editors.
     *
     * @return true to proceed with the disconnect, false otherwise.
     */
    public boolean promptForDisconnect() {
        return true;
    }
}
