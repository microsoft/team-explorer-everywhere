// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.build.IBuildInformation;
import com.microsoft.tfs.core.clients.build.IBuildInformationNode;
import com.microsoft.tfs.core.clients.build.InformationNodeConverters;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildTypeConvertor;

public class BuildInformation implements IBuildInformation {
    private static final Log log = LogFactory.getLog(BuildInformation.class);

    private final BuildDetail build;
    private final List<BuildInformationNode> nodes;
    private final BuildInformationNode owner;

    /**
     * Creates a top-level build information node collection for a BuildDetail
     * object and initializes it from an array of BuildInformation objects.
     *
     *
     * @param build
     *        The owner of this collection.
     * @param informationNodes
     *        The BuildInformation objects from which the tree is initialized.
     */
    public BuildInformation(final BuildDetail build, final BuildInformationNode[] informationNodes) {
        this(build, (BuildInformationNode) null);

        // No information nodes - nothing to do.
        if (informationNodes.length > 0) {
            final Map<Integer, Map<Integer, BuildInformationNode>> nodeParentDict =
                new HashMap<Integer, Map<Integer, BuildInformationNode>>();
            Map<Integer, BuildInformationNode> children;

            for (final BuildInformationNode node : informationNodes) {
                // Add node to parent node dictionary.
                children = nodeParentDict.get(node.getParentID());
                if (children == null) {
                    children = new HashMap<Integer, BuildInformationNode>();
                    nodeParentDict.put(node.getParentID(), children);
                }

                node.setBuild(build);

                if (!children.containsKey(node.getID())) {
                    children.put(node.getID(), node);
                } else {
                    log.warn("Duplicate information nodes present in a build!"); //$NON-NLS-1$
                }
            }

            // Build up as much of the tree structure as we can manage.
            for (final BuildInformationNode node : informationNodes) {
                children = nodeParentDict.get(node.getID());
                if (children != null) {
                    final BuildInformation theChildren = (BuildInformation) node.getChildren();

                    final Collection<BuildInformationNode> values = children.values();
                    final BuildInformationNode[] array = values.toArray(new BuildInformationNode[values.size()]);
                    Arrays.sort(array);

                    for (final BuildInformationNode child : array) {
                        child.setParent(node);
                        child.setOwner(theChildren);
                        theChildren.add(child);
                    }
                }
            }

            // Add any unparented nodes as top level nodes.
            for (final BuildInformationNode node : informationNodes) {
                if (node.getParent() == null) {
                    node.setOwner(this);
                    add(node);
                }
            }
        }
    }

    /**
     * Creates an empty build information node collection owned by a particular
     * build information node.
     *
     *
     * @param build
     *        The build which owns this collection.
     * @param owner
     *        The node which owns this collection.
     */
    public BuildInformation(final BuildDetail build, final BuildInformationNode owner) {
        this.build = build;
        this.owner = owner;
        this.nodes = new ArrayList<BuildInformationNode>();
    }

    /**
     * The information nodes in this collection. {@inheritDoc}
     */
    @Override
    public IBuildInformationNode[] getNodes() {
        return nodes.toArray(new IBuildInformationNode[nodes.size()]);
    }

    /**
     * Adds an information node to this collection. {@inheritDoc}
     */
    @Override
    public IBuildInformationNode createNode() {
        BuildInformationNode newNode;

        if (owner == null) {
            newNode = new BuildInformationNode(build, this);
        } else {
            newNode = new BuildInformationNode(build, owner);
        }

        nodes.add(newNode);
        return newNode;
    }

    /**
     * Removes the node from this collection.
     *
     *
     * @param node
     */
    public void deleteNode(final IBuildInformationNode node) {
        nodes.remove(node);
    }

    public void add(final BuildInformationNode node) {
        node.setParent(owner);
        nodes.add(node);
    }

    /**
     * Deletes this collection of information nodes from the server.
     * {@inheritDoc}
     */
    @Override
    public void delete() {
        final List<InformationChangeRequest> requests = new ArrayList<InformationChangeRequest>();

        for (final IBuildInformationNode node : getNodes()) {
            if (node.getID() > 0) {
                final InformationDeleteRequest deleteRequest = new InformationDeleteRequest();
                deleteRequest.setBuildURI(build.getURI());
                deleteRequest.setNodeID(node.getID());

                requests.add(deleteRequest);
            }
        }

        if (requests.size() > 0) {
            final BuildServer buildServer = (BuildServer) build.getBuildServer();
            final InformationChangeRequest[] changes = requests.toArray(new InformationChangeRequest[requests.size()]);

            if (buildServer.getBuildServerVersion().isV2()) {
                buildServer.getBuild2008Helper().updateBuildInformation(changes);
            } else if (buildServer.getBuildServerVersion().isV3()) {
                buildServer.getBuild2010Helper().updateBuildInformation(changes);
            } else {
                buildServer.getBuildService().updateBuildInformation(changes);
            }
        }

        nodes.clear();
    }

    /**
     * Returns the information node with the given Id. {@inheritDoc}
     */
    @Override
    public IBuildInformationNode getNode(final int id) {
        IBuildInformationNode result = null;

        for (final IBuildInformationNode node : getNodes()) {
            if (node.getID() == id) {
                result = node;
            } else {
                result = node.getChildren().getNode(id);
            }

            if (result != null) {
                return result;
            }
        }

        return result;
    }

