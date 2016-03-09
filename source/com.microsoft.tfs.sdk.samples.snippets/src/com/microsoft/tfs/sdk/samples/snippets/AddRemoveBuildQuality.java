// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildServer;

public class AddRemoveBuildQuality {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final IBuildServer buildServer = tpc.getBuildServer();
        System.out.println("Before change"); //$NON-NLS-1$
        showBuildQualities(buildServer, SnippetSettings.PROJECT_NAME);

        // Define a couple new build qualities.
        final String quality1 = "My Build Quality 1"; //$NON-NLS-1$
        final String quality2 = "My Build Quality 2"; //$NON-NLS-1$

        // Add a new build quality to the project.
        buildServer.addBuildQuality(SnippetSettings.PROJECT_NAME, quality1);
        System.out.println();
        System.out.println("After adding build quality"); //$NON-NLS-1$
        showBuildQualities(buildServer, SnippetSettings.PROJECT_NAME);

        // Remove the new build quality from the project
        buildServer.deleteBuildQuality(SnippetSettings.PROJECT_NAME, quality1);
        System.out.println();
        System.out.println("After removing build quality"); //$NON-NLS-1$
        showBuildQualities(buildServer, SnippetSettings.PROJECT_NAME);

        // Add two new build qualities to the project.
        buildServer.addBuildQuality(SnippetSettings.PROJECT_NAME, new String[] {
            quality1,
            quality2
        });
        System.out.println();
        System.out.println("After adding 2 build qualities"); //$NON-NLS-1$
        showBuildQualities(buildServer, SnippetSettings.PROJECT_NAME);

        // Remove the two new build qualities from the project.
        buildServer.deleteBuildQuality(SnippetSettings.PROJECT_NAME, new String[] {
            quality1,
            quality2
        });
        System.out.println();
        System.out.println("After removing 2 build qualities"); //$NON-NLS-1$
        showBuildQualities(buildServer, SnippetSettings.PROJECT_NAME);
    }

    private static void showBuildQualities(final IBuildServer buildServer, final String projectName) {
        final String[] buildQualities = buildServer.getBuildQualities(projectName);
        System.out.println("Build Qualities:"); //$NON-NLS-1$

        int count = 1;
        for (final String buildQuality : buildQualities) {
            System.out.println("\t[" + count++ + "]" + buildQuality); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
