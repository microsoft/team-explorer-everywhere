// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.versioncontrol.clientservices._03._WorkspaceItem;
import ms.tfs.versioncontrol.clientservices._03._WorkspaceItemSet;

/**
 * A collection of {@link WorkspaceItem}s, with some additional information
 * about the query that retrieved the set.
 *
 * @since TEE-SDK-11.0
 */
public final class WorkspaceItemSet extends WebServiceObjectWrapper {
    public WorkspaceItemSet() {
        this(new _WorkspaceItemSet());
    }

    public WorkspaceItemSet(final _WorkspaceItemSet set) {
        super(set);
    }

    /**
     * @param queryPath
     *        the path where this set is rooted (server usually supplies this),
     *        (must not be <code>null</code>)
     * @param pattern
     *        the name pattern represented by this set (server usually supplies
     *        this); may be <code>null</code>
     * @param items
     *        items to add to this set (must not be <code>null</code>)
     */
    public WorkspaceItemSet(final String queryPath, final String pattern, final WorkspaceItem[] items) {
        super(
            new _WorkspaceItemSet(
                queryPath,
                pattern,
                (_WorkspaceItem[]) WrapperUtils.unwrap(_WorkspaceItem.class, items)));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _WorkspaceItemSet getWebServiceObject() {
        return (_WorkspaceItemSet) webServiceObject;
    }

    /**
     * Gets a copy of the items in this item set.
     *
     * @return a copy of the items in this item set.
     */
    public WorkspaceItem[] getItems() {
        return (WorkspaceItem[]) WrapperUtils.wrap(WorkspaceItem.class, getWebServiceObject().getItems());
    }

    public String getQueryPath() {
        return getWebServiceObject().getQueryPath();
    }

    public String getPattern() {
        return getWebServiceObject().getPattern();
    }
}
