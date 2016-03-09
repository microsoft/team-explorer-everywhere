// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.MappingConflictException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.MultipleWorkspacesFoundException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.InternalWorkspaceConflictInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.LocalWorkspaceState;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.xml.DOMUtils;
import com.microsoft.tfs.util.xml.XMLException;

/**
 * This class manages a cache of workspaces and mappings that reside on the
 * local computer.
 * <p>
 * {@link InternalCacheLoader} handles serialization tasks to/from files.
 *
 * @threadsafety thread-safe
 */
public class InternalCache {
    private final static String XML_SERVER_INFO = "ServerInfo"; //$NON-NLS-1$
    private final static String XML_URI = "uri"; //$NON-NLS-1$
    private final static String XML_GUID = "repositoryGuid"; //$NON-NLS-1$

    /**
     * The mutex (shared with {@link Workstation}) used for synchronizing all
     * fields.
     */
    private final Object workstationMutex;

    /**
     * These are the active, non-deleted list of workspaces known to reside on
     * this computer for the current Windows user (owners may be different as
     * the user may authenticate using any valid identity accepted by the
     * server).
     *
     * Synchronized on {@link #workstationMutex}.
     */
    private final List<WorkspaceInfo> activeWorkspaces;

    /**
     * This is a list of workspaces that have been removed. It is used to keep
     * track of which workspaces should be deleted from the cache file on disk
     * when merging at load time.
     *
     * Synchronized on {@link #workstationMutex}.
     */
    private final List<WorkspaceInfo> removedWorkspaces;

    /**
     * Synchronized on {@link #workstationMutex}.
     */
    private boolean urlChanged;

    /**
     * Constructs an {@link InternalCache} that synchronizes on the specified
     * object. A mutex is specified here so the cache can be used exclusively by
     * {@link Workstation}.
     *
     * @param workstationMutex
     *        the object to synchronize access to internal data with (must not
     *        be <code>null</code>)
     */
    public InternalCache(final Object workstationMutex) {
        Check.notNull(workstationMutex, "workstationMutex"); //$NON-NLS-1$

        this.workstationMutex = workstationMutex;
        this.activeWorkspaces = new ArrayList<WorkspaceInfo>();
        this.removedWorkspaces = new ArrayList<WorkspaceInfo>();
    }

    /**
     * Gets the server info object that matches specified GUID or creates a new
     * one. Also updates the name of the server object if the GUID matches.
     */
    public InternalServerInfo getServerInfoByGUID(final GUID guid, final URI uri) {
        InternalServerInfo serverInfo = null;

        synchronized (workstationMutex) {
            for (final WorkspaceInfo workspace : activeWorkspaces) {
                // Update the server name if it isn't identical to what we had.
                if (workspace.getServer().getServerGUID().equals(guid)
                    && !Workspace.matchServerURI(workspace.getServer().getURI(), uri)) {
                    workspace.getServer().setURI(uri);
                    serverInfo = workspace.getServer();
                    break;
                }
            }
        }

        if (serverInfo == null) {
            serverInfo = new InternalServerInfo(uri, guid);
        }

        return serverInfo;
    }

    /**
     * Gets the server info object that matches specified uri or returns null.
     * <p>
     * NOTE: This is here to support the repository object, and it's generally
     * dangerous to get server info by uri rather than GUID.
     *
     * @param the
     *        uri of the server to look up
     * @return the server info if the uri matches, or null if there is no match
     */
    public InternalServerInfo getServerInfoByURI(final URI uri) {
        InternalServerInfo serverInfo = null;
        synchronized (workstationMutex) {
            for (final WorkspaceInfo workspace : activeWorkspaces) {
                if (Workspace.matchServerURI(workspace.getServer().getURI(), uri)) {
                    serverInfo = workspace.getServer();
                    break;
                }
            }
        }

        return serverInfo;
    }

