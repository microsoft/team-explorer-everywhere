// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.sync.resourcestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import com.microsoft.tfs.util.Check;

/**
 * ResourceStore contains a resource tree (the membersCache) and a resource to
 * operation mapping (operationsCache), which holds Objects. It is typically to
 * be used by Synchronize to hold a tree of locally changed resources and their
 * pending changes, and a tree of remotely changed resources and their get
 * operations.
 *
 * This class is NOT thread-safe.
 */
public class ResourceStore<T> {
    // a mapping of local path (key) to a list of children (value)
    private final Map<IResource, List<IResource>> memberMap = new HashMap<IResource, List<IResource>>();

    // a mapping of local path (key) to operation (defined by subclass)
    private final Map<IResource, T> operationMap = new HashMap<IResource, T>();

    /**
     * Return the members of this store beneath a resource.
     *
     * @param resource
     *        Resource to examine
     * @return All resources in the store which are the given resource or
     *         children of.
     */
    public IResource[] members(final IResource resource) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        final List<IResource> members = memberMap.get(resource);

        if (members == null) {
            return new IResource[0];
        }

        return members.toArray(new IResource[members.size()]);
    }

    /**
     * Get the operation for the given resource.
     *
     * @param resource
     *        Resource to query
     * @return The operation for the given Resource or null if there was none
     *         found.
     */
    public T getOperation(final IResource resource) {
        return operationMap.get(resource);
    }

    /**
     * Add the resource to the store with the given operation.
     *
     * @param resource
     *        Resource (key) for the store
     * @param operation
     *        Operation (value) for the store
     * @return true if the Resource was newly added, false if the resource
     *         already existed in the store.
     */
    public boolean addOperation(final IResource resource, final T operation) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(operation, "operation"); //$NON-NLS-1$

        final T oldOperation = operationMap.get(resource);

        if (oldOperation != null && oldOperation.equals(operation)) {
            return false;
        }

        operationMap.put(resource, operation);
        addToParent(resource);

        return true;
    }

    /**
     * Add the resource as a child of its parent.
     *
     * @param resource
     *        The resource to add.
     */
    private void addToParent(final IResource resource) {
        final IContainer parent = resource.getParent();

        if (parent == null) {
            return;
        }

        List<IResource> members = memberMap.get(parent);
        if (members == null) {
            members = new ArrayList<IResource>();
            memberMap.put(parent, members);
        }

        if (!members.contains(resource)) {
            members.add(resource);
        }

        addToParent(parent);
    }

    /**
     * Searches the store for a resource.
     *
     * @param resource
     *        Resource to examine
     * @return true if the Resource is in the store, false otherwise
     */
    public boolean contains(final IResource resource) {
        return operationMap.containsKey(resource);
    }

    /**
     * Remove the operation for the given resource from the store. Also removes
     * any children.
     *
     * @param resource
     *        The resource to remove
     * @return All resources removed (includes child resources)
     */
    public List<IResource> removeOperation(final IResource resource) {
        final List<IResource> removedResources = new ArrayList<IResource>();

        // remove any children from the membersCache and operations cache as
        // well
        final List<IResource> children = memberMap.remove(resource);
        if (children != null) {
            final Iterator<IResource> i = children.iterator();
            while (i.hasNext()) {
                removedResources.addAll(removeOperation(i.next()));
            }
        }

        // remove from the parent's membersCache entry, if there is one
        final IContainer parent = resource.getParent();
        if (parent != null && !(parent instanceof IWorkspaceRoot)) {
            final List<IResource> members = memberMap.get(parent);
            if (members != null) {
                members.remove(resource);

                // if there's no more changes to the parent, remove it, too...
                if (members.isEmpty()) {
                    removedResources.addAll(removeOperation(parent));
                }
            }
        }

        // remove from OperationsCache
        operationMap.remove(resource);

        removedResources.add(resource);

        return removedResources;
    }
}
