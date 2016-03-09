// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.events;

import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public class BuildDefinitionEventArg extends TeamExplorerEventArg {
    private final IBuildDefinition buildDefinition;

    public BuildDefinitionEventArg(final IBuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
    }

    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }
}
