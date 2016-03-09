// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

import java.util.Iterator;

/**
 * @since TEE-SDK-10.1
 */
public interface LinkCollection extends Iterable<Link> {
    /**
     * @return an iterator for this collection
     */
    @Override
    public Iterator<Link> iterator();

    /**
     * @return the number of {@link Link}s in the collection.
     */
    public int size();

    /**
     * Checks whether this collection contains the specified {@link Link} object
     * or an equivalent.
     *
     * @param link
     *        The {@link Link} object of interest.
     * @return True if the list contains the specified {@link Link} object;
     *         otherwise, false.
     */
    public boolean contains(Link link);

    /**
     * Adds the specified link to this collection.
     *
     * @param link
     *        the link to add (must not be <code>null</code>)
     * @return <code>true</code> if the link was not already in this collection
     *         and was added, <code>false</code> if this collection already
     *         contained the link.
     */
    public boolean add(Link link);

    /**
     * Removes the specified link from this collection.
     *
     * @param link
     *        the link to remove (must not be <code>null</code>)
     */
    public void remove(Link link);
}
