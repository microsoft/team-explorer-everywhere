// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.project.Project;

public class EnumerateWorkItemTeamProjects {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        // Enumerate the Team Projects.
        for (final Project project : tpc.getWorkItemClient().getProjects()) {
            System.out.println(project.getName());
        }
    }
}
