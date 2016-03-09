// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.node;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemConstants;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.NodeMetadata;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.node.NodeCollection;
import com.microsoft.tfs.util.GUID;

/**
 * The implementation of Node.
 *
 *         Threadsafe: Yes
 */
public class NodeImpl implements Node {
    private final WITContext witContext;
    private final NodeMetadata nodeMetadata;
    private final NodeImpl parentNode;
    private NodeCollectionImpl childNodes;

    public NodeImpl(final NodeMetadata nodeMetadata, final NodeImpl parentNode, final WITContext witContext) {
        this.nodeMetadata = nodeMetadata;
        this.parentNode = parentNode;
        this.witContext = witContext;
    }

    @Override
    public String toString() {
        return MessageFormat.format("node: {0} ({1})", Integer.toString(nodeMetadata.getID()), nodeMetadata.getName()); //$NON-NLS-1$
    }

    /*
     * ************************************************************************
     * START of implementation of Node interface
     * ***********************************************************************
     */

    @Override
    public String getURI() {
        String artifactType = null;
        if (isProjectNode()) {
            artifactType = InternalWorkItemConstants.TEAM_PROJECT_NODE_ARTIFACT_TYPE;
        } else {
            artifactType = InternalWorkItemConstants.NODE_ARTIFACT_TYPE;
        }

        final ArtifactID artifactId = new ArtifactID(
            InternalWorkItemConstants.CLASSIFICATION_TOOL_ID,
            artifactType,
            nodeMetadata.getGUID().getGUIDString());

        return artifactId.encodeURI();
    }

    @Override
    public synchronized NodeCollection getChildNodes() {
        if (childNodes == null) {
            final NodeMetadata[] childNodesMetadata =
                witContext.getMetadata().getHierarchyTable().getNodesWithParentID(nodeMetadata.getID());

            final Set<Node> childNodeSet = new HashSet<Node>();
            for (int i = 0; i < childNodesMetadata.length; i++) {
                final NodeImpl childNode = new NodeImpl(childNodesMetadata[i], this, witContext);
                childNodeSet.add(childNode);
            }

            childNodes = new NodeCollectionImpl(childNodeSet);
        }

        return childNodes;
    }

    @Override
    public int getID() {
        return nodeMetadata.getID();
    }

    @Override
    public GUID getGUID() {
        return nodeMetadata.getGUID();
    }

    @Override
    public String getName() {
        return nodeMetadata.getName();
    }

    @Override
    public Node getParent() {
        if (parentNode != null && parentNode.isAreaOrIterationRootNode()) {
            /*
             * don't expose the area or iteration root nodes (or above) to OM
             * clients - the implementation-only method getParentInternal()
             * bypasses this restriction
             *
             * this requires that an OM client can never get their hands on a
             * node at the -43 level or higher, which the OM currently enforces
             */
            return null;
        }

        return parentNode;
    }

    @Override
    public String getPath() {
        if (isProjectNode()) {
            return nodeMetadata.getName();
        }

        final List<String> segments = new ArrayList<String>();
        NodeImpl currentNode = this;

        while (!currentNode.isRootNode()) {
            if (!currentNode.isAreaOrIterationRootNode()) {
                segments.add(0, currentNode.getName());
            }
            currentNode = currentNode.getParentInternal();
        }

        return NodePathUtils.createPathFromSegments(segments.toArray(new String[] {}), 0);
    }

    @Override
    public int compareTo(final Node o) {
        return getName().compareToIgnoreCase(o.getName());
    }

    /*
     * ************************************************************************
     * END of implementation of Node interface
     * ***********************************************************************
     */

    /*
     * ************************************************************************
     * START of implementation of internal (NodeImpl) methods
     * ***********************************************************************
     */

    public NodeImpl findFirstChildOfStructureType(final int structureType) {
        for (final Iterator<Node> it = getChildNodes().iterator(); it.hasNext();) {
            final Node child = it.next();

            if (((NodeImpl) child).getStructureType() == structureType) {
                return (NodeImpl) child;
            }
        }

        throw new IllegalStateException(
            MessageFormat.format(
                "unable to find any child node of [{0}] with structure type [{1}]", //$NON-NLS-1$
                this,
                structureType));
    }

    public NodeImpl findNodeDownwards(final String path, final boolean includeThisNode, final int structureType) {
        final String[] pathSegments = NodePathUtils.splitPathIntoSegments(path);

        if (pathSegments.length == 0) {
            return null;
        }

        int startingIx = 0;

        if (includeThisNode) {
            if (!pathSegments[0].equalsIgnoreCase(nodeMetadata.getName())) {
                return null;
            }
            startingIx = 1;
        }

        NodeImpl currentNode = this;

        for (int i = startingIx; i < pathSegments.length; i++) {
            if (currentNode.isProjectNode()) {
                if (structureType == NodeStructureType.AREA || structureType == NodeStructureType.ITERATION) {
                    currentNode = currentNode.findFirstChildOfStructureType(structureType);
                }
            }

            final NodeImpl childNode = currentNode.getChildNodesInternal().getByNameInternal(pathSegments[i]);

            if (childNode == null) {
                return null;
            }
            currentNode = childNode;
        }

        return currentNode;
    }

    public NodeImpl findNodeDownwards(final int id) {
        /*
         * fast path: we are the requested node
         */
        if (nodeMetadata.getID() == id) {
            return this;
        }

        /*
         * first, try each child node (breadth-first search)
         */
        for (final Iterator<Node> it = getChildNodes().iterator(); it.hasNext();) {
            final Node child = it.next();

            if (child instanceof NodeImpl && ((NodeImpl) child).nodeMetadata.getID() == id) {
                return (NodeImpl) child;
            }
        }

        /*
         * finally, call to findNodeDownwards on each child
         */
        for (final Iterator<Node> it = getChildNodes().iterator(); it.hasNext();) {
            final Node child = it.next();

            if (child instanceof NodeImpl) {
                final Node target = ((NodeImpl) child).findNodeDownwards(id);

                if (target != null && target instanceof NodeImpl) {
                    return (NodeImpl) target;
                }
            }
        }

        /*
         * couldn't find it
         */
        return null;
    }

    public NodeImpl getProjectNodeParent() {
        if (isProjectNode()) {
            return this;
        }
        if (isRootNode()) {
            throw new IllegalStateException("getProjectNodeParent is illegal for root node"); //$NON-NLS-1$
        }
        return parentNode.getProjectNodeParent();
    }

    private boolean isProjectNode() {
        return nodeMetadata.getNodeType() == WorkItemFieldIDs.TEAM_PROJECT;
    }

    private boolean isAreaOrIterationRootNode() {
        return parentNode != null && parentNode.isProjectNode();
    }

    private boolean isRootNode() {
        return parentNode == null;
    }

    public int getStructureType() {
        return nodeMetadata.getStructureType();
    }

    public NodeCollectionImpl getChildNodesInternal() {
        return (NodeCollectionImpl) getChildNodes();
    }

    public NodeImpl getParentInternal() {
        return parentNode;
    }

    /*
     * ************************************************************************
     * END of implementation of internal (NodeImpl) methods
     * ***********************************************************************
     */
}
