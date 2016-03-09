// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results.data;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEnd;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;
import com.microsoft.tfs.core.clients.workitem.query.qe.DisplayField;

public class WorkItemInfoQueryResult implements LinkedQueryResultData {
    private static final Log log = LogFactory.getLog(WorkItemInfoQueryResult.class);

    private final Query query;
    private final WorkItemLinkInfo[] linkInfos;
    private final ResultItemsPager items;

    public WorkItemInfoQueryResult(final Query query, final WorkItemLinkInfo[] linkInfos) {
        this.query = query;
        this.linkInfos = linkInfos;
        // Get unique set of ID's from Link Infos
        items = new ResultItemsPager(query.getWorkItemClient(), getUniqueWorkItemIDs(linkInfos));

        // Add field names to pager.
        for (int i = 0; i < query.getDisplayFieldList().getSize(); i++) {
            items.addField(query.getDisplayFieldList().getField(i).getReferenceName());
        }
    }

    @Override
    public WorkItem getItem(final int displayRowIndex) {
        final int id = linkInfos[displayRowIndex].getTargetID();
        return items.getWorkItem(id);
    }

    @Override
    public int getLevel(final int displayRowIndex) {
        WorkItemLinkInfo link = linkInfos[displayRowIndex];
        int level = 0;
        while (link != null && link.getSourceID() != 0) {
            level++;
            link = findLinkInfo(displayRowIndex, link.getSourceID());
        }
        if (link == null) {
            return 0;
        }
        return level;
    }

    @Override
    public String getLinkTypeName(final int displayRowIndex) {
        final WorkItemLinkInfo link = linkInfos[displayRowIndex];
        if (link.getLinkTypeID() == 0) {
            return ""; //$NON-NLS-1$
        }

        final WorkItemLinkTypeEnd linkType =
            query.getWorkItemClient().getLinkTypes().getLinkTypeEnds().getByID(link.getLinkTypeID());

        return linkType == null ? "" : linkType.getName(); //$NON-NLS-1$
    }

    @Override
    public boolean isLinkLocked(final int displayRowIndex) {
        final WorkItemLinkInfo link = linkInfos[displayRowIndex];
        if (link != null) {
            return link.isLocked();
        }
        return false;
    }

    private WorkItemLinkInfo findLinkInfo(final int startPos, final int id) {
        // loop backwards from the startPos to find the linkInfo by the passed
        // ID, return null if not found.
        int index = startPos - 1;
        while (index != startPos) {
            if (index < 0) {
                index = linkInfos.length - 1;
            }
            if (linkInfos[index].getTargetID() == id) {
                return linkInfos[index];
            }
            index--;
        }
        return null;
    }

    private int[] getUniqueWorkItemIDs(final WorkItemLinkInfo[] links) {
        // Build up a list of unique ID's
        final Set idSet = new HashSet();
        for (int i = 0; i < links.length; i++) {
            final Integer sourceId = new Integer(links[i].getSourceID());
            final Integer targetId = new Integer(links[i].getTargetID());

            if (sourceId.intValue() != 0 && !idSet.contains(sourceId)) {
                idSet.add(sourceId);
            }
            if (targetId.intValue() != 0 && !idSet.contains(targetId)) {
                idSet.add(targetId);
            }
        }

        // Convert to int[]
        final int[] results = new int[idSet.size()];
        int i = 0;
        for (final Iterator it = idSet.iterator(); it.hasNext();) {
            results[i++] = ((Integer) it.next()).intValue();
        }
        return results;
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public int getCount() {
        return linkInfos.length;
    }

    @Override
    public String getFieldValue(final int displayRowIndex, final DisplayField[] displayFields, final int fieldIndex) {
        return null;
    }

}
