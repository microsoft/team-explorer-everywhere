// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import java.text.MessageFormat;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.NodeInfo;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.util.Check;

public class CreateUseAreaPath {
    private static int MAX_RETRY_COUNT = 5;
    private static int WAIT_IN_MILLISECONDS = 3000;

    public static void main(final String[] args) {
        // Connect to TFS
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        // Retrieve workitemclient
        final WorkItemClient witClient = tpc.getWorkItemClient();

        // Retrieve team project
        final Project project = witClient.getProjects().get(SnippetSettings.PROJECT_NAME);

        // Get team project URI
        final String teamProjectURI = project.getURI();

        final NodeInfo[] nodesGUIDs = tpc.getCommonStructureClient().listStructures(teamProjectURI);

        // Create a new area path
        final String areaName = "SampleAreaPath_" + System.currentTimeMillis(); //$NON-NLS-1$
        final String areaUri = tpc.getCommonStructureClient().createNode(areaName, nodesGUIDs[0].getURI());

        // Get new area path
        final String areaPath = getNodePathWithRetry(witClient, areaUri);

        // Check that the new area path has been successfully created
        if (areaPath == null) {
            System.out.println(MessageFormat.format("Error: failed to retrieve area path ''{0}''", areaName)); //$NON-NLS-1$
            return;
        }

        System.out.println(MessageFormat.format("Area path ''{0}'' successfully created", areaPath)); //$NON-NLS-1$

        // Create a new work item
        final WorkItemType bugWorkItemType = project.getWorkItemTypes().get("Bug"); //$NON-NLS-1$
        final WorkItem newWorkItem = witClient.newWorkItem(bugWorkItemType);

        // Set the title on the work item.
        newWorkItem.setTitle("Example Work Item"); //$NON-NLS-1$

        // Set the area path
        newWorkItem.getFields().getField(CoreFieldReferenceNames.AREA_PATH).setValue(areaPath);

        // Check if the work item fields have valid values
        for (final Field field : newWorkItem.getFields()) {
            if (field.getStatus() != FieldStatus.VALID) {
                System.out.println(MessageFormat.format("Error: {0}", field.getStatus().getInvalidMessage(field))); //$NON-NLS-1$
            }
        }

        // Save the new work item to the server.
        newWorkItem.save();

        System.out.println("Work item " + newWorkItem.getID() + " successfully created"); //$NON-NLS-1$ //$NON-NLS-2$

    }

    /**
     * Calculate the area path of the node with the given URI
     */
    public static String getNodePath(final Node[] nodes, final String uri) {
        Check.notNull(nodes, "nodes"); //$NON-NLS-1$

        for (int i = 0; i < nodes.length; i++) {

            if (nodes[i].getURI().equals(uri)) {
                return nodes[i].getPath();
            }

            /* Depth first recursion */
            final Node[] children = nodes[i].getChildNodes().getNodes();

            if (children != null && children.length > 0) {
                final String nodePath = getNodePath(children, uri);

                if (nodePath != null) {
                    return nodePath;
                }
            }
        }
        return null;
    }

    /**
     * Try to get area path node with some wait time between retries
     *
     */
    public static String getNodePathWithRetry(final WorkItemClient client, final String areaUri) {
        final Project project = client.getProjects().get(SnippetSettings.PROJECT_NAME);

        String areaPath = null;
        int index = 0;

        while (areaPath == null && index < MAX_RETRY_COUNT) {
            try {
                // Wait
                Thread.sleep((int) (WAIT_IN_MILLISECONDS * Math.pow(2, index)));
            } catch (final InterruptedException e) {
                index = MAX_RETRY_COUNT;
            }

            // Refresh the wit cache
            client.refreshCache();

            // Get the area path node
            areaPath = getNodePath(project.getAreaRootNodes().getNodes(), areaUri);

            index++;
        }

        return areaPath;
    }
}
