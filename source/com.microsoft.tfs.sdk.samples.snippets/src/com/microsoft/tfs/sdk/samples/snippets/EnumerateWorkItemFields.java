// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

public class EnumerateWorkItemFields {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final Project project = tpc.getWorkItemClient().getProjects().get(SnippetSettings.PROJECT_NAME);

        // Get the bug work item type definition.
        final WorkItemType bugWorkItemType = project.getWorkItemTypes().get("Bug"); //$NON-NLS-1$

        // Enumerate the work item types for this project.
        for (final FieldDefinition fieldDefinition : bugWorkItemType.getFieldDefinitions()) {
            System.out.println(fieldDefinition.getReferenceName() + "[" + fieldDefinition.getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