    /**
     * Returns the information nodes in Nodes with the given type. {@inheritDoc}
     */
    @Override
    public IBuildInformationNode[] getNodesByType(final String type) {
        return getNodesByType(type, false);
    }

    /**
     * Returns the information nodes in Nodes with the given type. {@inheritDoc}
     */
    @Override
    public IBuildInformationNode[] getNodesByType(final String type, final boolean recursive) {
        final List<IBuildInformationNode> result = new ArrayList<IBuildInformationNode>();

        for (final IBuildInformationNode node : getNodes()) {
            if (node.getType().equals(type)) {
                result.add(node);
            }

            if (recursive) {
                final IBuildInformationNode[] children = node.getChildren().getNodesByType(type, true);
                BuildTypeConvertor.addArrayToList(result, children);
            }
        }

        return result.toArray(new IBuildInformationNode[result.size()]);
    }

    /**
     * Returns the information nodes in Nodes with the given type(s).
     * {@inheritDoc}
     */
    @Override
    public IBuildInformationNode[] getNodesByTypes(final String[] types) {
        return getNodesByTypes(types, false);
    }

    /**
     * Returns the information nodes in Nodes with the given type(s).
     * {@inheritDoc}
     */
    @Override
    public IBuildInformationNode[] getNodesByTypes(final String[] types, final boolean recursive) {
        final List<IBuildInformationNode> result = new ArrayList<IBuildInformationNode>();

        for (final IBuildInformationNode node : getNodes()) {
            for (final String type : types) {
                if (node.getType().equals(type)) {
                    result.add(node);
                    continue;
                }
            }

            if (recursive) {
                final IBuildInformationNode[] children = node.getChildren().getNodesByTypes(types, true);
                BuildTypeConvertor.addArrayToList(result, children);
            }
        }

        return result.toArray(new IBuildInformationNode[result.size()]);
    }

    /**
     * Returns a sorted list of the information nodes in Nodes and all subtrees
     * with the given type. {@inheritDoc}
     */
    @Override
    public IBuildInformationNode[] getSortedNodesByType(
        final String type,
        final Comparator<IBuildInformationNode> comparator) {
        final List<IBuildInformationNode> result = new ArrayList<IBuildInformationNode>();

        final IBuildInformationNode[] nodes = getNodesByType(type);
        Arrays.sort(nodes, comparator);

        for (final IBuildInformationNode node : nodes) {
            result.add(node);
            final IBuildInformationNode[] children = node.getChildren().getSortedNodesByType(type, comparator);
            BuildTypeConvertor.addArrayToList(result, children);
        }

        return result.toArray(new IBuildInformationNode[result.size()]);
    }

    /**
     * Returns a sorted list of the information nodes in Nodes and all subtrees
     * with the given type(s). {@inheritDoc}
     */
    @Override
    public IBuildInformationNode[] getSortedNodesByTypes(
        final String[] types,
        final Comparator<IBuildInformationNode> comparator) {
        final List<IBuildInformationNode> result = new ArrayList<IBuildInformationNode>();
        final IBuildInformationNode[] list = getNodesByTypes(types);
        Arrays.sort(list, comparator);

        for (final IBuildInformationNode node : list) {
            result.add(node);
            final IBuildInformationNode[] children = node.getChildren().getSortedNodesByTypes(types, comparator);
            BuildTypeConvertor.addArrayToList(result, children);
        }

        return result.toArray(new IBuildInformationNode[result.size()]);
    }

    /**
     * Returns a sorted list of the information nodes in Nodes and all subtrees
     * (recursive). Sorting will be first by hierarchy, and then by the given
     * comparer. {@inheritDoc}
     */
    @Override
    public IBuildInformationNode[] getSortedNodes(final Comparator<IBuildInformationNode> comparator) {
        final List<IBuildInformationNode> result = new ArrayList<IBuildInformationNode>();

        final IBuildInformationNode[] nodes = getNodes();
        Arrays.sort(nodes, comparator);

        for (final IBuildInformationNode node : nodes) {
            result.add(node);
            final IBuildInformationNode[] children = node.getChildren().getSortedNodes(comparator);
            BuildTypeConvertor.addArrayToList(result, children);
        }

        return result.toArray(new IBuildInformationNode[result.size()]);
    }

    /**
     * Persists any changes to this collection of information nodes (and all
     * subtrees) to the server. {@inheritDoc}
     */
    @Override
    public void save() {
        synchronized (build.syncSave) {
            final List<InformationChangeRequest> requests = new ArrayList<InformationChangeRequest>();

            if (owner != null) {
                // Call GetRequest on m_owner which will walk up to collect
                // unsaved parents, as well as walk down through the subtree.
                requests.addAll(owner.getRequests(true));
            } else {
                for (final IBuildInformationNode node : getNodes()) {
                    // The parent node, m_owner, is null so we do not need to
                    // get unsaved parent nodes in the GetRequest call.
                    requests.addAll(((BuildInformationNode) node).getRequests(false));
                }
            }

            InformationNodeConverters.bulkUpdateInformationNodes(build, requests);
        }
    }

    public BuildDetail getBuild() {
        return build;
    }

    public static <T> void addToList(final List<T> list, final T[] items) {
        for (final T item : items) {
            list.add(item);
        }
    }
}
