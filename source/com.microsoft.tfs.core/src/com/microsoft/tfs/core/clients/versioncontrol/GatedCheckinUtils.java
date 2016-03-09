// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;

/**
 * Stateless methods to validate gated checkin changesets. These are required
 * because changesets created by the build system for gated definitions differ
 * slightly from normal changesets.
 */
public abstract class GatedCheckinUtils {
    /**
     * Tests whether a committed check in note matches the pending checkin note.
     * All of the committed notes must be present and identical to the pending
     * notes.
     *
     * @param committedCheckinNotes
     *        the notes which were committed (may be <code>null</code>)
     * @param pendingCheckinNotes
     *        the notes which belong to the pending changes (may be
     *        <code>null</code>)
     * @return <code>true</code> if the notes match, <code>false</code> if they
     *         do not match
     */
    public static boolean gatedCheckinNotesMatch(
        final CheckinNote committedCheckinNotes,
        final CheckinNote pendingCheckinNotes) {
        if (null == pendingCheckinNotes && null == committedCheckinNotes) {
            return true;
        } else if (null == pendingCheckinNotes) {
            /*
             * Either there must be no committed checkin notes, or all checkin
             * notes must be empty.
             */
            for (final CheckinNoteFieldValue committedNote : committedCheckinNotes.getValues()) {
                if (committedNote.getValue() != null && committedNote.getValue().length() > 0) {
                    return false;
                }
            }

            return true;
        } else if (null == committedCheckinNotes) {
            /*
             * Either there must be no pending checkin notes, or all checkin
             * notes must be empty.
             */
            for (final CheckinNoteFieldValue pendingNote : pendingCheckinNotes.getValues()) {
                if (pendingNote.getValue() != null && pendingNote.getValue().length() > 0) {
                    return false;
                }
            }

            return false;
        }

        final Map<String, String> pendingFields = new HashMap<String, String>();

        for (final CheckinNoteFieldValue pendingNote : pendingCheckinNotes.getValues()) {
            pendingFields.put(pendingNote.getName(), pendingNote.getValue());
        }

        for (final CheckinNoteFieldValue committedNote : committedCheckinNotes.getValues()) {
            final boolean containsNoteKey = pendingFields.containsKey(committedNote.getName());

            if (containsNoteKey) {
                final String pendingValue = pendingFields.get(committedNote.getName());
                final String committedValue = committedNote.getValue();

                if ((pendingValue == null && committedValue != null && committedValue.length() > 0)
                    || (committedValue == null && pendingValue != null && pendingValue.length() > 0)
                    || (pendingValue != null
                        && committedValue != null
                        && pendingValue.equals(committedValue) == false)) {
                    return false;
                }
            } else if (committedNote.getValue() != null && committedNote.getValue().length() > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether a committed changeset's comment matches a pending comment,
     * accounting for whitespace differences and the ***NO_CI*** comment for
     * gated checkins.
     *
     * @param committedComment
     *        the comment which was committed (may be <code>null</code>)
     * @param pendingComment
     *        the comment which belongs to the pending changes (may be
     *        <code>null</code>)
     * @return <code>true</code> if the comments match, <code>false</code> if
     *         they do not match
     */
    public static boolean gatedCheckinCommentsMatch(String committedComment, String pendingComment) {
        if (null == committedComment) {
            committedComment = ""; //$NON-NLS-1$
        }

        if (null == pendingComment) {
            pendingComment = ""; //$NON-NLS-1$
        }

        // Strip whitespace from the ends
        committedComment = committedComment.trim();
        pendingComment = pendingComment.trim();

        if (committedComment.endsWith(BuildConstants.NO_CI_CHECK_IN_COMMENT)) {
            committedComment =
                committedComment.substring(0, committedComment.lastIndexOf(BuildConstants.NO_CI_CHECK_IN_COMMENT));
        }

        return committedComment.trim().equals(pendingComment);
    }

    /**
     * Tests whether the given work item ID arrays match.
     *
     * @param committedWorkItemIds
     *        the associated IDs which were committed (may be <code>null</code>)
     * @param pendingWorkItemIds
     *        the associated IDs which belong to the pending changes (may be
     *        <code>null</code>)
     * @return <code>true</code> if the IDs match, <code>false</code> if they do
     *         not match
     */
    public static boolean gatedCheckinWorkItemsMatch(final int[] committedWorkItemIds, final int[] pendingWorkItemIds) {
        if (null == committedWorkItemIds) {
            return false;
        } else if (null == pendingWorkItemIds) {
            return committedWorkItemIds.length == 0;
        }

        if (pendingWorkItemIds.length != committedWorkItemIds.length) {
            return false;
        }

        if (pendingWorkItemIds.length == 0 || committedWorkItemIds.length == 0) {
            return true;
        }

        final int[] committedCopy = committedWorkItemIds.clone();
        final int[] pendingCopy = pendingWorkItemIds.clone();

        Arrays.sort(committedCopy);
        Arrays.sort(pendingCopy);

        return Arrays.equals(committedCopy, pendingCopy);
    }

}
