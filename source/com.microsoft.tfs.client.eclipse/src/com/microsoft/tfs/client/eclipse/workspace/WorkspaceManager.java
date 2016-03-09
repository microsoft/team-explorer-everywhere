// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.workspace;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.util.Check;

public class WorkspaceManager {
    private final Object lock = new Object();

    private final Map connectionMap = new HashMap();

    private final Map workspaceMap = new HashMap();
    private final Map projectMap = new HashMap();

    private Workspace defaultWorkspace = null;

    public WorkspaceManager() {
    }

    public void addWorkspaces(final TFSConnection connection, final Workspace[] workspaces) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(workspaces, "workspaces"); //$NON-NLS-1$

        synchronized (lock) {
            if (connectionMap.containsKey(connection)) {
                connectionMap.remove(connection);
            }

            connectionMap.put(connection, workspaces);

            for (int i = 0; i < workspaces.length; i++) {
                workspaceMap.put(workspaces[i], new ArrayList());
            }

            if (defaultWorkspace == null && workspaces.length > 0) {
                defaultWorkspace = workspaces[0];
            }
        }
    }

    public Workspace getDefaultWorkspace() {
        synchronized (lock) {
            return defaultWorkspace;
        }
    }

    public Workspace[] getWorkspaces() {
        synchronized (lock) {
            final Set workspaceSet = workspaceMap.keySet();
            return (Workspace[]) workspaceSet.toArray(new Workspace[workspaceSet.size()]);
        }
    }

    /**
     * @warning This does not "realize" cached workspaces that aren't already
     *          configured. This will NOT attempt to connect a cached workspace.
     *
     * @param workspace
     * @return
     */
    public Workspace getWorkspace(final WorkspaceInfo workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        return getWorkspace(
            workspace.getServerURI(),
            workspace.getName(),
            workspace.getComputer(),
            workspace.getOwnerName());
    }

    public Workspace getWorkspace(final URI serverUri, final String name, final String computer, final String owner) {
        Check.notNull(serverUri, "serverUri"); //$NON-NLS-1$
        Check.notNull(name, "name"); //$NON-NLS-1$
        Check.notNull(computer, "computer"); //$NON-NLS-1$
        Check.notNull(owner, "owner"); //$NON-NLS-1$

        synchronized (lock) {
            for (final Iterator i = workspaceMap.keySet().iterator(); i.hasNext();) {
                final Workspace workspace = (Workspace) i.next();

                if (serverUri.equals(workspace.getServerURI())
                    && name.equals(workspace.getName())
                    && computer.equals(workspace.getComputer())
                    && owner.equals(workspace.getOwnerName())) {
                    return workspace;
                }
            }
        }

        return null;
    }

    public Workspace getWorkspace(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        synchronized (lock) {
            if (projectMap.containsKey(project)) {
                return (Workspace) projectMap.get(project);
            }

            final WorkspaceInfo cachedWorkspace =
                Workstation.getCurrent(DefaultPersistenceStoreProvider.INSTANCE).getLocalWorkspaceInfo(
                    project.getLocation().toOSString());

            if (cachedWorkspace != null) {
                return getWorkspace(cachedWorkspace);
            }

            return null;
        }
    }

    public Workspace getWorkspace(final IResource resource) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        return getWorkspace(resource.getProject());
    }
}