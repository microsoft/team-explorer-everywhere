// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.internal.GetWorkItemFieldNames;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.files.AttachmentImpl;
import com.microsoft.tfs.core.clients.workitem.internal.link.ExternalLinkImpl;
import com.microsoft.tfs.core.clients.workitem.internal.link.HyperlinkImpl;
import com.microsoft.tfs.core.clients.workitem.internal.link.RegisteredLinkTypeImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.revision.RevisionImpl;

/**
 * Handles the Files table that comes back from the GetWorkItem call.
 */
public class WorkItemFilesRowSetHandler extends BaseGetWorkItemRowSetHandler {
    private final Map<Integer, Map<Date, Integer>> adds = new HashMap<Integer, Map<Date, Integer>>();
    private final Map<Integer, Map<Date, Integer>> removes = new HashMap<Integer, Map<Date, Integer>>();

    public WorkItemFilesRowSetHandler(final WorkItemImpl workItem, final IMetadata metadataManager) {
        super(workItem, metadataManager);
    }

    @Override
    protected void doHandleRow() {
        final int fieldId = getIntValue(GetWorkItemFieldNames.FIELD_ID);
        final Date removedDate = getDateValue(GetWorkItemFieldNames.REMOVED_DATE);
        final Date addedDate = getDateValue(GetWorkItemFieldNames.ADDED_DATE);

        incrementCount(adds, fieldId, addedDate);

        if (removedDate != null) {
            incrementCount(removes, fieldId, removedDate);

            /*
             * do no more processing of the deleted entry - all the OM does for
             * now is to record it for revision purposes
             */
            return;
        }

        if (fieldId == WorkItemFieldIDs.ATTACHED_FILES) {
            /*
             * the row represents a file attachment
             */
            final AttachmentImpl attachment = new AttachmentImpl(
                addedDate,
                getDateValue(GetWorkItemFieldNames.CREATION_DATE),
                getDateValue(GetWorkItemFieldNames.LAST_WRITE_DATE),
                getStringValue(GetWorkItemFieldNames.ORIGINAL_NAME),
                getStringValue(GetWorkItemFieldNames.COMMENT),
                getIntValue(GetWorkItemFieldNames.LENGTH),
                getIntValue(GetWorkItemFieldNames.EXT_ID));

            getWorkItem().getAttachmentsInternal().add(attachment);
        } else if (fieldId == WorkItemFieldIDs.LINKED_FILES) {
            /*
             * the row represents a Hyperlink link type
             */
            final HyperlinkImpl link = new HyperlinkImpl(
                getStringValue(GetWorkItemFieldNames.FILE_PATH),
                getStringValue(GetWorkItemFieldNames.COMMENT),
                getIntValue(GetWorkItemFieldNames.EXT_ID),
                false,
                false);

            getWorkItem().getLinksInternal().add(link);
        } else if (fieldId == WorkItemFieldIDs.BIS_LINKS) {
            /*
             * the row represents a ExternalLink link type
             */
            final String uri = getStringValue(GetWorkItemFieldNames.FILE_PATH);
            final ExternalLinkImpl link = new ExternalLinkImpl(
                new RegisteredLinkTypeImpl(getStringValue(GetWorkItemFieldNames.ORIGINAL_NAME)),
                uri,
                getStringValue(GetWorkItemFieldNames.COMMENT),
                getIntValue(GetWorkItemFieldNames.EXT_ID),
                false,
                false);

            getWorkItem().getLinksInternal().add(link);
        } else {
            throw new RuntimeException(
                MessageFormat.format("unexpected row type in Files rowset [{0}]", Integer.toString(fieldId))); //$NON-NLS-1$
        }
    }

    private void incrementCount(final Map<Integer, Map<Date, Integer>> data, final int fieldId, final Date date) {
        final Integer key = new Integer(fieldId);

        Map<Date, Integer> countMap = data.get(key);
        if (countMap == null) {
            countMap = new HashMap<Date, Integer>();
            data.put(key, countMap);
        }
        final Integer currentCount = countMap.get(date);
        if (currentCount == null) {
            countMap.put(date, new Integer(1));
        } else {
            countMap.put(date, new Integer(currentCount.intValue() + 1));
        }
    }

    @Override
    public void handleEndParsing() {
        int currentAttachedFileCount = 0;
        int currentExternalLinkCount = 0;
        int currentHyperlinkCount = 0;

        for (int i = 0; i < getWorkItem().getRevisionsInternal().size(); i++) {
            final RevisionImpl revision = getWorkItem().getRevisionsInternal().getRevisionInternal(i);
            final Date revisionDate = revision.getRevisionDate();

            currentAttachedFileCount = addCountToRevision(
                revision,
                revisionDate,
                currentAttachedFileCount,
                WorkItemFieldIDs.ATTACHED_FILES,
                WorkItemFieldIDs.ATTACHED_FILE_COUNT);

            currentExternalLinkCount = addCountToRevision(
                revision,
                revisionDate,
                currentExternalLinkCount,
                WorkItemFieldIDs.BIS_LINKS,
                WorkItemFieldIDs.EXTERNAL_LINK_COUNT);

            currentHyperlinkCount = addCountToRevision(
                revision,
                revisionDate,
                currentHyperlinkCount,
                WorkItemFieldIDs.LINKED_FILES,
                WorkItemFieldIDs.HYPERLINK_COUNT);
        }
    }

    private int addCountToRevision(
        final RevisionImpl revision,
        final Date revisionDate,
        int currentCount,
        final int trackerFieldId,
        final int countFieldId) {
        final Integer key = new Integer(trackerFieldId);

        /*
         * set the old value for the revision from currentCount
         */
        revision.getFieldInternal(countFieldId).setOriginalValue(new Integer(currentCount));

        /*
         * modify the currentCount with any adds for the revision date
         */
        final Map<Date, Integer> dateToAdds = adds.get(key);
        if (dateToAdds != null) {
            final Integer addCounts = dateToAdds.get(revisionDate);
            if (addCounts != null) {
                currentCount += addCounts.intValue();
            }
        }

        /*
         * modify the current count with any removes for the revision date
         */
        final Map<Date, Integer> dateToRemoves = removes.get(key);
        if (dateToRemoves != null) {
            final Integer removeCounts = dateToRemoves.get(revisionDate);
            if (removeCounts != null) {
                currentCount -= removeCounts.intValue();
            }
        }

        /*
         * now currentCount holds the (possibly modified) new value for this
         * revision
         */
        revision.getFieldInternal(countFieldId).setNewValue(new Integer(currentCount));

        return currentCount;
    }
}
