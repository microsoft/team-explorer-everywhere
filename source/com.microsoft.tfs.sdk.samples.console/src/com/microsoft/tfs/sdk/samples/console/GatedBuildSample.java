// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IProcessTemplate;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ActionDeniedBySubscriberException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.GatedCheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;

/**
 * This sample demonstrates how to queue a build to satisfy gated build
 * requirements. Multiple build definitions are configured which each affect the
 * files being checked in, and the sample catches
 * {@link ActionDeniedBySubscriberException} and queues a build for the first
 * definition to complete the check-in.
 */
public class GatedBuildSample {
    public static String PREFERRED_SEPARATOR_CHARACTER = "/"; //$NON-NLS-1$
    public static String BUILD_TYPE_FOLDER_NAME = "TeamBuildTypes"; //$NON-NLS-1$
    public static FileEncoding ENCODING = null;
    public static int TIME_OUT_COUNTER = 200;

    public static void main(final String[] args) throws InterruptedException {
        /*
         * NOTE: This sample omits some recommended SDK initialization steps for
         * clarity. See ConnectionAdvisorSample and LogConfigurationSample for
         * important information on initializing the TFS SDK for Java before
         * usage.
         */

        TFSTeamProjectCollection tpc = null;
        Workspace workspace = null;
        IBuildDefinition firstGatedBuildDef = null;
        IBuildDefinition secondGatedBuildDef = null;
        File addedFile = null;

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

            // Create and map a new workspace
            workspace = createAndMapWorkspace(tpc);

            // Get latest on a project
            getLatest(workspace);

            // Create 2 gated checkin build definitions
            firstGatedBuildDef = createGatedCheckinBuildDefinition(tpc, GatedBuildSample.class.getSimpleName()
                + "-1_" //$NON-NLS-1$
                + Calendar.getInstance().getTimeInMillis());
            secondGatedBuildDef = createGatedCheckinBuildDefinition(tpc, GatedBuildSample.class.getSimpleName()
                + "-2_" //$NON-NLS-1$
                + Calendar.getInstance().getTimeInMillis());

            // Add or edit a file in the mapped workspace
            addedFile = addOrEditFile(workspace);

            // Checkin the pending change and queue a new build of the first
            // definition
            final IQueuedBuild queuedBuild = doGatedCheckin(workspace, tpc.getBuildServer(), firstGatedBuildDef);

            // Check the position of the build in the queue
            System.out.println("Build position in queue is " + queuedBuild.getQueuePosition()); //$NON-NLS-1$

            // Wait for the build to finish.
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
                    + buildDetail.getBuildServer().getDisplayText(buildStatus));
            }

        } finally {
            // Clean up the workspace
            if (workspace != null && addedFile != null) {
                cleanupWorkspace(workspace, addedFile);
            }

            // Delete the builds and build definitions for clean up
            if (tpc != null && workspace != null) {
                if (firstGatedBuildDef != null) {
                    deleteBuildDefinition(tpc.getBuildServer(), firstGatedBuildDef);
                }

                if (secondGatedBuildDef != null) {
                    deleteBuildDefinition(tpc.getBuildServer(), secondGatedBuildDef);
                }

                tpc.getVersionControlClient().deleteWorkspace(workspace);
                System.out.println("Deleted the workspace"); //$NON-NLS-1$
            }
        }
    }

    public static Workspace createAndMapWorkspace(final TFSTeamProjectCollection tpc) {
        final String workspaceName = "SampleGatedBuildWorkspace" + System.currentTimeMillis(); //$NON-NLS-1$
        Workspace workspace = null;

        // Get the workspace
        workspace = tpc.getVersionControlClient().tryGetWorkspace(ConsoleSettings.MAPPING_LOCAL_PATH);

        // Create and map the workspace if it does not exist
        if (workspace == null) {
            workspace = tpc.getVersionControlClient().createWorkspace(
                null,
                workspaceName,
                "Sample workspace comment", //$NON-NLS-1$
                WorkspaceLocation.SERVER,
                null);

            // Map the workspace
            final WorkingFolder workingFolder = new WorkingFolder(
                ConsoleSettings.MAPPING_SERVER_PATH,
                LocalPath.canonicalize(ConsoleSettings.MAPPING_LOCAL_PATH));
            workspace.createWorkingFolder(workingFolder);
        }

        System.out.println("Workspace '" + workspaceName + "' now exists and is mapped"); //$NON-NLS-1$ //$NON-NLS-2$

        return workspace;
    }

    public static void getLatest(final Workspace workspace) {
        final ItemSpec spec = new ItemSpec(ConsoleSettings.MAPPING_LOCAL_PATH, RecursionType.FULL);
        final GetRequest request = new GetRequest(spec, LatestVersionSpec.INSTANCE);
        workspace.get(request, GetOptions.NONE);
    }

    public static IBuildDefinition createGatedCheckinBuildDefinition(
        final TFSTeamProjectCollection tpc,
        final String buildName) {
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

        // Set the trigger to gated checkin
        buildDefinition.setContinuousIntegrationType(ContinuousIntegrationType.GATED);
        buildDefinition.setContinuousIntegrationQuietPeriod(0);

        // Save the build definition
        buildDefinition.save();

        System.out.println("Created build definition " + buildDefinition.getName()); //$NON-NLS-1$

        return buildDefinition;
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

    public static File addOrEditFile(final Workspace workspace) {

        final File file = new File(ConsoleSettings.MAPPING_LOCAL_PATH, "SampleAppFile" + System.currentTimeMillis()); //$NON-NLS-1$

        // If the file exists edit it
        if (file.exists()) {
            final ItemSpec fileSpec = new ItemSpec(file.getAbsolutePath(), RecursionType.NONE);
            workspace.pendEdit(
                new ItemSpec[] {
                    fileSpec
            },
                LockLevel.UNCHANGED,
                ENCODING,

                GetOptions.NONE,
                PendChangesOptions.NONE);
            writeFileContents(file, Calendar.getInstance().getTime().toString(), "UTF-8"); //$NON-NLS-1$
        } else {
            // If the file does not exist add it
            writeFileContents(file, "Sample File", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$

            // Pend an add
            // The encoding is passed as null and the add will detect the
            // encoding
            // of the file

            try {
                workspace.pendAdd(new String[] {
                    file.getCanonicalPath()
                }, false, ENCODING, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);
            } catch (final IOException e) {
                e.printStackTrace();
            }

        }
        return file;
    }

    public static IQueuedBuild doGatedCheckin(
        final Workspace workspace,
        final IBuildServer buildServer,
        final IBuildDefinition buildDefinition) {
        IQueuedBuild build = null;
        final PendingSet pendingSet = workspace.getPendingChanges();

        if (pendingSet != null) {
            final PendingChange[] pendingChanges = pendingSet.getPendingChanges();
            if (pendingChanges != null) {
                try {
                    workspace.checkIn(pendingChanges, "GatedBuildSample comment"); //$NON-NLS-1$
                } catch (final ActionDeniedBySubscriberException e) {
                    // Checkin will fail because it affects gated checkin build
                    // definitions
                    final GatedCheckinException gatedCheckinException = new GatedCheckinException(e);
                    build = queueBuddyBuild(buildServer, buildDefinition, gatedCheckinException.getShelvesetName());
                    System.out.println("A new build of build definition '" //$NON-NLS-1$
                        + buildDefinition.getName()
                        + "' queued for shelveset '" //$NON-NLS-1$
                        + gatedCheckinException.getShelvesetName()
                        + "'"); //$NON-NLS-1$

                }

            }

        }

        return build;
    }

    public static IQueuedBuild queueBuddyBuild(
        final IBuildServer buildServer,
        final IBuildDefinition buildDefinition,
        String shelvesetName) {
        // Create a new build request

        final IBuildRequest request = buildDefinition.createBuildRequest();

        // Parse the specified specification into a name and user parts.
        // Specify the current authorized TFS user as the fallback in
        // the case no user is specified.
        final WorkspaceSpec spec = WorkspaceSpec.parse(shelvesetName, ConsoleSettings.USERNAME);

        // Reset the sheleveset name with the full specification.
        shelvesetName = spec.toString();

        request.setShelvesetName(shelvesetName);

        // Set the build reason to checkin the pending changes when build
        // succeeds
        request.setReason(BuildReason.CHECK_IN_SHELVESET);

        // Queue a new build
        return buildServer.queueBuild(request);

    }

    /**
     * Undo the pending changes in the specified workspace for the specified
     * file for clean up
     *
     *
     * @param workspace
     *        The workspace with the pending changes
     * @param file
     *        The file with the pending changes
     *
     *
     */
    public static void cleanupWorkspace(final Workspace workspace, final File file) {
        // Undo the pending changes and get latest
        final ItemSpec spec = new ItemSpec(file.getAbsolutePath(), RecursionType.NONE);
        workspace.undo(new ItemSpec[] {
            spec
        }, GetOptions.NONE);

        System.out.println("Reconciled the workspace"); //$NON-NLS-1$

    }

    /**
     * Write the specified contents to the given file
     *
     * @param file
     *        File to write to
     *
     * @param contents
     *        Contents to write to the file
     *
     * @param encoding
     *        File encoding to use (null for default)
     *
     * @throws IOException
     */
    public static void writeFileContents(final File file, final String contents, final String encoding) {
        if (file.exists()) {
            setReadOnly(file, false);
        }

        BufferedWriter bw = null;
        try {
            try {
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
                bw.write(contents);
            } finally {
                bw.close();
            }
        } catch (final Throwable t) {
            throw new RuntimeException("Failed to write contents to file " + file, t); //$NON-NLS-1$
        }
    }

    /**
     * Set or clear the readonly bit of the specified file
     *
     * @param file
     *        File to set/unset as readonly
     *
     * @param readOnly
     *        If true, make read-only. If false, make writable
     */
    public static void setReadOnly(final File file, final boolean readOnly) {
        final FileSystemAttributes attr = FileSystemUtils.getInstance().getAttributes(file);

        if (readOnly != attr.isReadOnly()) {
            attr.setReadOnly(readOnly);
            FileSystemUtils.getInstance().setAttributes(file, attr);
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

}
