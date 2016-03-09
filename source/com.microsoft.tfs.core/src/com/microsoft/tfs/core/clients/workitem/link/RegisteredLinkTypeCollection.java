// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

import java.util.Iterator;

/**
 * @since TEE-SDK-10.1
 */
public interface RegisteredLinkTypeCollection extends Iterable<RegisteredLinkType> {
    /**
     * @return an iterator for this collection
     */
    @Override
    public Iterator<RegisteredLinkType> iterator();

    /**
     * @return the number of {@link RegisteredLinkType}s in the collection.
     */
    public int size();

    /**
     * Checks whether this collection contains the specified
     * {@link RegisteredLinkType} object.
     *
     * @param linkType
     *        The {@link RegisteredLinkType} object of interest.
     * @return True if the list contains the specified
     *         {@link RegisteredLinkType} object; otherwise, false.
     */
    public boolean contains(RegisteredLinkType linkType);

    /**
     * Gets a {@link RegisteredLinkType} by name.
     *
     * @param linkTypeName
     *        the name of the {@link RegisteredLinkType} to get (must not be
     *        <code>null</code>)
     * @return the {@link RegisteredLinkType} for the specified name
     */
    public RegisteredLinkType get(String linkTypeName);
}
