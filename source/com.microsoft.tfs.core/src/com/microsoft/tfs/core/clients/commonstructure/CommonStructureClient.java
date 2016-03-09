// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.commonstructure;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.exceptions.mappers.ClassificationExceptionMapper;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.Check;

import ms.tfs.services.classification._03._Classification4Soap;
import ms.tfs.services.classification._03._ClassificationSoap;
import ms.tfs.services.classification._03._NodeInfo;
import ms.tfs.services.classification._03._ProjectInfo;
import ms.tfs.services.classification._03._ProjectProperty;

/**
 * Accesses the Team Foundation Server common structure web services.
 *
 * @since TEE-SDK-10.1
 */
public class CommonStructureClient {
    private final TFSTeamProjectCollection connection;
    private final _ClassificationSoap webService;
    private final _Classification4Soap webService4;

    private final Object cacheLock = new Object();
    private final HashMap<String, Object> projectInfoCacheByUri = new HashMap<String, Object>();
    private final HashMap<String, Object> projectInfoCacheByName = new HashMap<String, Object>();

    public CommonStructureClient(
        final TFSTeamProjectCollection connection,
        final _ClassificationSoap webService,
        final _Classification4Soap webService4) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(webService, "webService"); //$NON-NLS-1$

        this.connection = connection;
        this.webService = webService;
        this.webService4 = webService4;
    }

    public TFSTeamProjectCollection getConnection() {
        return connection;
    }

    /**
     * Clears the project information cache.
     */
    public void clearProjectInfoCache() {
        synchronized (cacheLock) {
            projectInfoCacheByUri.clear();
            projectInfoCacheByName.clear();
        }
    }

    /**
     * Creates a new node in a classification service structure.
     *
     * @param nodeName
     *        Name of the node to create. If the parent already has a node with
     *        this name, then the new node will not be created.
     * @param parentNodeUri
     *        The URI of the node which will be the parent of the new node.
     * @return A string value containing the URI of the created node.
     */
    public String createNode(final String nodeName, final String parentNodeUri) {
        String nodeUri;
        try {
            nodeUri = webService.createNode(nodeName, parentNodeUri);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
        return nodeUri;
    }

    /**
     * Deletes one or more branches
     *
     * @param nodeUris
     *        The URIs of the nodes that are the roots of the branches to be
     *        deleted
     * @param reclassifyUri
     *        The URI of the node to which artifacts are reclassified
     */
    public void deleteBranches(final String[] nodeUris, final String reclassifyUri) {
        try {
            webService.deleteBranches(nodeUris, reclassifyUri);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }

    /**
     * Deletes a project context and the associated structures
     *
     * @param projectUri
     *        The URI of the project context to delete
     */
    public void deleteProject(final String projectUri) {
        try {
            synchronized (cacheLock) {
                final ProjectInfo pi = (ProjectInfo) projectInfoCacheByUri.get(projectUri);
                projectInfoCacheByUri.remove(projectUri);
                projectInfoCacheByName.remove(pi.getName());
            }
            webService.deleteProject(projectUri);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }

    /**
     * Gets the set of changed nodes with an ID greater than a specified ID.
     *
     * @param firstSequenceId
     *        The lowest allowable ID.
     */
    public String getChangedNodes(final int firstSequenceId) {
        String changedNodes;
        try {
            changedNodes = webService.getChangedNodes(firstSequenceId);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
        return changedNodes;
    }

    /**
     * Gets a NodeInfo structure using a node URI
     *
     * @param nodeUri
     *        The URI of the node to be obtained
     * @return An ANodeInfo value containing the node information structure.
     */
    public NodeInfo getNode(final String nodeUri) {
        NodeInfo nodeInfo;
        try {
            final _NodeInfo ni = webService.getNode(nodeUri);
            nodeInfo = new NodeInfo(ni);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
        return nodeInfo;
    }

    /**
     * Gets a NodeInfo structure using a node pathname.
     *
     *
     * @param nodePath
     *        The URI of the node to be obtained
     * @return An ANodeInfo value containing the node information structure.
     */
    public NodeInfo getNodeFrom(final String nodePath) {
        NodeInfo nodeInfo;
        try {
            final _NodeInfo ni = webService.getNodeFromPath(nodePath);
            nodeInfo = new NodeInfo(ni);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
        return nodeInfo;
    }

    public CSSNode getCSSNodes(final String rootNodeUri, final boolean includeChildren) {
        final CSSNode[] nodes = getCSSNodes(new String[] {
            rootNodeUri
        }, includeChildren);
        if (nodes.length == 0) {
            throw new TECoreException(MessageFormat.format("Unable to find nodes for \"{0}\"", rootNodeUri)); //$NON-NLS-1$
        }
        if (nodes.length > 1) {
            throw new TECoreException(MessageFormat.format(
                "Too many nodes returned for \"{0}\".  Found {1}", //$NON-NLS-1$
                rootNodeUri,
                nodes.length));
        }
        return nodes[0];
    }

    public CSSNode[] getCSSNodes(final String[] rootNodeUris, final boolean includeChildren) {
        CSSNode[] nodeArray;
        try {
            final DOMAnyContentType result =
                (DOMAnyContentType) webService.getNodesXml(rootNodeUris, includeChildren, new DOMAnyContentType());

            final NodeList nodes = result.getElements()[0].getChildNodes();
            nodeArray = new CSSNode[nodes.getLength()];
            for (int i = 0; i < nodes.getLength(); i++) {
                nodeArray[i] = getCSSNode(nodes.item(i));
            }
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
        return nodeArray;
    }

    private CSSNode getCSSNode(final Node node) {
        final CSSNode cssNode = new CSSNode(
            CSSStructureType.fromString(getAttributeValue(node, "StructureType")), //$NON-NLS-1$
            getAttributeValue(node, "NodeID"), //$NON-NLS-1$
            getAttributeValue(node, "Name"), //$NON-NLS-1$
            getAttributeValue(node, "ParentID"), //$NON-NLS-1$
            getAttributeValue(node, "Path"), //$NON-NLS-1$
            getAttributeValue(node, "ProjectID")); //$NON-NLS-1$

        NodeList children = node.getChildNodes();
        if (children != null && children.getLength() > 0) {
            if ("Children".equals(children.item(0).getNodeName())) //$NON-NLS-1$
            {
                children = children.item(0).getChildNodes();
            }
            for (int i = 0; i < children.getLength(); i++) {
                cssNode.addChild(getCSSNode(children.item(i)));
            }
        }

        return cssNode;
    }

    private String getAttributeValue(final Node node, final String attributeName) {
        if (node == null || node.getAttributes() == null || node.getAttributes().getNamedItem(attributeName) == null) {
            return null;
        }
        return node.getAttributes().getNamedItem(attributeName).getNodeValue();
    }

    /**
     * @param projectUri
     *        The URI of the project to be obtained
     * @return An AProjectInfo value containing the project information
     *         structure.
     */
    public ProjectInfo getProject(final String projectUri) {
        ProjectInfo projectInfo = null;
        synchronized (cacheLock) {
            if (projectInfoCacheByUri.containsKey(projectUri)) {
                projectInfo = (ProjectInfo) projectInfoCacheByUri.get(projectUri);
            } else {
                try {
                    final _ProjectInfo pi = webService.getProject(projectUri);
                    if (pi != null) {
                        projectInfo = new ProjectInfo(pi);
                        projectInfo.setSourceControlCapabilityFlags(connection.getSourceControlCapability(projectInfo));
                        projectInfoCacheByUri.put(projectUri, projectInfo);
                        projectInfoCacheByName.put(pi.getName(), projectInfo);
                    }
                } catch (final ProxyException e) {
                    throw ClassificationExceptionMapper.map(e);
                }
            }
        }
        return projectInfo;
    }

    /**
     * Gets a ProjectInfo structure using a project name
     *
     * @param projectName
     *        The name of the project to be obtained
     * @return An AProjectInfo object containing the project information
     *         structure.
     */
    public ProjectInfo getProjectFromName(final String projectName) {
        ProjectInfo projectInfo = null;
        synchronized (cacheLock) {
            if (projectInfoCacheByName.containsKey(projectName)) {
                projectInfo = (ProjectInfo) projectInfoCacheByName.get(projectName);
            } else {
                try {
                    final _ProjectInfo pi = webService.getProjectFromName(projectName);
                    if (pi != null) {
                        projectInfo = new ProjectInfo(pi);
                        projectInfo.setSourceControlCapabilityFlags(connection.getSourceControlCapability(projectInfo));
                        projectInfoCacheByName.put(pi.getName(), projectInfo);
                        projectInfoCacheByUri.put(pi.getUri(), projectInfo);
                    }
                } catch (final ProxyException e) {
                    throw ClassificationExceptionMapper.map(e);
                }
            }
        }
        return projectInfo;
    }

    public ProjectInfo[] listAllProjects() {
        _ProjectInfo[] projectInfoArray;
        try {
            projectInfoArray = webService.listAllProjects();
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }

        final ProjectInfo[] aProjectInfoArray = new ProjectInfo[projectInfoArray.length];

        for (int i = 0; i < projectInfoArray.length; i++) {
            aProjectInfoArray[i] = new ProjectInfo(projectInfoArray[i]);
            aProjectInfoArray[i].setSourceControlCapabilityFlags(
                connection.getSourceControlCapability(aProjectInfoArray[i]));
        }

        return aProjectInfoArray;
    }

    public ProjectInfo[] listProjects() {
        _ProjectInfo[] projectInfoArray;
        try {
            projectInfoArray = webService.listProjects();
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }

        final ProjectInfo[] aProjectInfoArray = new ProjectInfo[projectInfoArray.length];

        for (int i = 0; i < projectInfoArray.length; i++) {
            aProjectInfoArray[i] = new ProjectInfo(projectInfoArray[i]);
            aProjectInfoArray[i].setSourceControlCapabilityFlags(
                connection.getSourceControlCapability(aProjectInfoArray[i]));
        }

        return aProjectInfoArray;
    }

    /**
     * Lists the structures in a project
     *
     * @param projectUri
     *        URI of the project from which the structure is to be obtained
     * @return An array of ANodeInfo objects containing the URI of the root node
     *         of the structure.
     */
    public NodeInfo[] listStructures(final String projectUri) {
        _NodeInfo[] nodeInfoArray;
        try {
            nodeInfoArray = webService.listStructures(projectUri);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }

        final NodeInfo[] aNodeInfoArray = new NodeInfo[nodeInfoArray.length];

        for (int i = 0; i < nodeInfoArray.length; i++) {
            aNodeInfoArray[i] = new NodeInfo(nodeInfoArray[i]);
        }

        return aNodeInfoArray;
    }

    /**
     * Moves a node, along with the entire branch below the node, to a new
     * position in the hierarchy
     *
     * The type of the moved node must be appropriate to be a child of the new
     * parent, and the new parent cannot be in the branch below the moved node.
     * The effects on tools that hold pathnames is equivalent to the effects of
     * RenameNode, above. There are additional effects on tools that infer
     * information from the parentage of the node. For example, TFS might derive
     * security from a node, and it's parentage in the hierarchy. Tools that
     * hold only URIs do not need to respond to CSS change events that occur for
     * MoveNode.
     *
     * @param nodeUri
     *        The URI of the node to be moved
     * @param newParentNodeUri
     *        The URI of the new parent
     */
    public void moveBranch(final String nodeUri, final String newParentNodeUri) {
        try {
            webService.moveBranch(nodeUri, newParentNodeUri);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }

    /**
     * Renames the specified node.
     *
     * This has the effect of changing the path for this node and the entire
     * branch below this node.
     *
     * @param nodeUri
     *        The URI of the node to rename
     * @param newNodeName
     *        The new name of the node
     */
    public void renameNode(final String nodeUri, final String newNodeName) {
        try {
            webService.renameNode(nodeUri, newNodeName);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }

    /**
     * Moves a node to a new position in the order of nodes within its parent
     *
     * @param nodeUri
     *        The URI of the node to be moved
     * @param moveBy
     *        The number of places to move. Negative numbers indicate upwards
     *        movement and positive numbers indicate downward movement. If
     *        moveby would indicate a position further than one end or the
     *        other, then the node is moved to that end.
     */
    public void reorderNode(final String nodeUri, final int moveBy) {
        try {
            webService.reorderNode(nodeUri, moveBy);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }

    /**
     *
     * @param projectUri
     *        The project to be updated
     * @param state
     *        A ProjectState value indicating the state of the project (New,
     *        WellFormed, or Deleting)
     * @param properties
     *        The array of properties for the project
     */
    public void updateProjectProperties(
        final String projectUri,
        final String state,
        final ProjectProperty[] properties) {

        _ProjectProperty[] pp = null;
        if (properties != null) {
            pp = new _ProjectProperty[properties.length];
            for (int i = 0; i < pp.length; i++) {
                pp[i] = new _ProjectProperty(properties[i].getName(), properties[i].getValue());
            }
        }

        try {
            webService.updateProjectProperties(projectUri, state, pp);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }

    /**
     * @since TFS 2012
     */
    public String createNode(
        final String nodeName,
        final String parentNodeUri,
        final Calendar startDate,
        final Calendar finishDate) {
        if (webService4 == null) {
            throw new CommonStructureException(Messages.getString("CommonStructureClient.ServerDoesNotSupportMethod")); //$NON-NLS-1$
        }

        try {
            return webService4.createNodeWithDates(nodeName, parentNodeUri, startDate, finishDate);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }

    /**
     * @since TFS 2012
     */
    public void setIterationDates(final String nodeUri, final Calendar startDate, final Calendar finishDate) {
        if (webService4 == null) {
            throw new CommonStructureException(Messages.getString("CommonStructureClient.ServerDoesNotSupportMethod")); //$NON-NLS-1$
        }

        try {
            webService4.setIterationDates(nodeUri, startDate, finishDate);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }

    /**
     * @since TFS 2012
     */
    public ProjectProperty getProjectProperty(final String projectUri, final String name) {
        if (webService4 == null) {
            throw new CommonStructureException(Messages.getString("CommonStructureClient.ServerDoesNotSupportMethod")); //$NON-NLS-1$
        }

        try {
            return new ProjectProperty(webService4.getProjectProperty(projectUri, name));
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }

    /**
     * @since TFS 2012
     */
    public void setProjectProperty(final String projectUri, final String name, final String value) {
        if (webService4 == null) {
            throw new CommonStructureException(Messages.getString("CommonStructureClient.ServerDoesNotSupportMethod")); //$NON-NLS-1$
        }

        try {
            webService4.setProjectProperty(projectUri, name, value);
        } catch (final ProxyException e) {
            throw ClassificationExceptionMapper.map(e);
        }
    }
}
