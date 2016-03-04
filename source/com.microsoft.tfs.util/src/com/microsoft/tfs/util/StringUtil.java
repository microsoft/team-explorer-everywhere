// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

/**
 * Static helper class to duplicate certain String helper methods used in TFS.
 * This is analogous to the .NET Microsoft.TeamFoundation.Common.StringUtil
 * class. Keeping this separate to StringHelpers as that contains Apache
 * licensed code.
 */
public class StringUtil {

    public static final int MAX_COMMENT_DISPLAY_LENGTH = 120;
    public static final String ELLIPSIS = "..."; //$NON-NLS-1$
    public static final String EMPTY = ""; //$NON-NLS-1$

    /**
     * Format a comment for one line display. Removes all new line characters
     * and tabs then trims any whitespace from the end of the comment and ands
     * an ellipsis if the comment is greater than
     * {@link StringUtil#MAX_COMMENT_DISPLAY_LENGTH}
     *
     * @param comment
     * @return
     */
    public static String formatCommentForOneLine(String comment) {
        if (StringHelpers.isNullOrEmpty(comment)) {
            return EMPTY;
        }

        // Remove new lines
        comment = comment.replace('\n', ' ').replace('\r', ' ');
        // Replace tabs with 4 spaces
        comment = StringHelpers.replace(comment, "\t", "    ").trim(); //$NON-NLS-1$ //$NON-NLS-2$

        // If the comment is greater than 120 characters trim it and add
        // ellipsis.
        if (comment.length() > MAX_COMMENT_DISPLAY_LENGTH) {
            comment = comment.substring(0, MAX_COMMENT_DISPLAY_LENGTH - 3);
            comment = comment + ELLIPSIS;
        }

        return comment;
    }

}
