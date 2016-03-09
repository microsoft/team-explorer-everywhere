// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Hyperlink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

public class AddExternalLinkToWorkItem {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final Project project = tpc.getWorkItemClient().getProjects().get(SnippetSettings.PROJECT_NAME);
        final WorkItemClient workItemClient = project.getWorkItemClient();

        // Find the work item type matching the specified name.
        final WorkItemType bugWorkItemType = project.getWorkItemTypes().get("Bug"); //$NON-NLS-1$

        // Create a new work item.
        final WorkItem newWorkItem = workItemClient.newWorkItem(bugWorkItemType);
        newWorkItem.setTitle("Created by sample"); //$NON-NLS-1$

        // Add an external link of type 'hyperlink' to the work item.
        final String hyperlinkLocation = "www.microsoft.com"; //$NON-NLS-1$
        final String hyperlinkComment = "Microsoft web location"; //$NON-NLS-1$
        final Hyperlink newHyperlink = LinkFactory.newHyperlink(hyperlinkLocation, hyperlinkComment, false);
        newWorkItem.getLinks().add(newHyperlink);

        // Save the new work item to the server.
        newWorkItem.save();
        System.out.println("Work item " + newWorkItem.getID() + " successfully created with hyperlink"); //$NON-NLS-1$ //$NON-NLS-2$

        // Reopen the new work item and display the links.
        final WorkItem workItem = workItemClient.getWorkItemByID(newWorkItem.getID());
        System.out.println("Open work item " + workItem.getID()); //$NON-NLS-1$
        System.out.println("Work item has " + workItem.getLinks().size() + " link(s)"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println();

        for (final Link link : workItem.getLinks()) {
            System.out.println("Link Type: " + link.getLinkType().getName()); //$NON-NLS-1$
            System.out.println("\tComment: " + link.getComment()); //$NON-NLS-1$
            System.out.println("\tDescription: " + link.getDescription()); //$NON-NLS-1$

            if (link instanceof Hyperlink) {
                final Hyperlink hyperlink = (Hyperlink) link;
                System.out.println("\tLocation: " + hyperlink.getLocation()); //$NON-NLS-1$
            } else if (link instanceof RelatedLink) {
                final RelatedLink relatedLink = (RelatedLink) link;
                System.out.println("\tTarget work item ID: " + relatedLink.getTargetWorkItemID()); //$NON-NLS-1$
            } else if (link instanceof ExternalLink) {
                final ExternalLink externalLink = (ExternalLink) link;
                System.out.println("\tArtifact ID: " + externalLink.getArtifactID()); //$NON-NLS-1$
                System.out.println("\tUri: " + externalLink.getURI()); //$NON-NLS-1$
            }
        }
    }
}
