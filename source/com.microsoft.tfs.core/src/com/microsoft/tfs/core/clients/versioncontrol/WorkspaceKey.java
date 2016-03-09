// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.net.URI;
import java.text.MessageFormat;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;

/**
 * {@link WorkspaceKey} is a subclass of {@link ConnectionKey} that adds a
 * workspace name attribute to the key. It can be used to cache workspaces or
 * workspace-related data in an environment where multiple connections are
 * anticipated. For more information, see the documentation on
 * {@link ConnectionKey}.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class WorkspaceKey {
    /**
     * The server URI (never <code>null</cod>)
     */
    private final URI serverURI;

    /**
     * The key's workspace name (never <code>null</code>).
     */
    private final String workspaceName;

    /**
     * The key's owner name (never <code>null</code>)
     */
    private final String ownerName;

    /**
     * Creates a new {@link WorkspaceKey} for the specified workspace.
     *
     * @param workspace
     *        the workspace to key (must not be <code>null</code>)
     */
    public WorkspaceKey(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        serverURI = URIUtils.removeTrailingSlash(workspace.getServerURI());
        workspaceName = workspace.getName();
        ownerName = workspace.getOwnerName();
    }

    /**
     * Creates a new {@link WorkspaceKey} for the specified cached workspace.
     *
     * @param cachedWorkspace
     *        the cached workspace to key (must not be <code>null</code>)
     */
    public WorkspaceKey(final WorkspaceInfo cachedWorkspace) {
        Check.notNull(cachedWorkspace, "cachedWorkspace"); //$NON-NLS-1$

        serverURI = URIUtils.removeTrailingSlash(cachedWorkspace.getServerURI());
        workspaceName = cachedWorkspace.getName();
        ownerName = cachedWorkspace.getOwnerName();
    }

    /**
     * @return this key's workspace name (never <code>null</code>)
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * @return this key's owner name (never <code>null</code>)
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MessageFormat.format(
            "WorkspaceKey: workspaceName=[{0}], server=[{1}]", //$NON-NLS-1$
            workspaceName,
            serverURI.toString());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final WorkspaceKey other = (WorkspaceKey) obj;

        return serverURI.equals(other.serverURI) && workspaceName.equalsIgnoreCase(other.workspaceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 17;
        hash = 37 * hash + serverURI.hashCode();
        hash = 37 * hash + workspaceName.toLowerCase().hashCode();
        hash = 37 * hash + ownerName.toLowerCase().hashCode();
        return hash;
    }
}
