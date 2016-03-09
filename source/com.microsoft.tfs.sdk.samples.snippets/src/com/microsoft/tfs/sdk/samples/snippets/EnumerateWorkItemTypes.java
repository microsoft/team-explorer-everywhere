// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

public class EnumerateWorkItemTypes {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final Project project = tpc.getWorkItemClient().getProjects().get(SnippetSettings.PROJECT_NAME);

        // Enumerate the work item types for this project.
        for (final WorkItemType workItemType : project.getWorkItemTypes()) {
            System.out.println(workItemType.getName());
        }
    }
}
