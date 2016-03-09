// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.node.NodeCollection;

public class NodeCollectionImpl implements NodeCollection {
    /*
     * we keep a HashSet of the nodes for fast contains()
     */
    private final Set<Node> nodeSet = new HashSet<Node>();

    /*
     * we keep a (lazily instantiated) List of nodes in sort order for methods
     * that need that
     */
    private List<Node> sortedNodes;

    /*
     * we keep a map of name -> node for fast lookups
     */
    private final Map<String, Node> nameToNodeMap = new HashMap<String, Node>();

    public NodeCollectionImpl(final Collection<Node> allNodes) {
        for (final Iterator<Node> it = allNodes.iterator(); it.hasNext();) {
            final Node node = it.next();
            nodeSet.add(node);
            nameToNodeMap.put(node.getName().toLowerCase(), node);
        }
    }

    /*
     * ************************************************************************
     * START of implementation of NodeCollection interface
     * ***********************************************************************
     */

    @Override
    public boolean contains(final Node node) {
        return nodeSet.contains(node);
    }

    @Override
    public Node get(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null"); //$NON-NLS-1$
        }
        return nameToNodeMap.get(name.toLowerCase());
    }

    @Override
    public Node[] getNodes() {
        return getSortedNodes().toArray(new Node[] {});
    }

    @Override
    public int size() {
        return nodeSet.size();
    }

    @Override
    public Iterator<Node> iterator() {
        // this is safe as getSortedNodes() returns an unmodifiable List
        return getSortedNodes().iterator();
    }

    /*
     * ************************************************************************
     * END of implementation of NodeCollection interface
     * ***********************************************************************
     */

    /*
     * ************************************************************************
     * START of implementation of internal (NodeCollectionImpl) methods
     * ***********************************************************************
     */

    private List<Node> getSortedNodes() {
        synchronized (this) {
            if (sortedNodes == null) {
                sortedNodes = new ArrayList<Node>();
                sortedNodes.addAll(nodeSet);
                Collections.sort(sortedNodes);

                /*
                 * make it unmodifiable - that way we can return an iterator
                 * directly
                 */
                sortedNodes = Collections.unmodifiableList(sortedNodes);
            }
        }

        return sortedNodes;
    }

    public NodeImpl getByNameInternal(final String name) {
        return (NodeImpl) get(name);
    }

    /*
     * ************************************************************************
     * END of implementation of internal (NodeCollectionImpl) methods
     * ***********************************************************************
     */
}
