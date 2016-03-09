// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.node;

import java.util.Iterator;

/**
 * Represents a collection of {@link Node} objects.
 *
 * @since TEE-SDK-10.1
 */
public interface NodeCollection {
    /**
     * @return an iterator for this collection
     */
    public Iterator<Node> iterator();

    /**
     * @return the number of {@link Node}s in the collection.
     */
    public int size();

    /**
     * Gets the {@link Node} object in this collection that has the specified
     * name.
     *
     * @param nodeName
     *        The name of the desired {@link Node}.
     * @return the {@link Node} with the specified name or <code>null</code> if
     *         no matching {@link Node} was found
     */
    public Node get(String nodeName);

    /**
     * @return an array containing all {@link Node}s from the collection.
     */
    public Node[] getNodes();

    /**
     * Checks whether this collection contains the specified {@link Node}
     * object.
     *
     * @param node
     *        The {@link Node} object of interest.
     * @return True if the list contains the specified {@link Node} object;
     *         otherwise, false.
     */
    public boolean contains(Node node);
}
