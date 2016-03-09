// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.internal.GetWorkItemFieldNames;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.link.RelatedLinkImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.revision.RevisionImpl;

/**
 * Handles the Relations table that comes back from the GetWorkItem call.
 */
public class WorkItemRelationsRowSetHandler extends BaseGetWorkItemRowSetHandler {
    private final Map<Date, Integer> data = new HashMap<Date, Integer>();

    public WorkItemRelationsRowSetHandler(final WorkItemImpl workItem, final IMetadata metadataManager) {
        super(workItem, metadataManager);
    }

    @Override
    protected void doHandleRow() {
        /*
         * The ID of the related work item
         */
        final int id = getIntValue(GetWorkItemFieldNames.ID);

        /*
         * The link comment
         */
        final String comment = getStringValue(GetWorkItemFieldNames.COMMENT);

        /*
         * the "added date" of the link
         */
        final Date changedDate = getDateValue(GetWorkItemFieldNames.CHANGED_DATE);

        /*
         * the "id" of the work item link type for Dev10 and beyond.
         */
        int linkTypeId = 0;
        if (getWorkItem().getClient().supportsWorkItemLinkTypes()) {
            linkTypeId = getIntValue(GetWorkItemFieldNames.LINK_TYPE);
        }

        /*
         * build a link object and add it to the work item
         */
        boolean readOnly = false;
        if (getWorkItem().getClient().supportsReadOnlyLinkTypes()) {
            readOnly = getBooleanValue(GetWorkItemFieldNames.LOCK);
        }

        final RelatedLinkImpl link = new RelatedLinkImpl(getWorkItem(), id, linkTypeId, comment, false, readOnly);
        getWorkItem().getLinksInternal().add(link);

        /*
         * the data map maps added dates to related link counts update the map,
         * incrementing the count for the current added date
         */
        final Integer currentCount = data.get(changedDate);
        if (currentCount == null) {
            data.put(changedDate, new Integer(1));
        } else {
            data.put(changedDate, new Integer(currentCount.intValue() + 1));
        }
    }

    @Override
    public void handleEndParsing() {
        int currentRelatedLinkCount = 0;

        for (int i = 0; i < getWorkItem().getRevisionsInternal().size(); i++) {
            final RevisionImpl revision = getWorkItem().getRevisionsInternal().getRevisionInternal(i);
            final Date revisionDate = revision.getRevisionDate();

            /*
             * the current related link count is the old value for the current
             * revision
             */
            revision.getFieldInternal(WorkItemFieldIDs.RELATED_LINK_COUNT).setOriginalValue(
                new Integer(currentRelatedLinkCount));

            if (data.containsKey(revisionDate)) {
                /*
                 * related links were added for this revision, so update the
                 * current link count
                 */
                final Integer addedAtThisRevisionCount = data.get(revisionDate);
                currentRelatedLinkCount += addedAtThisRevisionCount.intValue();
            }

            /*
             * the current count (either updated or unchanged) is now also the
             * new value for this revision
             */
            revision.getFieldInternal(WorkItemFieldIDs.RELATED_LINK_COUNT).setNewValue(
                new Integer(currentRelatedLinkCount));
        }
    }
}
