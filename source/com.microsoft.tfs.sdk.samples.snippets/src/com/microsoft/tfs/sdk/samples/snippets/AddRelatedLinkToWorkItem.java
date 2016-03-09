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

public class AddRelatedLinkToWorkItem {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final Project project = tpc.getWorkItemClient().getProjects().get(SnippetSettings.PROJECT_NAME);
        final WorkItemClient workItemClient = project.getWorkItemClient();

        // Find the work item type matching the specified name.
        final WorkItemType bugWorkItemType = project.getWorkItemTypes().get("Bug"); //$NON-NLS-1$

        // Create two new work items of type bug.
        final WorkItem firstNewWorkItem = workItemClient.newWorkItem(bugWorkItemType);
        final WorkItem secondNewWorkItem = workItemClient.newWorkItem(bugWorkItemType);

        firstNewWorkItem.setTitle("First work item -- created by sample"); //$NON-NLS-1$
        secondNewWorkItem.setTitle("Second work item -- created by sample"); //$NON-NLS-1$

        firstNewWorkItem.save();
        secondNewWorkItem.save();
        System.out.println("Work item " + firstNewWorkItem.getID() + " successfully created"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println("Work item " + secondNewWorkItem.getID() + " successfully created"); //$NON-NLS-1$ //$NON-NLS-2$

        // Create a related link between the work items.
        final String linkComment = "Link created by sample"; //$NON-NLS-1$
        final RelatedLink newRelatedLink =
            LinkFactory.newRelatedLink(firstNewWorkItem, secondNewWorkItem, linkComment, false);

        // Add the link to the first new work item.
        firstNewWorkItem.getLinks().add(newRelatedLink);
        firstNewWorkItem.save();
        System.out.println("Added a link to work item " + firstNewWorkItem.getID()); //$NON-NLS-1$

        // Reopen the work items and display the links.
        final WorkItem firstWorkItem = workItemClient.getWorkItemByID(firstNewWorkItem.getID());
        showLinks(firstWorkItem);

        final WorkItem secondWorkItem = workItemClient.getWorkItemByID(secondNewWorkItem.getID());
        showLinks(secondWorkItem);
    }

    private static void showLinks(final WorkItem workItem) {
        System.out.println();
        System.out.println("Links for work item " + workItem.getID()); //$NON-NLS-1$
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
