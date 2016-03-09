// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IProcessTemplate;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;

public class CreateBuildDefinition {
    public static void main(final String[] args) throws Exception {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final IBuildServer buildServer = tpc.getBuildServer();

        // Check if the build server version is supported
        if (SnippetSettings.isLessThanV3BuildServer(buildServer)) {
            return;
        }

        // Find a build controller.
        final IBuildController[] buildControllers = buildServer.queryBuildControllers();
        if (buildControllers.length == 0) {
            throw new Exception("no build controllers"); //$NON-NLS-1$
        }

        // Find a process template.
        final IProcessTemplate[] processTemplates = buildServer.queryProcessTemplates(SnippetSettings.PROJECT_NAME);
        if (processTemplates.length == 0) {
            throw new Exception("no process templates"); //$NON-NLS-1$
        }

        final IBuildDefinition buildDefinition = buildServer.createBuildDefinition(SnippetSettings.PROJECT_NAME);
        buildDefinition.setName("Created by " //$NON-NLS-1$
            + CreateBuildDefinition.class.getSimpleName()
            + " (" //$NON-NLS-1$
            + System.currentTimeMillis()
            + ")"); //$NON-NLS-1$
        buildDefinition.setDescription("description of build definition"); //$NON-NLS-1$
        buildDefinition.setContinuousIntegrationType(ContinuousIntegrationType.NONE);
        buildDefinition.setBuildController(buildControllers[0]);
        buildDefinition.setDefaultDropLocation(SnippetSettings.BUILD_DROP_LOCATION);
        buildDefinition.setEnabled(true);
        buildDefinition.setProcess(processTemplates[0]);

        buildDefinition.save();
        System.out.println("Created build definition " + buildDefinition.getURI()); //$NON-NLS-1$

        buildServer.deleteBuildDefinitions(new IBuildDefinition[] {
            buildDefinition
        });
        System.out.println("Deleted build definition " + buildDefinition.getURI()); //$NON-NLS-1$
    }
}
