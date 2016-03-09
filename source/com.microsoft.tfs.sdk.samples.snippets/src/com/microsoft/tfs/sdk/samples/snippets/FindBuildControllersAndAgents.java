// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildAgent;
import com.microsoft.tfs.core.clients.build.IBuildAgentQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildAgentSpec;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildControllerQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildControllerSpec;
import com.microsoft.tfs.core.clients.build.IBuildServer;

public class FindBuildControllersAndAgents {
    public static void main(final String[] args) throws Exception {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final IBuildServer buildServer = tpc.getBuildServer();

        // Check if the build server version is supported
        if (SnippetSettings.isLessThanV3BuildServer(buildServer)) {
            return;
        }

        //
        // Scenario 1: Retrieve all controllers with all agent information
        // included.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Retrieve all build controllers and agents"); //$NON-NLS-1$
        final IBuildController[] buildControllers = buildServer.queryBuildControllers(true);
        if (buildControllers.length == 0) {
            throw new Exception("no build controllers found"); //$NON-NLS-1$
        }

        displayBuildControllers(buildControllers);

        //
        // Scenario 2: Retrieve an individual controller by name.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Retrieve a build controller by name"); //$NON-NLS-1$
        final String aBuildControllerName = buildControllers[0].getName();
        IBuildController buildController = buildServer.getBuildController(aBuildControllerName);
        if (buildController == null) {
            throw new Exception("build controller not found"); //$NON-NLS-1$
        }

        displayBuildControllerProperties(buildController);

        //
        // Scenario 3: Retrieve an individual controller by URI.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Retrieve a build controller by URI"); //$NON-NLS-1$
        final String aBuildControllerUri = buildControllers[0].getURI();
        buildController = buildServer.getBuildController(aBuildControllerUri, false);
        if (buildController == null) {
            throw new Exception("Build controller not found"); //$NON-NLS-1$
        }

        displayBuildControllerProperties(buildController);

        //
        // Scenario 4: Retrieve an individual build agent by URI.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Retrieving a build agent by URI"); //$NON-NLS-1$
        buildController = buildControllers[0];
        final IBuildAgent[] buildAgents = buildController.getAgents();

        if (buildAgents == null) {
            throw new Exception("build agents not found"); //$NON-NLS-1$
        }

        final String anAgentUri = buildController.getAgents()[0].getURI();
        final IBuildAgent buildAgent = buildServer.getBuildAgent(anAgentUri);
        if (buildAgent == null) {
            throw new Exception("build agent not found"); //$NON-NLS-1$
        }

        System.out.println("Build agent URI: " + anAgentUri); //$NON-NLS-1$
        displayBuildAgentProperties(buildAgent);

        //
        // Scenario 5: Query build controllers using a BuildControllerSpec.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Query build controllers using a BuildControllerSpec"); //$NON-NLS-1$
        final IBuildControllerSpec buildControllerSpec = buildServer.createBuildControllerSpec();
        buildControllerSpec.setServiceHostName("*"); // all //$NON-NLS-1$
                                                     // controllers
        buildControllerSpec.setName("*"); // any controller name //$NON-NLS-1$
        buildControllerSpec.setIncludeAgents(false); // just controller data
        final IBuildControllerQueryResult controllerResult = buildServer.queryBuildControllers(buildControllerSpec);

        displayBuildControllers(controllerResult.getControllers());

        //
        // Scenario 6: Query build agents using a BuildAgentSpec.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Query build agents using a BuildAgentSpec"); //$NON-NLS-1$
        final IBuildAgentSpec buildAgentSpec = buildServer.createBuildAgentSpec();
        buildAgentSpec.setServiceHostName("*"); // all services //$NON-NLS-1$
        buildAgentSpec.setName("*"); // any agent name //$NON-NLS-1$
        final IBuildAgentQueryResult agentResult = buildServer.queryBuildAgents(buildAgentSpec);

        displayBuildAgents(agentResult.getAgents());
    }

    private static void displayBuildControllerProperties(final IBuildController buildController) {
        System.out.println("Build Controller"); //$NON-NLS-1$
        System.out.println("\tName: " + buildController.getName()); //$NON-NLS-1$
        System.out.println("\tURI: " + buildController.getURI()); //$NON-NLS-1$
        System.out.println("\tDescription: " + buildController.getDescription()); //$NON-NLS-1$
        System.out.println("\tEnabled: " + buildController.isEnabled()); //$NON-NLS-1$
        System.out.println("\tStatus: " + buildController.getStatus().toString()); //$NON-NLS-1$
        System.out.println();
    }

    private static void displayBuildAgentProperties(final IBuildAgent buildAgent) {
        System.out.println("\tBuild Agent"); //$NON-NLS-1$
        System.out.println("\t\tName: " + buildAgent.getName()); //$NON-NLS-1$
        System.out.println("\t\tURI: " + buildAgent.getURI()); //$NON-NLS-1$
        System.out.println("\t\tDescription: " + buildAgent.getDescription()); //$NON-NLS-1$
        System.out.println("\t\tBuild Directory: " + buildAgent.getBuildDirectory()); //$NON-NLS-1$
        System.out.println("\t\tEnabled: " + buildAgent.isEnabled()); //$NON-NLS-1$
        System.out.println("\t\tReserved: " + buildAgent.isReserved()); //$NON-NLS-1$
        System.out.println("\t\tStatus: " + buildAgent.getStatus().toString()); //$NON-NLS-1$

        System.out.print("\t\tTags: "); //$NON-NLS-1$
        for (final String tag : buildAgent.getTags()) {
            System.out.print(tag + ", "); //$NON-NLS-1$
        }
        System.out.println();
        System.out.println();
    }

    private static void displayBuildControllers(final IBuildController[] buildControllers) {
        for (final IBuildController buildController : buildControllers) {
            displayBuildControllerProperties(buildController);
            displayBuildAgents(buildController.getAgents());
        }
    }

    private static void displayBuildAgents(final IBuildAgent[] buildAgents) {
        for (final IBuildAgent buildAgent : buildAgents) {
            displayBuildAgentProperties(buildAgent);
        }
    }
}
