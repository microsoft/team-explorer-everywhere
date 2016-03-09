// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

public class EditWorkItemByID {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final Project project = tpc.getWorkItemClient().getProjects().get(SnippetSettings.PROJECT_NAME);
        final WorkItemClient workItemClient = project.getWorkItemClient();

        // Create a new work item, save it, and get its ID.
        final WorkItemType type = project.getWorkItemTypes().get("Bug"); //$NON-NLS-1$
        final WorkItem newWorkItem = workItemClient.newWorkItem(type);
        newWorkItem.setTitle("Created by sample"); //$NON-NLS-1$
        newWorkItem.save();
        final int newWorkItemId = newWorkItem.getID();
        System.out.println("Created work item " + newWorkItemId + " and title '" + newWorkItem.getTitle() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Open a new instance of the work item we just saved and change it.
        final WorkItem workItem = workItemClient.getWorkItemByID(newWorkItemId);
        workItem.setTitle("Edited by sample"); //$NON-NLS-1$
        workItem.save();
        System.out.println("Edited work item " + workItem.getID() + " and title '" + workItem.getTitle() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
