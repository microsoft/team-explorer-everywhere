// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._Item;
import ms.tfs.versioncontrol.clientservices._03._ItemSet;

/**
 * A collection of {@link Item}s, with some additional information about the
 * query that retrieved the set.
 *
 * @since TEE-SDK-10.1
 */
public final class ItemSet extends WebServiceObjectWrapper {
    public ItemSet() {
        this(new _ItemSet());
    }

    public ItemSet(final _ItemSet set) {
        super(set);
    }

    /**
     * @param queryPath
     *        the path where this set is rooted (server usually supplies this),
     *        may not be null.
     * @param pattern
     *        the name pattern represented by this set (server usually supplies
     *        this); may be null.
     * @param items
     *        server items to add to this set, may not be null.
     */
    public ItemSet(final String queryPath, final String pattern, final Item[] items) {
        super(new _ItemSet(queryPath, pattern, (_Item[]) WrapperUtils.unwrap(_Item.class, items)));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ItemSet getWebServiceObject() {
        return (_ItemSet) webServiceObject;
    }

    /**
     * Gets a copy of the items in this item set.
     *
     * @return a copy of the items in this item set.
     */
    public Item[] getItems() {
        return (Item[]) WrapperUtils.wrap(Item.class, getWebServiceObject().getItems());
    }

    /**
     * Sets the items in this item set.
     *
     * @param items
     *        the items to set (must not be <code>null</code>)
     */
    public void setItems(final Item[] items) {
        Check.notNull(items, "items"); //$NON-NLS-1$
        getWebServiceObject().setItems((_Item[]) WrapperUtils.unwrap(_Item.class, items));
    }

    public String getQueryPath() {
        return getWebServiceObject().getQueryPath();
    }

    public String getPattern() {
        return getWebServiceObject().getPattern();
    }
}
