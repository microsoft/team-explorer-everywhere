// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Manages node dependencies for {@link CatalogNode}s.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogDependencyGroup {
    /**
     * Holds the singleton dependencies. A singleton dependency is a dependency
     * that can have one node per key.
     */
    private final HashMap<String, CatalogNode> singletons;

    /**
     * Holds the dependency sets. A dependency set is a dependency that can have
     * one or more nodes per key.
     */
    private final HashMap<String, ArrayList<CatalogNode>> sets;

    /**
     * Default constructor.
     */
    public CatalogDependencyGroup() {
        singletons = new HashMap<String, CatalogNode>();
        sets = new HashMap<String, ArrayList<CatalogNode>>();
    }

    /**
     * Copy constructor.
     */
    public CatalogDependencyGroup(final CatalogDependencyGroup dependencies) {
        singletons = new HashMap<String, CatalogNode>(dependencies.singletons);
        sets = new HashMap<String, ArrayList<CatalogNode>>(dependencies.sets);
    }

    /**
     * Returns all the nodes that are dependencies. This is not a distinct set.
     */
    public CatalogNode[] getAllDependencies() {
        final ArrayList<CatalogNode> allDependencies = new ArrayList<CatalogNode>();
        allDependencies.addAll(singletons.values());

        final Iterator<ArrayList<CatalogNode>> iterator = sets.values().iterator();
        while (iterator.hasNext()) {
            final ArrayList<CatalogNode> list = iterator.next();
            allDependencies.addAll(list);
        }

        return allDependencies.toArray(new CatalogNode[allDependencies.size()]);
    }

    /**
     * Sets the singleton dependency for the given node with the key. This will
     * add the dependency if it is not present and overwrite it if it is.
     *
     * @param key
     *        The key for this dependency.
     *
     * @param node
     *        The node the dependency is set on.
     */
    public void setSingletonDependency(final String key, final CatalogNode node) {
        singletons.put(key, node);
    }

    /**
     * Removes the node dependency with the given key.
     *
     * @param key
     *        The key for this dependency.
     */
    public void removeSingletonDependency(final String key) {
        singletons.remove(key);
    }

    /**
     * Returns the CatalogNode that is a dependency of this node that is
     * associated with the given key. If the key does not associate with a given
     * dependency then null will be returned.
     *
     * @param key
     *        The key for this dependency.
     *
     * @return The CatalogNode that is a dependency of this node.
     */
    public CatalogNode getSingletonDependency(final String key) {
        if (singletons.containsKey(key)) {
            return singletons.get(key);
        } else {
            return null;
        }
    }

    /**
     * The set of singleton dependencies.
     */
    public HashMap<String, CatalogNode> getSingletons() {
        return singletons;
    }

    /**
     * Adds the CatalogNode to the dependency set that has the provided key.
     *
     * @param key
     *        The key for this dependency.
     *
     * @param node
     *        The node to add to the dependency set.
     */
    public void addSetDependency(final String key, final CatalogNode node) {
        ArrayList<CatalogNode> dependencySet;

        if (!sets.containsKey(key)) {
            dependencySet = new ArrayList<CatalogNode>();
            sets.put(key, dependencySet);
        } else {
            dependencySet = sets.get(key);
        }

        dependencySet.add(node);
    }

    /**
     * Removes the entire dependency set that is associated with the given key.
     *
     * @param key
     *        The key for this dependency.
     */
    public void removeSetDependency(final String key) {
        sets.remove(key);
    }

    /**
     * Removes the provided node from the dependency set that is associated with
     * the given key.
     *
     * @param key
     *        The key for this dependency.
     *
     * @param node
     *        The node to add to the dependency set.
     */
    public void removeSetDependency(final String key, final CatalogNode node) {
        if (sets.containsKey(key)) {
            final ArrayList<CatalogNode> dependencySet = sets.get(key);
            for (int i = dependencySet.size() - 1; i >= 0; i--) {
                final CatalogNode dependency = dependencySet.get(i);
                if (node.getChildItem().equals(dependency.getChildItem())) {
                    dependencySet.remove(i);
                    // don't break here in case it is repeated.
                }
            }

            if (dependencySet.size() == 0) {
                sets.remove(key);
            }
        }
    }

    /**
     * Returns The set of the dependency sets.
     */
    public CatalogNode[] getDependencySet(final String key) {
        if (sets.containsKey(key)) {
            final ArrayList<CatalogNode> dependencySet = sets.get(key);
            return dependencySet.toArray(new CatalogNode[dependencySet.size()]);
        } else {
            return new CatalogNode[0];
        }
    }

    /**
     * Returns the set of dependency sets.
     */
    public HashMap<String, ArrayList<CatalogNode>> getSets() {
        return sets;
    }

    /**
     * Clears the singleton dependencies.
     */
    public void clearSingletonDependencies() {
        singletons.clear();
    }

    /**
     * Clears the dependency set.
     */
    public void clearDependencySets() {
        sets.clear();
    }
}
