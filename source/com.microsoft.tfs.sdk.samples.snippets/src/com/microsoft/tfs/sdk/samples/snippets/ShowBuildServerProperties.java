// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.flags.BuildServerVersion;

public class ShowBuildServerProperties {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final IBuildServer buildServer = tpc.getBuildServer();

        // Show the build server version number.
        final BuildServerVersion version = buildServer.getBuildServerVersion();
        System.out.println("Build server version: " + version.getVersion()); //$NON-NLS-1$

        // Show all localized display strings for BuildServerVersion.
        showDisplayValues(buildServer);
    }

    private static void showDisplayValues(final IBuildServer buildServer) {
        System.out.println("Display values for BuildServerVersion"); //$NON-NLS-1$
        for (final String displayValue : buildServer.getDisplayTextValues(BuildServerVersion.class)) {
            System.out.println(displayValue);
        }
    }
}
