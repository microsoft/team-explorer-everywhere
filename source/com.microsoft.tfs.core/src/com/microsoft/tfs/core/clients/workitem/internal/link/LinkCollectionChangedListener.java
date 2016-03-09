// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkCollection;

public interface LinkCollectionChangedListener {
    /**
     * Called when a link is added to the monitored link collection.
     *
     *
     * @param link
     *        The link that was added.
     *
     * @param collection
     *        The link collection that was modified.
     */
    public void linkAdded(Link link, LinkCollection collection);

    /**
     * Called when a link is removed from the monitored link collection.
     *
     *
     * @param link
     *        The link that was removed.
     *
     * @param collection
     *        The link collection that was modified.
     */
    public void linkRemoved(Link link, LinkCollection collection);

    /**
     * Called when a query to retrieve all target work items of related links
     * has completed.
     *
     *
     * @param collection
     *        The link collection that updated the target work item link types.
     */
    public void linkTargetsUpdated(LinkCollection collection);
}
