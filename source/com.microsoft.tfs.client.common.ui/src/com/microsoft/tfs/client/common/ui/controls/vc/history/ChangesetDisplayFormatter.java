// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.history;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.NewlineUtils;

public class ChangesetDisplayFormatter {

    public static final int MAX_COMMENT_DISPLAY_LENGTH = 120;

    public static String getCommentString(final Changeset changeset) {
        String comment = changeset.getComment();
        if (comment == null) {
            comment = ""; //$NON-NLS-1$
        } else {
            comment = NewlineUtils.stripNewlines(comment);
        }

        if (comment.length() > MAX_COMMENT_DISPLAY_LENGTH) {
            comment = comment.substring(0, MAX_COMMENT_DISPLAY_LENGTH)
                + Messages.getString("ChangesetDisplayFormatter.CommentTrucationElipsis"); //$NON-NLS-1$
        }

        return comment;
    }

    public static String getUserString(final TFSRepository repository, final Changeset changeset) {
        return changeset.getOwnerDisplayName();
    }

    public static String getChangeString(final Changeset changeset) {
        if (changeset.getChanges() != null && changeset.getChanges().length > 0) {
            final Change change = changeset.getChanges()[0];
            if (change != null) {
                return change.getChangeType().toUIString(false, change);
            }
        }
        return ""; //$NON-NLS-1$
    }

    public static String getIDString(final Changeset changeset) {
        return String.valueOf(changeset.getChangesetID());
    }

}