    /**
     * Checks working folders for any conflicts with mappings already in the
     * cache. Each local path can only reside in one mapping.
     *
     * @param workspaceToCheck
     *        the workspace to check against the cache
     * @param workspaceToIgnore
     *        Optional parameter. If provided, the mappings of this workspace
     *        are ignored. (Used by UpdateWorkspace to avoid detecting mapping
     *        conflicts with itself)
     */
    public void checkForMappingConflicts(final Workspace workspaceToCheck, final Workspace workspaceToIgnore) {
        // Lock over both getServerInfoByGUID() calls
        synchronized (workstationMutex) {
            final WorkspaceInfo wiToCheck = new WorkspaceInfo(
                getServerInfoByGUID(
                    workspaceToCheck.getServerGUID(),
                    workspaceToCheck.getClient().getConnection().getBaseURI()),
                workspaceToCheck);

            WorkspaceInfo wiToIgnore = null;

            if (null != workspaceToIgnore) {
                wiToIgnore = new WorkspaceInfo(
                    getServerInfoByGUID(
                        workspaceToIgnore.getServerGUID(),
                        workspaceToIgnore.getClient().getConnection().getBaseURI()),
                    workspaceToIgnore);
            }

            checkForMappingConflicts(wiToCheck, wiToIgnore);
        }
    }

