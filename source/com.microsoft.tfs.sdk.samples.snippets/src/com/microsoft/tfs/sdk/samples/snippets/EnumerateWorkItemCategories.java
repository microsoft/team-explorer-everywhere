// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.category.Category;
import com.microsoft.tfs.core.clients.workitem.project.Project;

public class EnumerateWorkItemCategories {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final Project project = tpc.getWorkItemClient().getProjects().get(SnippetSettings.PROJECT_NAME);

        // Enumerate the work item categories for this project.
        for (final Category category : project.getCategories()) {
            System.out.println(category.getName());
        }
    }
}
