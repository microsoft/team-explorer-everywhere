// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

/**
 * Base class for the build definition {@link ToolStripTabPage}.
 */
public abstract class BuildDefinitionTabPage implements ToolStripTabPage {

    private final IBuildDefinition buildDefinition;

    public BuildDefinitionTabPage(final IBuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
    }

    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

}
