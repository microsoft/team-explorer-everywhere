// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

import java.text.MessageFormat;
import java.util.ArrayList;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.util.Check;

/**
 * @since TEE-SDK-10.1
 */
public class WorkItemLinkUtils {
    public static String buildDescriptionFromWorkItems(final WorkItem[] workItems) {
        if (workItems == null || workItems.length == 0) {
            return ""; //$NON-NLS-1$
        } else if (workItems.length == 1) {
            return buildDescriptionFromWorkItem(workItems[0]);
        } else {
            return Messages.getString("WorkItemLinkUtils.ParentheticalMultipleWorkItemsSelectedMessage"); //$NON-NLS-1$
        }
    }

    /**
     * Builds a one-line work item description including type, ID, and title (if
     * title is present). Locale invariant.
     *
     * @param workItem
     *        the work item (may be <code>null</code>)
     * @return the string containing the work item's information, or an empty
     *         string if the given work item was <code>null</code>
     */
    public static String buildDescriptionFromWorkItem(final WorkItem workItem) {
        if (workItem == null) {
            return ""; //$NON-NLS-1$
        }

        final String rawTitle = (String) workItem.getFields().getField(CoreFieldReferenceNames.TITLE).getValue();
        final String title = (rawTitle != null && rawTitle.trim().length() > 0) ? rawTitle : ""; //$NON-NLS-1$

        return MessageFormat.format(
            "{0} {1}: {2}", //$NON-NLS-1$
            workItem.getFields().getWorkItemType(),
            Integer.toString(workItem.getFields().getID()),
            title);
    }

    /**
     * Builds a string list of the given work items' IDs, separated by commas.
     * Locale invariant.
     *
     * @param workItems
     *        the work items to make a list of (may be <code>null</code>)
     * @return the list of IDs (never <code>null</code> but may be empty)
     */
    public static String buildCommaSeparatedWorkItemIDList(final WorkItem[] workItems) {
        final StringBuffer ids = new StringBuffer();

        if (workItems != null) {
            for (int i = 0; i < workItems.length; i++) {
                if (i > 0) {
                    ids.append(", "); //$NON-NLS-1$
                }
                ids.append(String.valueOf(workItems[i].getID()));
            }
        }

        return ids.toString();
    }

    /**
     * Parses a text list of work item IDs. Locale invariant. Only commas and
     * whitespace characters may separate numeric values. Other characters cause
     * an exception to be thrown.
     *
     * @param textIds
     *        the string containing the work item ID list (must not be
     *        <code>null</code>)
     * @return the array of work item IDs (never <code>null</code> but may be
     *         empty)
     * @throws NumberFormatException
     *         if non-digit, non-whitespace, non-comma characters were found in
     *         the input or an invalid work item ID (like 0) was present
     */
    public static int[] buildWorkItemIDListFromText(String textIds) throws NumberFormatException {
        Check.notNull(textIds, "textIds"); //$NON-NLS-1$

        /*
         * Test for any invalid chars by removing digits, whitespace, and
         * commas.
         */
        final String remainingChars = textIds.replaceAll("[\\d\\s,]", ""); //$NON-NLS-1$ //$NON-NLS-2$
        if (remainingChars.length() > 0) {
            throw new NumberFormatException(
                Messages.getString("WorkItemLinkUtils.WorkItemListContainsInvalidCharacters")); //$NON-NLS-1$
        }

        // replace any non-digit character with whitespace
        textIds = textIds.replaceAll("\\D", " "); //$NON-NLS-1$ //$NON-NLS-2$
        textIds = textIds.trim();

        final ArrayList<Integer> list = new ArrayList<Integer>();
        final String[] ids = textIds.split(" "); //$NON-NLS-1$
        for (int i = 0; i < ids.length; i++) {
            final String textId = ids[i].trim();
            if (textId.length() > 0) {
                final int value = Integer.valueOf(textId);

                if (value == 0) {
                    throw new NumberFormatException(
                        Messages.getString("WorkItemLinkUtils.WorkItemListContainsInvalidCharacters")); //$NON-NLS-1$
                }

                list.add(value);
            }
        }

        final int[] intIds = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            intIds[i] = list.get(i).intValue();
        }

        return intIds;
    }
}
