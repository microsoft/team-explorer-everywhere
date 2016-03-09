// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

public class CreateWorkItem {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final Project project = tpc.getWorkItemClient().getProjects().get(SnippetSettings.PROJECT_NAME);

        // Find the work item type matching the specified name.
        final WorkItemType bugWorkItemType = project.getWorkItemTypes().get("Bug"); //$NON-NLS-1$

        // Create a new work item of the specified type.
        final WorkItem newWorkItem = project.getWorkItemClient().newWorkItem(bugWorkItemType);

        // Set the title on the work item.
        newWorkItem.setTitle("Example Work Item"); //$NON-NLS-1$

        // Add a comment as part of the change
        newWorkItem.getFields().getField(CoreFieldReferenceNames.HISTORY).setValue(
            "<p>Created automatically by a sample</p>"); //$NON-NLS-1$

        // Save the new work item to the server.
        newWorkItem.save();

        System.out.println("Work item " + newWorkItem.getID() + " successfully created"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
