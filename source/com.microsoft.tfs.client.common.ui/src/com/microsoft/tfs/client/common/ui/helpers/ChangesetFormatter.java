// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.text.SimpleDateFormat;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;

public class ChangesetFormatter {
    private final Changeset changeset;

    public ChangesetFormatter(final Changeset changeset) {
        this.changeset = changeset;
    }

    public String getID() {
        return String.valueOf(changeset.getChangesetID());
    }

    public String getFirstChangeDescription() {
        final String changeString = getChangeDescription();

        final int commaIndex = changeString.indexOf(',');
        if (commaIndex != -1) {
            return changeString.substring(0, commaIndex);
        } else {
            return changeString;
        }
    }

    public String getChangeDescription() {
        String changeString = ""; //$NON-NLS-1$
        if (changeset.getChanges() != null && changeset.getChanges().length > 0 && changeset.getChanges()[0] != null) {
            changeString = changeset.getChanges()[0].getChangeType().toUIString(false, changeset.getChanges()[0]);
        }

        return changeString;
    }

    public String getUser() {
        return changeset.getOwnerDisplayName();
    }

    public String getFormattedDate() {
        return SimpleDateFormat.getDateTimeInstance().format(changeset.getDate().getTime());
    }

    public String getComment() {
        String cmt = changeset.getComment();
        if (cmt == null) {
            cmt = ""; //$NON-NLS-1$
        }
        cmt = cmt.replace('\n', ' ').replace('\r', ' ');
        return cmt;
    }

    public String getItemPath() {
        String itemPath = ""; //$NON-NLS-1$
        if (changeset.getChanges() != null
            && changeset.getChanges().length > 0
            && changeset.getChanges()[0] != null
            && changeset.getChanges()[0].getItem() != null
            && changeset.getChanges()[0].getItem().getServerItem() != null) {
            itemPath = changeset.getChanges()[0].getItem().getServerItem();
        }
        return itemPath;
    }
}
