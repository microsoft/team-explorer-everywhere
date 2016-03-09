// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;

public class RunWorkItemQuery {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final Project project = tpc.getWorkItemClient().getProjects().get(SnippetSettings.PROJECT_NAME);
        final WorkItemClient workItemClient = project.getWorkItemClient();

        // Define the WIQL query.
        final String wiqlQuery = "Select ID, Title from WorkItems where (State = 'Active') order by Title"; //$NON-NLS-1$

        // Run the query and get the results.
        final WorkItemCollection workItems = workItemClient.query(wiqlQuery);
        System.out.println("Found " + workItems.size() + " work items."); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println();

        // Write out the heading.
        System.out.println("Query: " + wiqlQuery); //$NON-NLS-1$
        System.out.println();
        System.out.println("ID\tTitle"); //$NON-NLS-1$

        // Output the results of the query.
        final int maxToPrint = 20;
        for (int i = 0; i < workItems.size(); i++) {
            if (i >= maxToPrint) {
                System.out.println("[...]"); //$NON-NLS-1$
                break;
            }

            final WorkItem workItem = workItems.getWorkItem(i);
            System.out.println(workItem.getID() + "\t" + workItem.getTitle()); //$NON-NLS-1$
        }
    }
}
