// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;

public class FindBuildDefinitions {
    public static void main(final String[] args) {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final IBuildServer buildServer = tpc.getBuildServer();

        //
        // Scenario 1: Query all definitions in the specified project.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Query for build definitions by project name"); //$NON-NLS-1$
        System.out.println("***"); //$NON-NLS-1$

        IBuildDefinition[] buildDefinitions = buildServer.queryBuildDefinitions(SnippetSettings.PROJECT_NAME);
        System.out.println("Found " + buildDefinitions.length + " build definition(s)."); //$NON-NLS-1$ //$NON-NLS-2$
        for (final IBuildDefinition buildDefinition : buildDefinitions) {
            displayBuildDefinitionProperties(buildDefinition);
        }

        //
        // Scenario 2: Query definitions in a specified project by type.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Query build definitions of type GATED"); //$NON-NLS-1$
        System.out.println("***"); //$NON-NLS-1$

        final IBuildDefinitionSpec buildDefinitionSpec =
            buildServer.createBuildDefinitionSpec(SnippetSettings.PROJECT_NAME);
        buildDefinitionSpec.setContinuousIntegrationType(ContinuousIntegrationType.GATED);
        final IBuildDefinitionQueryResult queryResults = buildServer.queryBuildDefinitions(buildDefinitionSpec);
        buildDefinitions = queryResults.getDefinitions();

        System.out.println("Found " + buildDefinitions.length + " gated build definition(s)."); //$NON-NLS-1$ //$NON-NLS-2$
        for (final IBuildDefinition buildDefinition : buildDefinitions) {
            displayBuildDefinitionProperties(buildDefinition);
        }

        //
        // Scenario 3: Get an individual build definition by name.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Get build definition by name"); //$NON-NLS-1$
        System.out.println("***"); //$NON-NLS-1$

        IBuildDefinition buildDefinition;
        buildDefinition =
            buildServer.getBuildDefinition(SnippetSettings.PROJECT_NAME, SnippetSettings.BUILD_DEFINITION_NAME);
        displayBuildDefinitionProperties(buildDefinition);

        //
        // Scenario 4: Get an individual build definition by URI
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Get build definition by URI"); //$NON-NLS-1$
        System.out.println("***"); //$NON-NLS-1$

        if (buildDefinitions.length > 0) {
            final String aBuildDefinitionUri = buildDefinitions[0].getURI();
            buildDefinition = buildServer.getBuildDefinition(aBuildDefinitionUri);
            displayBuildDefinitionProperties(buildDefinition);
        }

        //
        // Scenario 5: Get an individual build definition by name with
        // options to control the amount of data retrieved for the
        // definition.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Get build definition by name with options"); //$NON-NLS-1$
        System.out.println("***"); //$NON-NLS-1$

        buildDefinition = buildServer.getBuildDefinition(
            SnippetSettings.PROJECT_NAME,
            SnippetSettings.BUILD_DEFINITION_NAME,
            QueryOptions.DEFINITIONS);

        displayBuildDefinitionProperties(buildDefinition);

        //
        // Scenario 6: Get an individual build definition by URI with
        // options to control the amount of data retrieved for the
        // definition.
        //
        System.out.println("***"); //$NON-NLS-1$
        System.out.println("*** Get build definition by URI with options"); //$NON-NLS-1$
        System.out.println("***"); //$NON-NLS-1$

        if (buildDefinitions.length > 0) {
            final String aBuildDefinitionURI = buildDefinitions[0].getURI();
            buildDefinition = buildServer.getBuildDefinition(aBuildDefinitionURI, QueryOptions.DEFINITIONS);
            displayBuildDefinitionProperties(buildDefinition);
        }
    }

    private static void displayBuildDefinitionProperties(final IBuildDefinition buildDefinition) {
        System.out.println("Build Definition"); //$NON-NLS-1$
        System.out.println("\tName: " + buildDefinition.getName()); //$NON-NLS-1$
        System.out.println("\tURI: " + buildDefinition.getURI()); //$NON-NLS-1$
        System.out.println("\tDescription: " + buildDefinition.getDescription()); //$NON-NLS-1$
        System.out.println("\tEnabled: " + buildDefinition.isEnabled()); //$NON-NLS-1$
        System.out.println("\tDrop Folder: " + buildDefinition.getDefaultDropLocation()); //$NON-NLS-1$
        System.out.println(""); //$NON-NLS-1$
    }
}
