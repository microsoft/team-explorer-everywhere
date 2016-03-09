// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

public class QueuePrivateBuild {
    public static void main(final String[] args) throws InterruptedException {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final IBuildServer buildServer = tpc.getBuildServer();
        final IBuildDefinition buildDefinition =
            buildServer.getBuildDefinition(SnippetSettings.PROJECT_NAME, SnippetSettings.BUILD_DEFINITION_NAME);

        // Create a build request for a private build based on a shelveset.
        final IBuildRequest buildRequest = buildDefinition.createBuildRequest();
        buildRequest.setShelvesetName("MyShelvesetName;" //$NON-NLS-1$
            + tpc.getAuthorizedDomainName()
            + "\\" //$NON-NLS-1$
            + tpc.getAuthorizedAccountName());
        buildRequest.setReason(BuildReason.VALIDATE_SHELVESET);

        final IQueuedBuild queuedBuild = buildServer.queueBuild(buildRequest);
        waitForQueuedBuildToFinish(queuedBuild);

        if (queuedBuild.getStatus().contains(QueueStatus.COMPLETED)) {
            // Display the status of the completed build.
            final IBuildDetail buildDetail = queuedBuild.getBuild();
            final BuildStatus buildStatus = buildDetail.getStatus();

            System.out.println("Build " //$NON-NLS-1$
                + buildDetail.getBuildNumber()
                + " completed with status " //$NON-NLS-1$
                + buildServer.getDisplayText(buildStatus));
        } else {
            System.out.println("Build canceled or did not finish in time."); //$NON-NLS-1$
        }
    }

    private static void waitForQueuedBuildToFinish(final IQueuedBuild queuedBuild) throws InterruptedException {
        // Wait for the build to finish.
        System.out.print("Waiting for build to finish"); //$NON-NLS-1$
        do {
            Thread.sleep(2000);
            System.out.print("."); //$NON-NLS-1$
            queuedBuild.refresh(QueryOptions.ALL);
        } while (queuedBuild.getBuild() == null || !queuedBuild.getBuild().isBuildFinished());

        System.out.println();
    }
}
