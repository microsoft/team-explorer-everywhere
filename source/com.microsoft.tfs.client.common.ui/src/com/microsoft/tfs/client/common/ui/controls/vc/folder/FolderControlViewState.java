// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.folder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.ui.IMemento;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class FolderControlViewState {
    private static final String FOLDER_CONTROL_CHILD = "folder-control"; //$NON-NLS-1$
    private static final String SERVER_CHILDREN = "server"; //$NON-NLS-1$
    private static final String USER_CHILDREN = "user"; //$NON-NLS-1$
    private static final String WORKSPACE_CHILDREN = "workspace"; //$NON-NLS-1$
    private static final String EXPANDED_CHILDREN = "expanded"; //$NON-NLS-1$
    private static final String SELECTED_CHILD = "selected"; //$NON-NLS-1$
    private static final String PATH_KEY = "path"; //$NON-NLS-1$

    private final Map servers = new HashMap();

    private VSWorkspace getCurrentWorkspace(final boolean create) {
        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

        if (repository == null) {
            return null;
        }

        final Workspace aWorkspace = repository.getWorkspace();
        final String serverGUID = aWorkspace.getClient().getServerGUID().getGUIDString();
        final String userName = aWorkspace.getOwnerName();
        final String workspaceName = aWorkspace.getName();

        VSServer server = (VSServer) servers.get(serverGUID);
        if (server == null) {
            if (!create) {
                return null;
            }
            server = new VSServer(serverGUID);
            servers.put(serverGUID, server);
        }

        VSUser user = (VSUser) server.users.get(userName);
        if (user == null) {
            if (!create) {
                return null;
            }
            user = new VSUser(userName);
            server.users.put(userName, user);
        }

        VSWorkspace workspace = (VSWorkspace) user.workspaces.get(workspaceName);
        if (workspace == null && create) {
            workspace = new VSWorkspace(workspaceName);
            user.workspaces.put(workspaceName, workspace);
        }

        return workspace;
    }

    public void setCurrentlySelectedItem(final TFSItem item) {
        final VSWorkspace workspace = getCurrentWorkspace(true);
        if (workspace != null) {
            workspace.selectedPath = (item != null ? item.getFullPath() : null);
        }
    }

    public void setCurrentlyExpandedPaths(final Object[] expandedElements) {
        final VSWorkspace workspace = getCurrentWorkspace(true);
        if (workspace != null) {
            final Set paths = new HashSet();
            if (expandedElements != null) {
                for (int i = 0; i < expandedElements.length; i++) {
                    paths.add(((TFSItem) expandedElements[i]).getFullPath());
                }
            }
            workspace.expandedPaths = paths;
        }
    }

    public ServerItemPath getPreviouslySelectedPath() {
        final VSWorkspace workspace = getCurrentWorkspace(false);
        if (workspace != null && workspace.selectedPath != null) {
            return new ServerItemPath(workspace.selectedPath);
        }
        return null;
    }

    public ServerItemPath[] getPreviouslyExpandedPaths() {
        final VSWorkspace workspace = getCurrentWorkspace(false);
        if (workspace != null) {
            final ServerItemPath[] paths = new ServerItemPath[workspace.expandedPaths.size()];
            int ix = 0;
            for (final Iterator it = workspace.expandedPaths.iterator(); it.hasNext();) {
                paths[ix++] = new ServerItemPath((String) it.next());
            }
            return paths;
        }
        return null;
    }

    public void populateWithStateToSave(final IMemento memento) {
        if (servers.size() == 0) {
            return;
        }

        final IMemento folderControlMemento = memento.createChild(FOLDER_CONTROL_CHILD);

        for (final Iterator it = servers.keySet().iterator(); it.hasNext();) {
            final String serverGUID = (String) it.next();
            final VSServer server = (VSServer) servers.get(serverGUID);
            final IMemento serverMemento = folderControlMemento.createChild(SERVER_CHILDREN, serverGUID);
            populateWithStateToSave(server, serverMemento);
        }
    }

    private void populateWithStateToSave(final VSServer server, final IMemento memento) {
        for (final Iterator it = server.users.keySet().iterator(); it.hasNext();) {
            final String username = (String) it.next();
            final VSUser user = (VSUser) server.users.get(username);
            final IMemento userMemento = memento.createChild(USER_CHILDREN, username);
            populateWithStateToSave(user, userMemento);
        }
    }

    private void populateWithStateToSave(final VSUser user, final IMemento memento) {
        for (final Iterator it = user.workspaces.keySet().iterator(); it.hasNext();) {
            final String workspaceName = (String) it.next();
            final VSWorkspace workspace = (VSWorkspace) user.workspaces.get(workspaceName);
            final IMemento workspaceMemento = memento.createChild(WORKSPACE_CHILDREN, workspace.name);

            for (final Iterator expandedPathsIt = workspace.expandedPaths.iterator(); expandedPathsIt.hasNext();) {
                final String path = (String) expandedPathsIt.next();
                final IMemento expandedMemento = workspaceMemento.createChild(EXPANDED_CHILDREN);
                expandedMemento.putString(PATH_KEY, path);
            }

            if (workspace.selectedPath != null) {
                final IMemento selectedMemento = workspaceMemento.createChild(SELECTED_CHILD);
                selectedMemento.putString(PATH_KEY, workspace.selectedPath);
            }
        }
    }

    public void setSavedState(final IMemento memento) {
        servers.clear();

        if (memento == null) {
            return;
        }

        final IMemento folderControlMemento = memento.getChild(FOLDER_CONTROL_CHILD);
        if (folderControlMemento == null) {
            return;
        }

        final IMemento[] serverMementos = folderControlMemento.getChildren(SERVER_CHILDREN);
        if (serverMementos != null) {
            for (int i = 0; i < serverMementos.length; i++) {
                if (serverMementos[i].getID() != null) {
                    final VSServer server = new VSServer(serverMementos[i].getID());
                    setSavedState(server, serverMementos[i]);
                    servers.put(server.guid, server);
                }
            }
        }
    }

    private void setSavedState(final VSServer server, final IMemento memento) {
        final IMemento[] userMementos = memento.getChildren(USER_CHILDREN);
        if (userMementos != null) {
            for (int i = 0; i < userMementos.length; i++) {
                if (userMementos[i].getID() != null) {
                    final VSUser user = new VSUser(userMementos[i].getID());
                    setSavedState(user, userMementos[i]);
                    server.users.put(user.username, user);
                }
            }
        }
    }

    private void setSavedState(final VSUser user, final IMemento memento) {
        final IMemento[] workspaceMementos = memento.getChildren(WORKSPACE_CHILDREN);
        if (workspaceMementos != null) {
            for (int i = 0; i < workspaceMementos.length; i++) {
                if (workspaceMementos[i].getID() != null) {
                    final VSWorkspace workspace = new VSWorkspace(workspaceMementos[i].getID());
                    final IMemento[] expandedMementos = workspaceMementos[i].getChildren(EXPANDED_CHILDREN);
                    if (expandedMementos != null) {
                        restoreExpanded(workspace, expandedMementos);
                    }
                    final IMemento selectedMemento = workspaceMementos[i].getChild(SELECTED_CHILD);
                    if (selectedMemento != null) {
                        restoreSelected(workspace, selectedMemento);
                    }
                    user.workspaces.put(workspace.name, workspace);
                }
            }
        }
    }

    private void restoreSelected(final VSWorkspace workspace, final IMemento memento) {
        workspace.selectedPath = memento.getString(PATH_KEY);
    }

    private void restoreExpanded(final VSWorkspace workspace, final IMemento[] mementos) {
        for (int i = 0; i < mementos.length; i++) {
            final String path = mementos[i].getString(PATH_KEY);
            if (path != null) {
                workspace.expandedPaths.add(path);
            }
        }
    }

    private static class VSServer {
        public String guid;
        public Map users = new HashMap();

        public VSServer(final String guid) {
            this.guid = guid;
        }
    }

    private static class VSUser {
        public String username;
        public Map workspaces = new HashMap();

        public VSUser(final String username) {
            this.username = username;
        }
    }

    private static class VSWorkspace {
        public String name;
        public String selectedPath;
        public Set expandedPaths = new HashSet();

        public VSWorkspace(final String name) {
            this.name = name;
        }
    }
}
