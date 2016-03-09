// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Comparator;

public interface IBuildInformation {
    /**
     * The information nodes in the collection.
     *
     *
     * @return
     */
    public IBuildInformationNode[] getNodes();

    /**
     * Returns the information node with the given Id.
     *
     *
     * @param id
     *        The Id of the node to get.
     * @return The node with the given Id, or null if no node was found.
     */
    public IBuildInformationNode getNode(int id);

    /**
     * Returns the information nodes in Nodes (non-recursive) with the given
     * type.
     *
     *
     * @param type
     *        The type for which nodes are returned.
     * @return The list of nodes in Nodes with the given type.
     */
    public IBuildInformationNode[] getNodesByType(String type);

    /**
     * Returns the information nodes in Nodes with the given type and recursion
     * type.
     *
     *
     * @param type
     *        The type for which nodes are returned.
     * @param recursive
     * @return The list of nodes in Nodes with the given type.
     */
    public IBuildInformationNode[] getNodesByType(String type, boolean recursive);

    /**
     * Returns the information nodes in Nodes (non-recursive) with the given
     * type(s).
     *
     *
     * @param types
     *        The type(s) for which nodes are returned.
     * @return The list of nodes in Nodes with the given type(s).
     */
    public IBuildInformationNode[] getNodesByTypes(String[] types);

    /**
     * Returns the information nodes in Nodes with the given type(s) and
     * recursion type.
     *
     *
     * @param types
     *        The type(s) for which nodes are returned.
     * @param recursive
     * @return The list of nodes in Nodes with the given type(s).
     */
    public IBuildInformationNode[] getNodesByTypes(String[] types, boolean recursive);

    /**
     * Returns a sorted list of the information nodes in Nodes and all subtrees
     * (recursive) with the given type. Sorting will be first by hierarchy, and
     * then by the given comparer.
     *
     *
     * @param type
     *        The type for which nodes are returned.
     * @param comparer
     *        The comparison used to sort nodes at the same level in the
     *        hierarchy.
     * @return A sorted list of the information nodes in Nodes and all subtrees
     *         with the given type.
     */
    public IBuildInformationNode[] getSortedNodesByType(String type, Comparator<IBuildInformationNode> comparer);

    /**
     * Returns a sorted list of the information nodes in Nodes and all subtrees
     * (recursive) with the given type(s). Sorting will be first by hierarchy,
     * and then by the given comparer.
     *
     *
     * @param types
     *        The type(s) for which nodes are returned.
     * @param comparer
     *        The comparison used to sort nodes at the same level in the
     *        hierarchy.
     * @return A sorted list of the information nodes in Nodes and all subtrees
     *         with the given type(s).
     */
    public IBuildInformationNode[] getSortedNodesByTypes(String[] types, Comparator<IBuildInformationNode> comparer);

    /**
     * Returns a sorted list of the information nodes in Nodes and all subtrees
     * (recursive). Sorting will be first by hierarchy, and then by the given
     * comparer.
     *
     *
     * @param comparer
     *        The comparison used to sort nodes at the same level in the
     *        hierarchy.
     * @return A sorted list of the information nodes in Nodes and all subtrees.
     */
    public IBuildInformationNode[] getSortedNodes(Comparator<IBuildInformationNode> comparer);

    /**
     * Adds an information node to the collection.
     *
     *
     * @return The new information node.
     */
    public IBuildInformationNode createNode();

    /**
     * Deletes the collection of information nodes from the server.
     *
     *
     */
    public void delete();

    /**
     * Persists any changes to the collection of information nodes (and all
     * subtrees) to the server.
     *
     *
     */
    public void save();
}
