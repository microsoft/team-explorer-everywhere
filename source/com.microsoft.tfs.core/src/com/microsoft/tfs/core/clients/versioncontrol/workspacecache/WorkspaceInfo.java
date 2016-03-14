// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.InternalServerInfo;
import com.microsoft.tfs.core.ws.runtime.xml.XMLConvert;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.xml.DOMUtils;

/**
 * <p>
 * Holds the properties of a cached workspace.
 * </p>
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-11.0
 */
public final class WorkspaceInfo implements Comparable<WorkspaceInfo> {
    private static final Log log = LogFactory.getLog(WorkspaceInfo.class);

    // These XML element and attribute names must be kept compatible with the VS
    // implementation.

    private static final String XML_WORKSPACE_INFO = "WorkspaceInfo"; //$NON-NLS-1$
    private static final String XML_NAME = "name"; //$NON-NLS-1$
    private static final String XML_OWNER_NAME = "ownerName"; //$NON-NLS-1$
    private static final String XML_OWNER_DISPLAY_NAME = "ownerDisplayName"; //$NON-NLS-1$
    private static final String XML_OWNER_ALIASES = "OwnerAliases"; //$NON-NLS-1$
    private static final String XML_OWNER_ALIAS = "OwnerAlias"; //$NON-NLS-1$
    private static final String XML_COMPUTER = "computer"; //$NON-NLS-1$
    private static final String XML_COMMENT = "comment"; //$NON-NLS-1$
    private static final String XML_IS_LOCAL_WORKSPACE = "isLocalWorkspace"; //$NON-NLS-1$
    private static final String XML_MAPPED_PATHS = "MappedPaths"; //$NON-NLS-1$
    private static final String XML_MAPPED_PATH = "MappedPath"; //$NON-NLS-1$
    private static final String XML_WORKING_FOLDERS = "WorkingFolders"; //$NON-NLS-1$
    private static final String XML_MAP = "Map"; //$NON-NLS-1$
    private static final String XML_PATH = "path"; //$NON-NLS-1$
    private static final String XML_LOCAL_PATH = "local"; //$NON-NLS-1$
    private static final String XML_SECURITY_TOKEN = "securityToken"; //$NON-NLS-1$
    private static final String XML_LAST_SAVED_CHECKIN = "LastSavedCheckin"; //$NON-NLS-1$
    private static final String XML_LAST_SAVED_CHECKIN_TIME_STAMP = "LastSavedCheckinTimeStamp"; //$NON-NLS-1$
    private static final String OPTIONS_NAME = "options"; //$NON-NLS-1$

    private InternalServerInfo serverInfo;
    private String name;
    private String ownerName;
    private String ownerDisplayName;
    private String computer;
    private String[] ownerAliases;
    private String comment;
    private boolean isLocalWorkspace;
    private String securityToken;
    private String[] mappedPaths;
    private WorkspaceOptions options;
    private LocalWorkspaceState state;

    // The following are local metadata maintained in the cache. See
    // copyLocalMetadata().

    private SavedCheckin lastSavedCheckin;
    private Calendar lastSavedCheckinTimeStamp;

