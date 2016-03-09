// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.IBuildServer;

public class EditBuildDefinition {
    public static void main(final String[] args) throws Exception {
        final TFSTeamProjectCollection tpc = SnippetSettings.connectToTFS();

        final IBuildServer buildServer = tpc.getBuildServer();

        final IBuildDefinitionSpec buildDefinitionSpec =
            buildServer.createBuildDefinitionSpec(SnippetSettings.PROJECT_NAME);
        buildDefinitionSpec.setName(SnippetSettings.BUILD_DEFINITION_NAME);

        final IBuildDefinitionQueryResult queryResult = buildServer.queryBuildDefinitions(buildDefinitionSpec);
        final IBuildDefinition[] buildDefinitions = queryResult.getDefinitions();
        if (buildDefinitions.length == 0) {
            throw new Exception("Build definition was not found"); //$NON-NLS-1$
        }

        // Toggle the enabled flag and save the definition.
        final IBuildDefinition buildDefinition = buildDefinitions[0];
        buildDefinition.setEnabled(!buildDefinition.isEnabled());
        buildDefinition.save();

        // Set it back
        buildDefinition.setEnabled(!buildDefinition.isEnabled());
        buildDefinition.save();

        System.out.print("Build definition '" + buildDefinition.getName() + "' was edited."); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
