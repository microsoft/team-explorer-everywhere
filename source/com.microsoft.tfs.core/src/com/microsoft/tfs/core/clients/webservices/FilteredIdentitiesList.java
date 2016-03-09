// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.ws._FilteredIdentitiesList;

/**
 * This is the result of a call to ReadFilteredIdentities
 *
 * @since TEE-SDK-11.0
 * @threadsafety thread-compatible
 */
public class FilteredIdentitiesList {
    private boolean hasMoreItems;
    private TeamFoundationIdentity[] items;
    private int startingIndex;
    private int totalItems;

    /**
     * Creates a {@link FilteredIdentitiesList} that copies its data (shallowly)
     * from a {@link _FilteredIdentitiesList}. {@link FilteredIdentitiesList} is
     * not a proper web service object wrapper, so this is not done as a
     * constructor.
     */
    public static FilteredIdentitiesList fromWebServiceObject(final _FilteredIdentitiesList webServiceObject) {
        final FilteredIdentitiesList ret = new FilteredIdentitiesList();

        ret.hasMoreItems = webServiceObject.isHasMoreItems();
        ret.items =
            (TeamFoundationIdentity[]) WrapperUtils.wrap(TeamFoundationIdentity.class, webServiceObject.getItems());
        ret.startingIndex = webServiceObject.getStartingIndex();
        ret.totalItems = webServiceObject.getTotalItems();

        return ret;
    }

    public FilteredIdentitiesList() {
    }

    public TeamFoundationIdentity[] getItems() {
        return items;
    }

    public void setItems(final TeamFoundationIdentity[] items) {
        this.items = items;
    }

    public boolean getHasMoreItems() {
        return hasMoreItems;
    }

    public void setHasMoreItems(final boolean hasMoreItems) {
        this.hasMoreItems = hasMoreItems;
    }

    public int getTotalItems() {
        if (hasMoreItems) {
            return totalItems;
        } else {
            return items.length;
        }
    }

    public void setTotalItems(final int totalItems) {
        this.totalItems = totalItems;
    }

    public int getStartingIndex() {
        return startingIndex;
    }

    public void setStartingIndex(final int index) {
        this.startingIndex = index;
    }
}