    public WorkspaceInfo(final InternalServerInfo serverInfo, final Workspace workspace) {
        Check.notNull(serverInfo, "serverInfo"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.serverInfo = serverInfo;
        this.comment = workspace.getComment();
        this.computer = workspace.getComputer();
        this.isLocalWorkspace = (workspace.getLocation() == WorkspaceLocation.LOCAL);
        this.name = workspace.getName();
        this.ownerName = workspace.getOwnerName();
        this.ownerDisplayName = workspace.getOwnerDisplayName();
        this.securityToken = workspace.getSecurityToken();
        this.mappedPaths = WorkingFolder.extractMappedPaths(workspace.getFolders());
        this.options = workspace.getOptions();
        this.ownerAliases = workspace.getOwnerAliases() != null ? workspace.getOwnerAliases().clone() : new String[0];

        this.state = LocalWorkspaceState.NEW;

        lastSavedCheckinTimeStamp = Calendar.getInstance(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
        lastSavedCheckinTimeStamp.set(1, Calendar.JANUARY, 1, 0, 0, 0);
        lastSavedCheckinTimeStamp.set(Calendar.MILLISECOND, 0);

        Check.isTrue(
            this.name.equals(this.ownerName) == false,
            MessageFormat.format("Something went wrong since name and owner are the same: {0}", this.toString())); //$NON-NLS-1$
    }

    /**
     * Used in loading info from the cache.
     */
    private WorkspaceInfo() {
    }

    /**
     * Get the workspace object that matches this {@link CachedWorkspace}.
     *
     * @param collection
     *        the team project collection to use (must not be <code>null</code>)
     * @return the workspace or <code>null</code> if it was not found on the
     *         server
     */
    public Workspace getWorkspace(final TFSTeamProjectCollection collection) {
        Check.notNull(collection, "collection"); //$NON-NLS-1$

        // Use the internal client method to return a workspace object without
        // going to the server. There's no need for the round-trip at this
        // point.
        return collection.getVersionControlClient().getLocalWorkspace(getName(), getOwnerName());
    }

    /**
     * This method is for LocalCache to use. Gets the mapping for the given
     * path. The mapping returned is the actual mapping, which may be for an
     * ancestor of the path. For example, if c:\x is mapped,
     * GetMapping("c:\x\y\z\a.txt") will return c:\x.
     *
     * @param path
     *        the path
     * @return the mapping for the path
     */
    public String getMapping(final String path) {
        int length = 0;
        String mappingFound = null;

        /*
         * Find the longest mapping that is equal to or is a parent of path.
         * This is necessary to be able to determine if the path is explicitly
         * mapped, making it a root mapping, since server paths may be mapped to
         * local paths and cut out intermediate directories.
         */
        for (final String map : getMappedPaths()) {
            if (LocalPath.isChild(map, path) && map.length() > length) {
                length = map.length();
                mappingFound = map;
            }
        }

        return mappingFound;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns true if the {@link WorkspaceInfo} objects are equal according to
     * workspace name, owner, and repository.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof WorkspaceInfo == false) {
            return false;
        }

        final WorkspaceInfo other = (WorkspaceInfo) obj;

        final boolean isEqual = Workspace.matchName(getName(), other.getName())
            && Workspace.matchOwner(getOwnerName(), other.getOwnerName())
            && serverInfo.getServerGUID().equals(other.serverInfo.getServerGUID());

        Check.isTrue(
            !isEqual || hashCode() == other.hashCode(),
            "WorkspaceInfo is equal to another, but the hashcodes didn't match."); //$NON-NLS-1$

        return isEqual;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Gets the hash code for this instance (computed from name, owner, and
     * repository guid).
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + (Workspace.hashName(getName()));
        result = result * 37 + (Workspace.hashOwner(getOwnerName()));
        result = result * 37 + (serverInfo.getServerGUID().hashCode());

        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares two workspace info objects, first comparing the server names,
     * then comparing the workspace names, and finally comparing owners.
     */
    @Override
    public int compareTo(final WorkspaceInfo other) {
        if (other == null) {
            return 1;
        }

        int result = 0;

        // Server URI

        result = Workspace.compareServerURI(getServerURI(), other.getServerURI());

        if (result != 0) {
            return result;
        }

        // Workspace name

        result = Workspace.compareName(getName(), other.getName());

        if (result != 0) {
            return result;
        }

        // Owner name

        return Workspace.compareOwner(getOwnerName(), other.getOwnerName());
    }

    /**
     * Creates an instance from the XML representation used in the cache file.
     *
     * @param serverINfo
     *        the workspace's host server
     * @param workspaceInfoNode
     *        the {@value #XML_WORKSPACE_INFO} node to load from
     * @return an instance of a {@link WorkspaceInfo} created from the XML
     */
    public static WorkspaceInfo loadFromXML(final InternalServerInfo serverInfo, final Element workspaceInfoNode) {
        final WorkspaceInfo workspaceInfo = new WorkspaceInfo();
        workspaceInfo.serverInfo = serverInfo;

        final NamedNodeMap attributes = workspaceInfoNode.getAttributes();
        Check.notNull(attributes, "attributes"); //$NON-NLS-1$

        workspaceInfo.name = getStringValue(attributes.getNamedItem(XML_NAME));
        workspaceInfo.ownerName = getStringValue(attributes.getNamedItem(XML_OWNER_NAME));
        workspaceInfo.ownerDisplayName = getStringValue(attributes.getNamedItem(XML_OWNER_DISPLAY_NAME));

        // The "ownerDisplayName" attribute was absent for some users.
        // Expecting it to always exist was causing a NullPointerException.
        // Now, use the "ownerName" if the "ownerDisplayName" is missing.
        if (StringUtil.isNullOrEmpty(workspaceInfo.ownerDisplayName)) {
            workspaceInfo.ownerDisplayName = workspaceInfo.getOwnerName();
        }

        workspaceInfo.computer = getStringValue(attributes.getNamedItem(XML_COMPUTER));
        workspaceInfo.comment = getStringValue(attributes.getNamedItem(XML_COMMENT));
        workspaceInfo.securityToken = getStringValue(attributes.getNamedItem(XML_SECURITY_TOKEN), null);
        workspaceInfo.isLocalWorkspace = getBooleanValue(attributes.getNamedItem(XML_IS_LOCAL_WORKSPACE));
        workspaceInfo.lastSavedCheckinTimeStamp =
            getTimeStampValue(attributes.getNamedItem(XML_LAST_SAVED_CHECKIN_TIME_STAMP));
        workspaceInfo.options = getWorkspaceOptionsValue(attributes.getNamedItem(OPTIONS_NAME));

        for (final Element child : DOMUtils.getChildElements(workspaceInfoNode)) {
            final String name = child.getNodeName();

            if (name.equals(XML_MAPPED_PATHS)) {
                final Element[] mappedPathElements = DOMUtils.getChildElements(child, XML_MAPPED_PATH);
                workspaceInfo.mappedPaths = new String[mappedPathElements.length];

                for (int i = 0; i < mappedPathElements.length; i++) {
                    workspaceInfo.mappedPaths[i] =
                        getStringValue(mappedPathElements[i].getAttributes().getNamedItem(XML_PATH));
                }
            } else if (name.equals(XML_WORKING_FOLDERS)) {
                // Backwards compatibility with PDC build of Dev11 (and
                // earlier).
                final Element[] mapElements = DOMUtils.getChildElements(child, XML_MAP);
                workspaceInfo.mappedPaths = new String[mapElements.length];

                for (int i = 0; i < mapElements.length; i++) {
                    workspaceInfo.mappedPaths[i] =
                        getStringValue(mapElements[i].getAttributes().getNamedItem(XML_LOCAL_PATH));
                }

                workspaceInfo.state = LocalWorkspaceState.MODIFIED;
            } else if (name.equals(XML_LAST_SAVED_CHECKIN)) {
                Check.isTrue(
                    DOMUtils.getChildElements(child, SavedCheckin.XML_SAVED_CHECKIN).length == 1,
                    MessageFormat.format(
                        "Wrong number of LastSavedCheckin children: {0}", //$NON-NLS-1$
                        DOMUtils.getChildElements(child, SavedCheckin.XML_SAVED_CHECKIN).length));
                workspaceInfo.lastSavedCheckin =
                    SavedCheckin.loadFromXML(DOMUtils.getFirstChildElement(child, SavedCheckin.XML_SAVED_CHECKIN));
            } else if (name.equals(XML_OWNER_ALIASES)) {
                final Element[] aliasNodes = DOMUtils.getChildElements(child, XML_OWNER_ALIAS);
                final List<String> aliases = new ArrayList<String>(aliasNodes.length);

                for (int i = 0; i < aliasNodes.length; i++) {
                    final String alias = getStringValue(aliasNodes[i].getAttributes().getNamedItem(XML_OWNER_ALIAS));

                    if (!StringUtil.isNullOrEmpty(alias)) {
                        aliases.add(alias);
                    } else {
                        log.error(MessageFormat.format(
                            "Owner alias loaded from cache was null or empty: {0}", //$NON-NLS-1$
                            workspaceInfo));
                    }
                }

                workspaceInfo.ownerAliases = aliases.toArray(new String[aliases.size()]);
            } else {
                log.warn(MessageFormat.format("Unknown workspace child node: {0}", name)); //$NON-NLS-1$
            }
        }

        if (workspaceInfo.ownerAliases == null) {
            workspaceInfo.ownerAliases = new String[0];
        }

        // Since we've just loaded the workspace, make sure it is marked as
        // clean.
        workspaceInfo.markClean();

        return workspaceInfo;
    }

    private static String getStringValue(final Node attrNode) {
        return getStringValue(attrNode, StringUtil.EMPTY);
    }

    private static String getStringValue(final Node attrNode, final String defaultValue) {
        if (attrNode == null) {
            return defaultValue;
        } else {
            return attrNode.getNodeValue();
        }
    }

    private static boolean getBooleanValue(final Node attrNode) {
        final String v = getStringValue(attrNode, null);

        if (StringUtil.isNullOrEmpty(v)) {
            return false;
        } else {
            return Boolean.parseBoolean(v);
        }
    }

    private static Calendar getTimeStampValue(final Node attrNode) {
        final String v = getStringValue(attrNode, null);

        if (StringUtil.isNullOrEmpty(v)) {
            return null;
        } else {
            return XMLConvert.toCalendar(v, true);
        }
    }

    private static WorkspaceOptions getWorkspaceOptionsValue(final Node attrNode) {
        final String v = getStringValue(attrNode, null);

        if (StringUtil.isNullOrEmpty(v)) {
            return WorkspaceOptions.NONE;
        } else {
            try {
                return WorkspaceOptions.fromFlags(Integer.parseInt(v));
            } catch (final NumberFormatException e) {
                log.warn(
                    MessageFormat.format(
                        "Exception parsing WorkspaceInfo options from ''{0}'', using WorkspaceOptions.NONE", //$NON-NLS-1$
                        v));
                return WorkspaceOptions.NONE;
            }
        }
    }

    /**
     * Saves this instance to the XML format used in the cache file.
     *
     * @param parent
     *        the "ServerInfo" element to append a {@value #XML_WORKSPACE_INFO}
     *        node to with this {@link WorkspaceInfo}'s data
     */
    public void saveAsXML(final Element parent) {
        final Element workspaceInfoNode = DOMUtils.appendChild(parent, XML_WORKSPACE_INFO);

        workspaceInfoNode.setAttribute(XML_NAME, getName());
        workspaceInfoNode.setAttribute(XML_OWNER_NAME, getOwnerName());
        workspaceInfoNode.setAttribute(XML_OWNER_DISPLAY_NAME, getOwnerDisplayName());
        workspaceInfoNode.setAttribute(XML_COMPUTER, getComputer());
        workspaceInfoNode.setAttribute(XML_COMMENT, getComment());
        workspaceInfoNode.setAttribute(XML_IS_LOCAL_WORKSPACE, Boolean.toString(isLocalWorkspace).toLowerCase());
        workspaceInfoNode.setAttribute(
            XML_LAST_SAVED_CHECKIN_TIME_STAMP,
            XMLConvert.toString(lastSavedCheckinTimeStamp, true));
        workspaceInfoNode.setAttribute(OPTIONS_NAME, Integer.toString(options.toIntFlags()));

        /*
         * The security token will not be available from pre-Dev10 servers.
         * Don't try to serialize it if it's null.
         */
        if (null != securityToken) {
            workspaceInfoNode.setAttribute(XML_SECURITY_TOKEN, getSecurityToken());
        }

        final Element mappedPathsNode = DOMUtils.appendChild(workspaceInfoNode, XML_MAPPED_PATHS);

        for (final String path : mappedPaths) {
            final Element mappedPathNode = DOMUtils.appendChild(mappedPathsNode, XML_MAPPED_PATH);
            mappedPathNode.setAttribute(XML_PATH, path);
        }

        if (lastSavedCheckin != null) {
            final Element lastSavedCheckinNode = DOMUtils.appendChild(workspaceInfoNode, XML_LAST_SAVED_CHECKIN);
            lastSavedCheckin.saveAsXML(lastSavedCheckinNode);
        }

        if (ownerAliases != null) {
            final Element ownerAliasesNode = DOMUtils.appendChild(workspaceInfoNode, XML_OWNER_ALIASES);

            for (final String ownerAlias : ownerAliases) {
                final Element ownerAliasNode = DOMUtils.appendChild(ownerAliasesNode, XML_OWNER_ALIAS);
                ownerAliasNode.setAttribute(XML_OWNER_ALIAS, ownerAlias);
            }
        }
    }

    /**
     * Only used by LocalCache to clear the state after a cache merge.
     *
     */
    public void markClean() {
        state = LocalWorkspaceState.CLEAN;
    }

    /**
     * Formats the conflicting workspace name for exception when there is a
     * conflict. The owner of the workspace is included when the owner is
     * different.
     *
     * @param conflictingWorkspace
     *        the workspace that conflicts
     * @return the formatted workspace name
     */
    public String formatWorkspaceNameForException(final WorkspaceInfo conflictingWorkspace) {
        Check.notNull(conflictingWorkspace, "conflictingWorkspace"); //$NON-NLS-1$

        String fullWorkspaceName;
        if (Workspace.compareOwner(getOwnerName(), conflictingWorkspace.getOwnerName()) == 0) {
            fullWorkspaceName = conflictingWorkspace.getName();
        } else {
            fullWorkspaceName =
                new WorkspaceSpec(conflictingWorkspace.getName(), conflictingWorkspace.getOwnerName()).toString();
        }

        // If the repository is different, tack it on too so that it's easier to
        // determine where the other workspace is.
        if (!serverInfo.getServerGUID().equals(conflictingWorkspace.serverInfo.getServerGUID())) {
            fullWorkspaceName =
                MessageFormat.format("{0} [{1}]", fullWorkspaceName, conflictingWorkspace.serverInfo.getURI()); //$NON-NLS-1$
        }

        return fullWorkspaceName;
    }

    /**
     *
     * If the specified mappings are different, they are used instead of the
     * current mappings.
     *
     * @param mappedPaths
     *        the mappings to use to update this workspace (must not be
     *        <code>null</code>)
     * @param fromCache
     *        if true, this update is from the cache file and will not affected
     *        the modification state; use false for updates from the server
     */
    private void updateMappings(final String[] mappedPaths, final boolean fromCache) {
        Check.notNull(mappedPaths, "mappedPaths"); //$NON-NLS-1$

        if (areMappedPathSetsEqual(mappedPaths, this.mappedPaths)) {
            // No work to do
            return;
        }

        this.mappedPaths = mappedPaths.clone();

        if (!fromCache) {
            // Use the property here to ensure that the state hierarchy is
            // honored.
            setState(LocalWorkspaceState.MODIFIED);
        }
    }

    /**
     * Returns true if two sets of mapped local paths are identical.
     *
     * @param set1
     *        the first set (must not be <code>null</code>)
     * @param set2
     *        the second set (must not be <code>null</code>)
     * @return True if the two sets are identical, false otherwise
     */
    public static boolean areMappedPathSetsEqual(final String[] set1, final String[] set2) {
        Check.notNull(set1, "set1"); //$NON-NLS-1$
        Check.notNull(set2, "set2"); //$NON-NLS-1$

        if (set1.length != set2.length) {
            return false;
        }

        Arrays.sort(set1);
        Arrays.sort(set2);

        int i;
        for (i = 0; i < set1.length; i++) {
            if (!LocalPath.equals(set1[i], set2[i])) {
                break;
            }
        }

        return (i == set1.length);
    }

    /**
     * Returns true if two sets of owner aliases are identical.
     *
     * @param set1
     *        The first set of owner aliases (must not be <code>null</code>)
     * @param set2
     *        The second set of owner aliases (must not be <code>null</code>)
     * @return True if the two sets are identical, false otherwise
     */
    public static boolean areOwnerAliasesSetsEqual(final String[] set1, final String[] set2) {
        Check.notNull(set1, "set1"); //$NON-NLS-1$
        Check.notNull(set2, "set2"); //$NON-NLS-1$

        if (set1.length != set2.length) {
            return false;
        }

        // Case-sensitive
        Arrays.sort(set1);
        Arrays.sort(set2);

        int i;
        for (i = 0; i < set1.length; i++) {
            if (!Workspace.matchOwner(set1[i], set2[i])) {
                break;
            }
        }

        return (i == set1.length);
    }

    /**
     * Updates this workspace using the comment, computer, and mappings of the
     * workspace object passed in, which must have the same name, owner, and
     * repository as this workspace instance.
     *
     * @param ws
     *        the workspace from which to get updated info (must not be
     *        <code>null</code>)
     * @param fromCache
     *        if true, this update is from the cache file and will not affected
     *        the modification state; use false for updates from the server
     */
    public void update(final WorkspaceInfo ws, final boolean fromCache) {
        Check.isTrue(
            Workspace.matchName(ws.getName(), getName()),
            MessageFormat.format("Names must be the same: {0} vs. {1}", getName(), ws.getName())); //$NON-NLS-1$
        Check.isTrue(
            ws.ownerNameMatches(getOwnerName()),
            MessageFormat.format("Owners must be the same: {0} vs. {1}", getOwnerName(), ws.getOwnerName())); //$NON-NLS-1$
        Check.isTrue(
            ws.serverInfo.getServerGUID().equals(serverInfo.getServerGUID()),
            MessageFormat.format(
                "Servers (guid) must be the same: {0} vs. {1}", //$NON-NLS-1$
                serverInfo.getURI(),
                ws.serverInfo.getURI()));

        // Update the name in case the casing has changed.
        if (!getName().equals(ws.getName())) {
            name = ws.getName();

            if (!fromCache) {
                // Use the property here to ensure that the state hierarchy is
                // honored.
                setState(LocalWorkspaceState.MODIFIED);
            }
        }

        // Update owner in case the casing has changed.
        if (!getOwnerName().equals(ws.getOwnerName())) {
            ownerName = ws.getOwnerName();

            if (!fromCache) {
                // Use the property here to ensure that the state hierarchy is
                // honored.
                setState(LocalWorkspaceState.MODIFIED);
            }
        }

        // Update owner display in case it has changed.
        if (!getOwnerDisplayName().equals(ws.getOwnerDisplayName())) {
            ownerDisplayName = ws.getOwnerDisplayName();

            if (!fromCache) {
                // Use the property here to ensure that the state hierarchy is
                // honored.
                setState(LocalWorkspaceState.MODIFIED);
            }
        }

        if (!Workspace.matchComment(ws.getComment(), getComment())) {
            comment = ws.getComment();

            if (!fromCache) {
                // Use the property here to ensure that the state hierarchy is
                // honored.
                setState(LocalWorkspaceState.MODIFIED);
            }
        }

        if (!Workspace.matchSecurityToken(ws.getSecurityToken(), getSecurityToken())) {
            securityToken = ws.getSecurityToken();

            if (!fromCache) {
                // Use the property here to ensure that the state hierarchy is
                // honored.
                setState(LocalWorkspaceState.MODIFIED);
            }
        }

        if (ws.getLocation() != getLocation()) {
            isLocalWorkspace = ws.isLocalWorkspace;

            if (!fromCache) {
                // Use the property here to ensure that the state hierarchy is
                // honored.
                setState(LocalWorkspaceState.MODIFIED);
            }
        }

        if (!Workspace.matchComputer(ws.getComputer(), getComputer())) {
            computer = ws.getComputer();

            if (!fromCache) {
                // Use the property here to ensure that the state hierarchy is
                // honored.
                setState(LocalWorkspaceState.MODIFIED);
            }
        }

        // Create local mappings that point to this workspace and then update.
        final List<String> mappingList = new ArrayList<String>(ws.mappedPaths.length);

        for (final String mapping : ws.mappedPaths) {
            mappingList.add(mapping);
        }

        updateMappings(mappingList.toArray(new String[mappingList.size()]), fromCache);

        // Keep the incoming last attempted checkin if it is newer.
        if (lastSavedCheckinTimeStamp.compareTo(ws.lastSavedCheckinTimeStamp) < 0) {
            lastSavedCheckin = ws.getLastSavedCheckin();
            lastSavedCheckinTimeStamp = ws.lastSavedCheckinTimeStamp;

            if (!fromCache) {
                // Use the property here to ensure that the state hierarchy is
                // honored.
                setState(LocalWorkspaceState.MODIFIED);
            }
        }

        if (!areOwnerAliasesSetsEqual(ws.ownerAliases, ownerAliases)) {
            ownerAliases = ws.ownerAliases.clone();

            if (!fromCache) {
                // Use the property here to ensure that the state hierarchy is
                // honored.
                setState(LocalWorkspaceState.MODIFIED);
            }
        }
    }

    /**
     * This method is used by Workstation to keep local metadata when updating
     * the cache after querying the server.
     */
    public void copyLocalMetadata(final WorkspaceInfo oldInfo) {
        lastSavedCheckin = oldInfo.lastSavedCheckin;
        lastSavedCheckinTimeStamp = oldInfo.lastSavedCheckinTimeStamp;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "WorkspaceInfo [serverInfo={0}, name={1}, ownerName={2}, ownerDisplayName={3}, computer={4}, ownerAliases={5}, comment={6}, isLocalWorkspace={7}, securityToken={8}, mappedPaths={9}, options={10}, state={11}, lastSavedCheckin={12}, lastSavedCheckinTimeStamp={13}]", //$NON-NLS-1$
            serverInfo,
            name,
            ownerName,
            ownerDisplayName,
            computer,
            Arrays.toString(ownerAliases),
            comment,
            isLocalWorkspace,
            securityToken,
            Arrays.toString(mappedPaths),
            options,
            state,
            lastSavedCheckin,
            lastSavedCheckinTimeStamp);
    }

    /**
     * Returns true if the owner name matches any of the valid owner names for
     * this workspace.
     *
     * @param ownerName
     *        the name to match (must not be <code>null</code> or empty)
     */
    public boolean ownerNameMatches(final String ownerName) {
        Check.notNullOrEmpty(ownerName, "ownerName"); //$NON-NLS-1$

        if (Workspace.matchOwner(ownerName, getOwnerName())) {
            return true;
        }

        final String[] aliases = getOwnerAliases();
        if (aliases != null) {
            for (final String aliasName : aliases) {
                if (Workspace.matchOwner(aliasName, ownerName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return the URI of the associated Team Foundation Server
     */
    public URI getServerURI() {
        return serverInfo.getURI();
    }

    /**
     * @return The GUID of the associated Team Foundation Server
     */
    public GUID getServerGUID() {
        return serverInfo.getServerGUID();
    }

    /**
     * @return the name of the workspace
     */
    public String getName() {
        return name;
    }

    /**
     * @return the identity name of the workspace owner
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * @return return the display name of the workspace owner
     */
    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    /**
     * @return the aliases of the workspace owner
     */
    public String[] getOwnerAliases() {
        return this.ownerAliases;
    }

    /**
     * @return the formatted display name of the workspace
     */
    public String getDisplayName() {
        return new WorkspaceSpec(getName(), getOwnerDisplayName()).toString();
    }

    /**
     * @return the formatted unique name of the workspace
     */
    public String getQualifiedName() {
        return new WorkspaceSpec(getName(), getOwnerName()).toString();

    }

    /**
     * @return the computer the workspace is located on
     */
    public String getComputer() {
        return computer;
    }

    /**
     * @return the comment associated with this workspace
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return a {@link WorkspaceLocation} that indicates the location where
     *         data (pending changes, local versions) for this workspace are
     *         stored
     */
    public WorkspaceLocation getLocation() {
        if (isLocalWorkspace) {
            return WorkspaceLocation.LOCAL;
        }

        return WorkspaceLocation.SERVER;
    }

    /**
     * The mapped local paths of the workspace.
     */
    public String[] getMappedPaths() {
        return mappedPaths;
    }

    /**
     * @return the settings associated with the last saved checkin attempt
     */
    public SavedCheckin getLastSavedCheckin() {
        return lastSavedCheckin;
    }

    /**
     * Sets the settings associated with the last saved checkin attempt
     */
    public void setLastSavedCheckin(final SavedCheckin value, final Workstation workstationToSave) {
        if (value == null && lastSavedCheckin == null) {
            return;
        }

        lastSavedCheckin = value;
        lastSavedCheckinTimeStamp = Calendar.getInstance();
        setState(LocalWorkspaceState.MODIFIED);
        workstationToSave.saveConfigIfDirty();
    }

    /**
     * @return the user's security identifier (may be <code>null</code> for
     *         older servers)
     */
    public String getSecurityToken() {
        return securityToken;
    }

    /**
     * @return the options set on a workspace
     */
    public WorkspaceOptions getOptions() {
        return options;
    }

    public InternalServerInfo getServer() {
        return serverInfo;
    }

    public LocalWorkspaceState getState() {
        return state;
    }

    public void setState(final LocalWorkspaceState newState) {
        if (newState.getValue() > state.getValue()) {
            state = newState;
        }
    }
}
