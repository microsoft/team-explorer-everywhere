// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.text.MessageFormat;

public class PagedCollection {
    private final Object[] objects;
    private int pageSize;
    private final PageCallback callback;
    private boolean pagedAny = false;

    public PagedCollection(final int totalSize, final int pageSize, final PageCallback callback) {
        objects = new Object[totalSize];
        this.pageSize = pageSize;
        this.callback = callback;
    }

    public int getSize() {
        return objects.length;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(final int pageSize) {
        if (pagedAny) {
            throw new IllegalStateException("cannot change page size once paging begins"); //$NON-NLS-1$
        }

        this.pageSize = pageSize;
    }

    public Object getItem(final int index) {
        if (index < 0 || index >= getSize()) {
            throw new IllegalArgumentException(MessageFormat.format(
                "index [{0}] is outside the range of items (0,{1})", //$NON-NLS-1$
                Integer.toString(index),
                Integer.toString(getSize())));
        }

        /*
         * is the requested index already paged in?
         */
        if (objects[index] == null) {
            pagedAny = true;

            /*
             * which page does the requested index fall in?
             */
            final int pageNumber = index / pageSize;

            /*
             * what is the starting index for that page?
             */
            final int pageStart = pageNumber * pageSize;

            /*
             * how long is that page? (normally pageSize, but the last page may
             * be shorter)
             */
            int pageLength = pageSize;
            if (pageStart + pageLength > getSize()) {
                pageLength = getSize() - pageStart;
            }

            /*
             * call back to get the items
             */
            final Object[] newItems = callback.pageInItems(pageStart, pageLength);

            if (newItems == null || newItems.length != pageLength) {
                throw new IllegalStateException("page callback returned bad itemset"); //$NON-NLS-1$
            }

            /*
             * copy the newly paged in item references into our array
             */
            System.arraycopy(newItems, 0, objects, pageStart, pageLength);
        }

        return objects[index];
    }

    public boolean hasPagedAny() {
        return pagedAny;
    }
}
