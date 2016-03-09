// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.WorkItemQueryConstants;
import com.microsoft.tfs.core.clients.workitem.internal.query.PageCallback;
import com.microsoft.tfs.core.clients.workitem.internal.query.PagedCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;

/**
 * A paging cache of work items (by id)
 */
public class ResultItemsPager implements PageCallback {
    private final WorkItemClient workItemClient;
    private final int[] ids;

    private final PagedCollection pagedCollection;

    private final List fieldNames = new ArrayList();

    private int currentPos = -1;

    public ResultItemsPager(final WorkItemClient workItemClient, final int[] workItemIds) {
        this.workItemClient = workItemClient;
        ids = workItemIds;

        pagedCollection =
            new PagedCollection((ids != null ? ids.length : 0), WorkItemQueryConstants.DEFAULT_PAGE_SIZE, this);

        fieldNames.add(CoreFieldReferenceNames.ID);
        fieldNames.add(CoreFieldReferenceNames.AREA_ID);
    }

    public WorkItem getWorkItem(final int id) {
        final int index = getWorkItemArrayIndex(id);
        if (index < 0) {
            System.out.println("-1 detected"); //$NON-NLS-1$
        }
        return (WorkItem) pagedCollection.getItem(index);
    }

    /**
     * Return the position of the work item id in the array of ids. Assume that
     * the last position is a good place to start as this list is usually simply
     * incremented over.
     */
    private int getWorkItemArrayIndex(final int id) {
        if (currentPos >= 0 && currentPos < ids.length && ids[currentPos] == id) {
            return currentPos;
        }

        int index = currentPos + 1;
        while (index != currentPos) {
            if (index >= ids.length) {
                // Loop back from beginning if we missed it
                index = 0;
            }
            if (ids[index] == id) {
                currentPos = index;
                return index;
            }
            index++;
        }
        return -1;
    }

    public void addField(final String fieldName) {
        if (fieldNames.contains(fieldName)) {
            return;
        }
        fieldNames.add(fieldName);
    }

    @Override
    public Object[] pageInItems(final int pageStart, final int pageSize) {
        // Build up WIQL to page in those ID's
        final StringBuffer wiql = new StringBuffer();
        wiql.append("Select "); //$NON-NLS-1$

        // Get displayfields
        for (final Iterator it = fieldNames.iterator(); it.hasNext();) {
            final String fieldName = it.next().toString();
            if (!CoreFieldReferenceNames.LINK_TYPE.equals(fieldName)) {
                wiql.append("["); //$NON-NLS-1$
                wiql.append(fieldName);
                wiql.append("], "); //$NON-NLS-1$
            }
        }
        // Remove trailing comma and space
        wiql.setLength(wiql.length() - 2);

        wiql.append(" from workitems"); //$NON-NLS-1$

        // build up list of ID's
        final int[] idsToPage = new int[pageSize];
        System.arraycopy(ids, pageStart, idsToPage, 0, pageSize);

        // TODO - AS OF
        final WorkItemCollection workItems = workItemClient.query(idsToPage, wiql.toString());

        // Put works items into our paging collection
        final WorkItem[] pageData = new WorkItem[workItems.size()];
        for (int i = 0; i < pageData.length; i++) {
            pageData[i] = workItems.getWorkItem(i);
        }

        return pageData;
    }

}
