// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildServer;

public class EnumerateBuildQualities {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final IBuildServer buildServer = tpc.getBuildServer();
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