    /**
     * Checks mappings for any conflicts with mappings already in the cache.
     * Each local path can only reside in one workspace. Workspace checks for
     * working folder conflicts within its own working folder array.
     *
     * @param workspaceToCheck
     *        the workspace to check against the cache (must not be
     *        <code>null</code>)
     * @param workspaceToIgnore
     *        Optional parameter. If provided, the mappings of this workspace
     *        are ignored. (Used by UpdateWorkspace to avoid detecting mapping
     *        conflicts with itself) (may be <code>null</code>)
     * @throws MappingConflictException
     *         if there is any conflict
     */
    public void checkForMappingConflicts(final WorkspaceInfo workspaceToCheck, final WorkspaceInfo workspaceToIgnore)
        throws MappingConflictException {
        // NOTE: A workspace can appear to have "redundant" mappings (local
        // paths that are children
        // of a another mapped path) because of "map-ins" that, without the
        // server path,
        // look redundant. The same exact path should not, however, appear more
        // than once.

        for (final String newMap : workspaceToCheck.getMappedPaths()) {
            // Lock before getMapping()
            synchronized (workstationMutex) {
                // Check to see if an existing workspace contains a mapping for
                // newMap (newMap is equal
                // to or is a child of an existing mapping).
                String conflictingMap;
                if ((conflictingMap = getMapping(newMap, workspaceToIgnore)) != null) {
                    throw new MappingConflictException(
                        MessageFormat.format(
                            Messages.getString("InternalCache.ConflictingWorkingFoldersFormat"), //$NON-NLS-1$
                            newMap,
                            formatWorkspaceNameForException(workspaceToCheck, getWorkspace(conflictingMap))));
                }

                // Check for any new mapping being the parent of an existing
                // mapping.
                for (final WorkspaceInfo existingWS : activeWorkspaces) {
                    // If a workspace-to-ignore is specified, then skip it.
                    if (null != workspaceToIgnore && existingWS.equals(workspaceToIgnore)) {
                        continue;
                    }

                    for (final String existingMap : existingWS.getMappedPaths()) {
                        if (LocalPath.isChild(newMap, existingMap)) {
                            throw new MappingConflictException(
                                MessageFormat.format(
                                    Messages.getString("InternalCache.ConflictingWorkingFoldersFormat"), //$NON-NLS-1$
                                    existingMap,
                                    formatWorkspaceNameForException(workspaceToCheck, getWorkspace(existingMap))));
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes from the cache any workspaces that conflict with the workspace
     * passed in. Each local path can only reside in one workspace. Any
     * workspace with mappings that conflict with the new workspace will be
     * removed from the cache.
     *
     * The caller must have acquired {@link #workstationMutex} before calling.
     *
     * @param workspace
     *        the new workspace (must not already be in the cache)
     * @return List of conflicting workspaces that were removed
     */
    private InternalWorkspaceConflictInfo[] removeConflictingWorkspaces(final WorkspaceInfo workspace) {
        final Set<InternalWorkspaceConflictInfo> removedWorkspaces = new HashSet<InternalWorkspaceConflictInfo>();
        for (final String newMap : workspace.getMappedPaths()) {
            // Check to see if an existing workspace contains a mapping for
            // newMap (newMap is equal
            // to or is a child of an existing mapping).
            String conflictingMap;
            if ((conflictingMap = getMapping(newMap)) != null) {
                final WorkspaceInfo wi = getWorkspace(conflictingMap);
                removedWorkspaces.add(new InternalWorkspaceConflictInfo(workspace, wi, conflictingMap));
                removeWorkspace(wi);
            }

            // Check for any new mapping being the parent of an existing
            // mapping.
            final List<String> invalidWorkspacePaths = new ArrayList<String>();
            for (final WorkspaceInfo existingWS : activeWorkspaces) {
                for (final String existingMap : existingWS.getMappedPaths()) {
                    if (LocalPath.isChild(newMap, existingMap)) {
                        // We can't remove the workspace here because that would
                        // modify the collection during the enumeration.
                        invalidWorkspacePaths.add(existingMap);
                        break;
                    }
                }
            }

            // Now go through and actually remove the invalid workspaces
            for (final String invalidWorkspacePath : invalidWorkspacePaths) {
                final WorkspaceInfo wi = getWorkspace(invalidWorkspacePath);
                removedWorkspaces.add(new InternalWorkspaceConflictInfo(workspace, wi, invalidWorkspacePath));
                removeWorkspace(wi);
            }
        }

        return removedWorkspaces.toArray(new InternalWorkspaceConflictInfo[removedWorkspaces.size()]);
    }

    /**
     * Formats the conflicting workspace name for exception when there is a
     * conflict. The owner of the workspace is included when the owner is
     * different.
     *
     * @param workspace
     *        the workspace (must not be <code>null</code>)
     * @param conflictingWorkspace
     *        the workspace that conflicts (must not be <code>null</code>)
     * @return the formatted workspace name
     */
    private String formatWorkspaceNameForException(
        final WorkspaceInfo workspace,
        final WorkspaceInfo conflictingWorkspace) {
        return workspace.formatWorkspaceNameForException(conflictingWorkspace);
    }

    /**
     * Gets the mapping for the given path. The mapping returned is the actual
     * mapping, which may be for an ancestor of the path. For example, if c:\x
     * is mapped, getMapping("c:\x\y\z\a.txt") will return c:\x.
     *
     * @param path
     *        the path
     * @return the mapping for the path
     */
    public String getMapping(final String path) {
        return getMapping(path, null);
    }

    /**
     * Gets the mapping for the given path. The mapping returned is the actual
     * mapping, which may be for an ancestor of the path. For example, if c:\x
     * is mapped, getMapping("c:\x\y\z\a.txt") will return c:\x.
     *
     * @param path
     *        the path
     * @param workspaceToIgnore
     *        Optional parameter. If provided, the mappings of this
     *        {@link WorkspaceInfo} are ignored. (Used by UpdateWorkspace to
     *        avoid detecting mapping conflicts with itself)
     * @return the mapping for the path
     */
    public String getMapping(final String path, final WorkspaceInfo workspaceToIgnore) {
        synchronized (workstationMutex) {
            for (final WorkspaceInfo ws : activeWorkspaces) {
                if (null != workspaceToIgnore && ws.equals(workspaceToIgnore)) {
                    continue;
                }

                final String mappingFound = ws.getMapping(path);

                if (null != mappingFound) {
                    return mappingFound;
                }
            }
        }

        return null;
    }

    /**
     * Gets the mapping for the given path. The mapping returned is the
     * inherited mapping. For example, if c:\x is mapped,
     * getInheritedMapping("c:\x\y\z\a.txt") will return c:\x\y\z\a.txt.
     *
     * @param the
     *        path
     * @param the
     *        inherited mapping for the path
     */
    public String getInheritedMapping(final String path) {
        final String mapping = getMapping(path);

        if (mapping == null) {
            return null;
        }

        return path;
    }

    /**
     * Gets the mappings at and below the path.
     *
     * @param path
     *        the path
     * @return the mappings for the path and its children
     */
    public String[] getMappingsRecursively(final String path) {
        final List<String> list = new ArrayList<String>();

        synchronized (workstationMutex) {
            for (final WorkspaceInfo ws : activeWorkspaces) {
                for (final String map : ws.getMappedPaths()) {
                    if (LocalPath.isChild(path, map)) {
                        list.add(map);
                    }
                }
            }
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Gets the workspace for the given path.
     *
     * @param path
     *        the path
     * @return the workspace path or null
     */
    public WorkspaceInfo getWorkspace(final String path) {
        synchronized (workstationMutex) {
            for (final WorkspaceInfo ws : activeWorkspaces) {
                if (ws.getMapping(path) != null) {
                    return ws;
                }
            }
        }

        return null;
    }

    /**
     * Gets the workspace in the cache that matches the repository GUID,
     * workspace name, and workspace owner.
     *
     * @param repositoryGuid
     *        the repository Guid
     * @param name
     *        the workspace name
     * @param owner
     *        the workspace owner
     * @return the matching local workspace or null if none matches
     */
    public WorkspaceInfo getWorkspace(final GUID repositoryGuid, final String name, final String owner) {
        Check.isTrue(
            VersionControlConstants.AUTHENTICATED_USER.equals(owner) == false,
            "owner must not be VersionControlConstants.AUTHENTICATED_USER"); //$NON-NLS-1$

        final List<WorkspaceInfo> matchingWorkspaces = new ArrayList<WorkspaceInfo>();
        synchronized (workstationMutex) {
            for (final WorkspaceInfo ws : activeWorkspaces) {
                if (repositoryGuid.equals(ws.getServerGUID())
                    && Workspace.matchName(name, ws.getName())
                    && ws.ownerNameMatches(owner)) {
                    matchingWorkspaces.add(ws);
                }
            }
        }

        if (matchingWorkspaces.size() == 0) {
            return null;
        }

        if (matchingWorkspaces.size() > 1) {
            final List<String> specs = new ArrayList<String>(matchingWorkspaces.size());
            for (final WorkspaceInfo wsInfo : matchingWorkspaces) {
                specs.add(wsInfo.getQualifiedName());
            }

            throw new MultipleWorkspacesFoundException(name, owner, specs);
        }

        return matchingWorkspaces.get(0);
    }

    /**
     * Gets all workspaces in the cache.
     *
     * @return all workspaces in the cache
     */
    public WorkspaceInfo[] getAllWorkspaces() {
        synchronized (workstationMutex) {
            return activeWorkspaces.toArray(new WorkspaceInfo[activeWorkspaces.size()]);
        }
    }

    /**
     * Load the cache file.
     *
     * @param config
     *        the XML node containing the cache configuration
     */
    public void load(final Element config) {
        final InternalCache newCache = load(config, true);

        synchronized (workstationMutex) {
            urlChanged = false;

            activeWorkspaces.clear();
            removedWorkspaces.clear();

            activeWorkspaces.addAll(newCache.activeWorkspaces);
        }
    }

    /**
     * Load the cache file. A sample of the expected XML is:
     *
     * <pre>
     *        &lt;ServerInfo uri=&quot;http://server:8080/tfs/collection&quot; guid=&quot;ba99f1f5-d1fa-4029-8621-28b72ed1b4fb&quot;&gt;
     *          &lt;WorkspaceInfo name=&quot;NEWELL&quot; owner=&quot;domainuser&quot; computer=&quot;machinename&quot; comment=&quot;&quot;&gt;
     *            &lt;MappedPath path=&quot;C:\bin\test&quot; /&gt;
     *          &lt;/WorkspaceInfo&gt;
     *        &lt;/ServerInfo&gt;
     * </pre>
     *
     * @param config
     *        the XML node containing the cache configuration (must not be
     *        <code>null</code>)
     * @param workstationMutex
     *        the synchronization object for the new instance to use (see
     *        {@link #InternalCache(Object)}) (must not be <code>null</code>)
     * @returns a cache
     */
    private static InternalCache load(final Element config, final Object workstationMutex) {
        Check.notNull(config, "config"); //$NON-NLS-1$
        Check.notNull(workstationMutex, "workstationMutex"); //$NON-NLS-1$

        final InternalCache newCache = new InternalCache(workstationMutex);

        for (final Element serverNode : DOMUtils.getChildElements(config)) {
            final Node guidAttr = serverNode.getAttributes().getNamedItem(XML_GUID);
            final GUID guid = new GUID(guidAttr.getNodeValue());

            /*
             * This URI is already encoded if it contains non-Latin characters,
             * so use the constructor that does not encode special chars (it
             * would double-encode percent signs).
             */
            final Node serverUriAttr = serverNode.getAttributes().getNamedItem(XML_URI);
            URI serverUri;
            try {
                serverUri = new URI(serverUriAttr.getNodeValue());
            } catch (final URISyntaxException e) {
                throw new XMLException(Messages.getString("InternalCache.CouldNotReadServerURI"), e); //$NON-NLS-1$
            }

            final InternalServerInfo serverInfo = new InternalServerInfo(serverUri, guid);

            // Read each repository record.
            for (final Element workspaceNode : DOMUtils.getChildElements(serverNode)) {
                final WorkspaceInfo workspace = WorkspaceInfo.loadFromXML(serverInfo, workspaceNode);

                // If for some reason the workspace already exists (e.g.,
                // somebody merged two repository computers into one), skip it.
                if (newCache.getWorkspace(
                    serverInfo.getServerGUID(),
                    workspace.getName(),
                    workspace.getOwnerName()) != null) {
                    continue;
                }

                synchronized (workstationMutex) {
                    newCache.activeWorkspaces.add(workspace);
                }
            }
        }

        synchronized (workstationMutex) {
            newCache.urlChanged = false;
        }

        return newCache;
    }

    /**
     * Save the working folder data under the specified node in the specified
     * document. We don't clear the dirty flag here because the caller could
     * fail to write the data to disk. The caller should clear the dirty flag
     * once the save has been successful. See Load for some sample XML.
     *
     * @param inputXml
     *        the XML local mappings root node for the existing cache file (may
     *        be <code>null</code> if there are none)
     * @param outputXml
     *        the root node for the local cache (must not be <code>null</code>)
     * @param removedConflictingWorkspaces
     *        array of workspaces that were conflict and were removed (must not
     *        be <code>null</code>)
     */
    public void save(
        final Element inputXml,
        final Element outputXml,
        final AtomicReference<InternalWorkspaceConflictInfo[]> removedConflictingWorkspaces) {
        synchronized (workstationMutex) {
            removedConflictingWorkspaces.set(InternalWorkspaceConflictInfo.EMPTY_ARRAY);

            // If there is an existing cache file, merge the current cache and
            // the cache file.
            if (inputXml != null) {
                merge(inputXml, removedConflictingWorkspaces);
            }

            // No need to continue if there are no workspaces. But do continue
            // when not dirty since
            // this may be called due to attempted checkin info being updated.
            if (activeWorkspaces.size() == 0) {
                return;
            }

            // Sort them so that the workspaces for each repository are
            // contiguous.
            Collections.sort(activeWorkspaces);

            InternalServerInfo lastServer = null;
            Element serverNode = null;
            for (final WorkspaceInfo ws : activeWorkspaces) {
                if (lastServer == null || !ws.getServerGUID().equals(lastServer.getServerGUID())) {
                    serverNode = addRepositoryXMLNode(outputXml, ws.getServer());
                    lastServer = ws.getServer();
                }

                ws.saveAsXML(serverNode);
            }
        }

        // Caller will need to mark it as clean.
    }

    /**
     * Adds a server entry to XML representation of the cache.
     *
     * @param outputXml
     *        the root node for the local cache
     * @param server
     *        the server
     * @return the XML node for the server
     */
    private Element addRepositoryXMLNode(final Element outputXml, final InternalServerInfo server) {
        // Create the server node.
        final Element serverNode = DOMUtils.appendChild(outputXml, XML_SERVER_INFO);

        // Strip trailing slash (unless path is simply "/") for VS compat
        serverNode.setAttribute(XML_URI, URIUtils.removeTrailingSlash(server.getURI()).toString());
        serverNode.setAttribute(XML_GUID, server.getServerGUID().toString());

        return serverNode;
    }

    /**
     * Inserts a repository workspace into the local cache.
     *
     * @param the
     *        workspace to insert
     * @param removedConflictingWorkspaces
     *        array of workspaces that were conflict and were removed
     * @return the {@link WorkspaceInfo} inserted into the cache
     */
    public WorkspaceInfo insertWorkspace(
        final Workspace workspace,
        final AtomicReference<InternalWorkspaceConflictInfo[]> removedConflictingWorkspaces) {
        final InternalServerInfo serverInfo =
            getServerInfoByGUID(workspace.getServerGUID(), workspace.getClient().getConnection().getBaseURI());
        return insertWorkspace(new WorkspaceInfo(serverInfo, workspace), removedConflictingWorkspaces);
    }

    /**
     * Inserts a local workspace into the local cache. NOTE: The workspace must
     * have already been checked for conflicts!
     *
     * @param workspace
     *        the local workspace to insert
     * @param removedConflictingWorkspaces
     *        array of workspaces that were conflicting and were removed from
     *        the cache, empty array if there were no conflicts
     * @return the WorkspaceInfo inserted into the cache
     */
    public WorkspaceInfo insertWorkspace(
        final WorkspaceInfo workspace,
        final AtomicReference<InternalWorkspaceConflictInfo[]> removedConflictingWorkspaces) {
        Check.isTrue(
            Workspace.matchComputer(workspace.getComputer(), LocalHost.getShortName()),
            MessageFormat.format("Workspace is on a different computer: {0}", workspace.getName())); //$NON-NLS-1$
        Check.isTrue(
            VersionControlConstants.AUTHENTICATED_USER.equals(workspace.getOwnerName()) == false,
            "workspace.OwnerName must not be VersionControlConstants.AUTHENTICATED_USER"); //$NON-NLS-1$

        synchronized (workstationMutex) {
            checkRedundantWorkspaceName(workspace.getServer(), workspace.getName(), workspace.getOwnerName());

            removedConflictingWorkspaces.set(removeConflictingWorkspaces(workspace));

            activeWorkspaces.add(workspace);
        }

        return workspace;
    }

    /**
     * Debug code to verify that a workspace name is not already in the cache.
     */
    private void checkRedundantWorkspaceName(
        final InternalServerInfo server,
        final String name,
        final String ownerUniqueName) {
        final WorkspaceInfo ws = getWorkspace(server.getServerGUID(), name, ownerUniqueName);

        Check.isTrue(
            ws == null,
            MessageFormat.format(
                "There is already a workspace with the same repository, owner, and name: \n Original{0} Requested: {1} name: {2} owner: {3}", //$NON-NLS-1$
                ws,
                server.getServerGUID(),
                name,
                ownerUniqueName));
    }

    /**
     * Removes the workspace from the local cache using the repository,
     * workspace name, and workspace owner.
     *
     * @param workspace
     *        the workspace to remove
     *
     */
    public void removeWorkspace(final WorkspaceInfo workspace) {
        synchronized (workstationMutex) {
            final WorkspaceInfo ws =
                getWorkspace(workspace.getServerGUID(), workspace.getName(), workspace.getOwnerName());
            Check.notNull(ws, MessageFormat.format("workspace: {0}", workspace.getName())); //$NON-NLS-1$
            activeWorkspaces.remove(ws);
            ws.setState(LocalWorkspaceState.REMOVED);
            removedWorkspaces.add(ws);
        }
    }

    /**
     * Sets up the internal cache state such that the cache is clean. IMPORTANT:
     * The caller must have the lock on Workstation.mutex.
     */
    public void markClean() {
        synchronized (workstationMutex) {
            // Clear the flag that indicates a repository URL changed.
            urlChanged = false;

            // Update the state on all workspaces to clean and clear the removed
            // list.
            for (final WorkspaceInfo ws : activeWorkspaces) {
                ws.markClean();
            }

            removedWorkspaces.clear();
        }
    }

    /**
     * Intelligently merges an existing cache file's contents (input) with the
     * current in-memory cache.
     *
     * @param xmlOnDisk
     *        the contents of the cache file on disk\
     */
    public void merge(
        final Element xmlOnDisk,
        final AtomicReference<InternalWorkspaceConflictInfo[]> removedConflictingWorkspaces) {
        final InternalCache cacheOnDisk = load(xmlOnDisk, false);
        final Set<InternalWorkspaceConflictInfo> removedWorkspacesSet = new HashSet<InternalWorkspaceConflictInfo>();

        synchronized (workstationMutex) {
            // For each workspace in the removed list, remove it from the cache
            // loaded from disk.
            for (final WorkspaceInfo ws : removedWorkspaces) {
                final WorkspaceInfo wsToRemove =
                    cacheOnDisk.getWorkspace(ws.getServerGUID(), ws.getName(), ws.getOwnerName());
                if (wsToRemove != null) {
                    cacheOnDisk.removeWorkspace(wsToRemove);
                }
            }

            // Remove each clean active workspace not in the cache loaded from
            // disk.
            for (int i = 0; i < activeWorkspaces.size(); i++) {
                final WorkspaceInfo ws = activeWorkspaces.get(i);
                if (ws.getState() == LocalWorkspaceState.CLEAN) {
                    if (cacheOnDisk.getWorkspace(ws.getServerGUID(), ws.getName(), ws.getOwnerName()) == null) {
                        activeWorkspaces.remove(ws);

                        // Back up since we removed one (we're using a for loop
                        // since we can't
                        // remove elements from the list in a foreach).
                        i--;
                    }
                }
            }

            // Update clean active workspaces that are also in the cache loaded
            // from disk.
            for (final WorkspaceInfo ws : activeWorkspaces) {
                if (ws.getState() == LocalWorkspaceState.CLEAN) {
                    final WorkspaceInfo newerWs =
                        cacheOnDisk.getWorkspace(ws.getServerGUID(), ws.getName(), ws.getOwnerName());
                    if (newerWs != null) {
                        ws.update(newerWs, true);
                    }
                }
            }

            // Add each workspace that is in the cache on disk but not in the
            // active workspace list.
            for (final WorkspaceInfo ws : cacheOnDisk.activeWorkspaces) {
                if (getWorkspace(ws.getServerGUID(), ws.getName(), ws.getOwnerName()) == null) {
                    try {
                        final AtomicReference<InternalWorkspaceConflictInfo[]> removedWorkspaces =
                            new AtomicReference<InternalWorkspaceConflictInfo[]>();
                        insertWorkspace(ws, removedWorkspaces);
                        for (final InternalWorkspaceConflictInfo wi : removedWorkspaces.get()) {
                            removedWorkspacesSet.add(wi);
                        }
                    } catch (final MappingConflictException e) {
                        // Just skip the workspace since it conflicts. In this
                        // case, the last one
                        // to write the cache file wins.
                    }

                    Check.isTrue(ws.getState() == LocalWorkspaceState.CLEAN, "State is not clean: " + ws.getState()); //$NON-NLS-1$
                }
            }
        }
        removedConflictingWorkspaces.set(
            removedWorkspacesSet.toArray(new InternalWorkspaceConflictInfo[removedWorkspacesSet.size()]));
    }

    /**
     * Internal method to update the name of a server when a user accesses a
     * known server through a URI different than a previous one that was cached.
     *
     * @param repositoryGuid
     *        the GUID for the repository on the server
     * @param uri
     *        the URI of the server
     */
    public void updateServerURI(final GUID repositoryGuid, final URI uri) {
        synchronized (workstationMutex) {
            for (final WorkspaceInfo workspaceInfo : activeWorkspaces) {
                if (workspaceInfo.getServerGUID().equals(repositoryGuid)
                    && !Workspace.matchServerURI(uri, workspaceInfo.getServerURI())) {
                    workspaceInfo.getServer().setURI(uri);
                    urlChanged = true;
                }
            }
        }
    }

    public int getWorkspaceCount() {
        synchronized (workstationMutex) {
            return activeWorkspaces.size();
        }
    }

    public boolean isDirty() {
        synchronized (workstationMutex) {
            if (urlChanged) {
                return true;
            }

            // The cache is dirty if any workspaces have been removed.
            if (removedWorkspaces.size() > 0) {
                return true;
            }

            // The cache is dirty if any workspaces have been added or modified.
            for (final WorkspaceInfo ws : activeWorkspaces) {
                if (ws.getState() != LocalWorkspaceState.CLEAN) {
                    return true;
                }
            }
        }

        return false;
    }
}
