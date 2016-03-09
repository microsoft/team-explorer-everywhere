// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.util.HashSet;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkCollection;

/**
 * This class manages listeners for add, remove, or updated change events for an
 * associated link collection.
 *
 *
 * @threadsafety thread-safe
 */
public class LinkCollectionChangedListenerSupport {
    private final Set<LinkCollectionChangedListener> listeners = new HashSet<LinkCollectionChangedListener>();
    private final LinkCollection linkCollection;

    public LinkCollectionChangedListenerSupport(final LinkCollection linkCollection) {
        this.linkCollection = linkCollection;
    }

    /**
     * Add a listener to this link collection.
     */
    public synchronized void addListener(final LinkCollectionChangedListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener from from this link collection.
     */
    public synchronized void removeListener(final LinkCollectionChangedListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify listeners that the specified link has been added to the link
     * collection.
     */
    public synchronized void fireLinkAdded(final Link link) {
        for (final LinkCollectionChangedListener listener : listeners) {
            listener.linkAdded(link, linkCollection);
        }
    }

    /**
     * Notify listeners that the specified link has been removed from the link
     * collection.
     */
    public synchronized void fireLinkRemoved(final Link link) {
        for (final LinkCollectionChangedListener listener : listeners) {
            listener.linkRemoved(link, linkCollection);
        }
    }

    /**
     * Notify listeners that the query to retrieve the target work item of
     * related links has completed successfully.
     */
    public synchronized void fireLinkTargetsUpdated() {
        for (final LinkCollectionChangedListener listener : listeners) {
            listener.linkTargetsUpdated(linkCollection);
        }
    }
}
