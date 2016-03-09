// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.editors;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditorInput;
import com.microsoft.tfs.core.clients.workitem.WorkItem;

public class WebAccessWorkItemEditorInput extends WorkItemEditorInput {
    private int workItemID;

    public WebAccessWorkItemEditorInput(final TFSServer server, final WorkItem workItem, final int documentNumber) {
        super(server, workItem, documentNumber);
        this.workItemID = workItem.getID();
    }

    @Override
    public String getName() {
        return WorkItemHelpers.getWorkItemDocumentName(getWorkItem().getType(), workItemID, getDocumentNumber());
    }

    @Override
    public int getWorkItemID() {
        return workItemID;
    }

    public void updateWorkItemID(final int workItemID) {
        this.workItemID = workItemID;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof WorkItemEditorInput) {
            final WorkItemEditorInput other = (WorkItemEditorInput) obj;

            // If both are new work item IDs, then check the unique document
            // number to see if they are equal. Otherwise the work items are
            // only equal if their IDs match.
            if (workItemID == 0 && other.getWorkItemID() == 0) {
                return getDocumentNumber() == other.getDocumentNumber();
            } else {
                return workItemID == other.getWorkItemID();
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return workItemID;
    }
}
