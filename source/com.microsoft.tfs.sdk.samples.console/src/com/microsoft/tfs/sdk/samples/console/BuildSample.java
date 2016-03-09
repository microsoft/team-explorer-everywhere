// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import java.util.Calendar;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IProcessTemplate;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;

/**
 * This sample demonstrates creating a build definition, queuing a build for
 * that definition, waiting on build completion, printing completed builds, and
 * deleting builds and definitions.
 */
public class BuildSample {
    public static int TIME_OUT_COUNTER = 200;

    public static void main(final String[] args) throws InterruptedException {
        /*
         * NOTE: This sample omits some recommended SDK initialization steps for
         * clarity. See ConnectionAdvisorSample and LogConfigurationSample for
         * important information on initializing the TFS SDK for Java before
         * usage.
         */

        TFSTeamProjectCollection tpc = null;
        IBuildDefinition buildDefinition = null;

        try {
            // Connect to TFS
            tpc = ConsoleSettings.connectToTFS();

            // Verify that the tfs server has a build service
            if (!isBuildServiceConfigured(tpc.getBuildServer())) {
                return;
            }

            // Check if the build server version is supported
            if (ConsoleSettings.isLessThanV3BuildServer(tpc.getBuildServer())) {
                return;
            }

            // Create a new build definition
            buildDefinition = createBuildDefinition(tpc, BuildSample.class.getSimpleName()
                + "_" //$NON-NLS-1$
                + Calendar.getInstance().getTimeInMillis());

            // Queue a new build
            final IQueuedBuild queuedBuild = queueBuild(tpc, buildDefinition);

            // wait for the build to finish
            waitForBuildToFinish(tpc, queuedBuild);

            // Query builds
            final IBuildDetail[] builds = queryBuilds(buildDefinition);

            // Print builds
            printBuilds(builds);

        } finally {
            // Delete the build definition for clean up
            if (tpc != null && buildDefinition != null) {
                deleteBuildDefinition(tpc.getBuildServer(), buildDefinition);
            }
        }
    }

    public static IBuildDetail[] queryBuilds(final IBuildDefinition buildDefinition) {
        return buildDefinition.queryBuilds();
    }

    public static IBuildDefinition createBuildDefinition(final TFSTeamProjectCollection tpc, final String buildName) {
        // Create a new build definition
        final IBuildDefinition buildDefinition =
            tpc.getBuildServer().createBuildDefinition(ConsoleSettings.PROJECT_NAME);

        // Set the name of the build definition
        buildDefinition.setName(buildName);

        // Enable the build definition
        buildDefinition.setEnabled(true);

        // Set the workspace mapping
        buildDefinition.getWorkspace().addMapping(
            ConsoleSettings.MAPPING_SERVER_PATH,
            "$(SourceDir)", //$NON-NLS-1$
            WorkspaceMappingType.MAP,
            WorkspaceMappingDepth.FULL);

        // Set the build controller
        buildDefinition.setBuildController(getAvailableBuildController(tpc.getBuildServer()));

        // Set the drop location
        buildDefinition.setDefaultDropLocation(ConsoleSettings.BUILD_DROP_LOCATION);

        // Set the process template
        final IProcessTemplate processTemplate = getUpgradeProcessTemplate(tpc.getBuildServer());
        if (processTemplate != null) {
            buildDefinition.setProcess(processTemplate);
        }

        // Set the build config file
        buildDefinition.setConfigurationFolderPath(ConsoleSettings.BUILD_CONFIG_FOLDER_PATH);

        // Save it
        buildDefinition.save();

        System.out.println("Created build " + buildName); //$NON-NLS-1$

        return buildDefinition;
    }

    public static IQueuedBuild queueBuild(final TFSTeamProjectCollection tpc, final IBuildDefinition buildDefinition) {
        final IQueuedBuild queuedBuild = tpc.getBuildServer().queueBuild(buildDefinition);
        System.out.println("Queued a new build for build definition " + buildDefinition.getName()); //$NON-NLS-1$
        return queuedBuild;
    }

