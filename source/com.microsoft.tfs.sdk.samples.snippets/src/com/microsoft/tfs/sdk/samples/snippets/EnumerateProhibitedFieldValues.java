// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

public class EnumerateProhibitedFieldValues {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final Project project = tpc.getWorkItemClient().getProjects().get(SnippetSettings.PROJECT_NAME);
        final WorkItemClient client = project.getWorkItemClient();

        int prohibitedCount = 0;

        for (final WorkItemType workItemType : project.getWorkItemTypes()) {
            final WorkItem workItem = client.newWorkItem(workItemType);
            for (final Field field : workItem.getFields()) {
                for (final String prohibitedValue : field.getProhibitedValues()) {
                    prohibitedCount++;
                    System.out.println("Type='" //$NON-NLS-1$
                        + workItemType.getName()
                        + "' Field='" //$NON-NLS-1$
                        + field.getName()
                        + "' Prohibited='" //$NON-NLS-1$
                        + prohibitedValue
                        + "'"); //$NON-NLS-1$
                }
            }
        }

        System.out.println(prohibitedCount
            + " prohibited values for " //$NON-NLS-1$
            + project.getWorkItemTypes().size()
            + " work item types."); //$NON-NLS-1$
    }
}