    public static void waitForBuildToFinish(final TFSTeamProjectCollection tpc, final IQueuedBuild queuedBuild)
        throws InterruptedException {
        System.out.println("Waiting for build to finish"); //$NON-NLS-1$
        int timeOutIndex = 0;
        do {
            Thread.sleep(2000);
            queuedBuild.refresh(QueryOptions.NONE);
            System.out.println("Build status is " + queuedBuild.getStatus().toString()); //$NON-NLS-1$
            timeOutIndex++;
        } while (!queuedBuild.getStatus().contains(QueueStatus.COMPLETED) && timeOutIndex < TIME_OUT_COUNTER);

        // Check if the build wait timed out
        if (!queuedBuild.getStatus().contains(QueueStatus.COMPLETED)) {
            System.out.println("The build wait timed out"); //$NON-NLS-1$
        } else {

            // Display the status of the completed build.
            final IBuildDetail buildDetail = queuedBuild.getBuild();
            final BuildStatus buildStatus = buildDetail.getStatus();

            System.out.println("Build " //$NON-NLS-1$
                + buildDetail.getBuildNumber()
                + " completed with status " //$NON-NLS-1$
                + queuedBuild.getBuildServer().getDisplayText(buildStatus));
        }
    }

    public static boolean isBuildServiceConfigured(final IBuildServer buildServer) {
        final IBuildController[] controllers =
            TeamBuildCache.getInstance(buildServer, ConsoleSettings.PROJECT_NAME).getBuildControllers(false);
        if (controllers == null || controllers.length == 0) {
            System.out.println("This server does not have a build service configured"); //$NON-NLS-1$
            return false;
        }

        return true;
    }

    public static void printBuilds(final IBuildDetail[] builds) {
        for (final IBuildDetail build : builds) {
            System.out.println("BuildNumber: " + build.getBuildNumber() + ", Status: " + build.getStatus().toString()); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public static void deleteBuildDefinition(final IBuildServer buildServer, final IBuildDefinition definition) {
        // Delete existing builds
        deleteBuilds(buildServer, definition);

        // Delete build definition
        definition.delete();

        System.out.println("Deleted build definition: " + definition.getName()); //$NON-NLS-1$
    }

    /**
     * Deletes all build of the specified build definition
     *
     * @param buildDefinition
     *        The build definition whose builds should be deleted
     */
    public static void deleteBuilds(final IBuildServer buildServer, final IBuildDefinition buildDefinition) {
        /*
         * Try to stop any builds in progress
         */
        final IBuildDetail[] details = buildDefinition.queryBuilds();
        for (final IBuildDetail detail : details) {
            if (!detail.isBuildFinished()) {
                try {
                    detail.stop();
                } catch (final Throwable t) {
                    System.out.println("Failed to stop in-progress build: " + t.getMessage()); //$NON-NLS-1$
                }
            }
        }

        /*
         * Delete the builds
         */
        buildServer.deleteBuilds(details);

        System.out.println("Deleted all builds of build definition : " + buildDefinition.getName()); //$NON-NLS-1$
    }

    /**
     * Queries for build controllers and returns the first available one
     *
     * @return IBuildController
     */
    public static IBuildController getAvailableBuildController(final IBuildServer buildServer) {
        // Retrieve all the build controllers
        final IBuildController[] buildControllers = buildServer.queryBuildControllers();

        // Return the first available build controller
        for (final IBuildController buildController : buildControllers) {
            if (buildController.isEnabled()) {
                return buildController;
            }
        }

        return null;
    }

    public static IProcessTemplate getUpgradeProcessTemplate(final IBuildServer buildServer) {
        // Find the upgrade template
        final IProcessTemplate[] templates =
            buildServer.queryProcessTemplates(ConsoleSettings.PROJECT_NAME, new ProcessTemplateType[] {
                ProcessTemplateType.UPGRADE
        });

        if (templates.length == 0) {

            return null;
        }

        return templates[0];
    }
}
