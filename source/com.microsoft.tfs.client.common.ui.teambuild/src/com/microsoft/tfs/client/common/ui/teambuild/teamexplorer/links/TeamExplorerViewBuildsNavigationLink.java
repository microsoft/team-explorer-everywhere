// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.links;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.link.TeamExplorerBaseNavigationLink;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;

public class TeamExplorerViewBuildsNavigationLink extends TeamExplorerBaseNavigationLink {
    @Override
    public boolean isEnabled(final TeamExplorerContext context) {
        return context.isConnected()
            && context.getServer() != null
            && context.getCurrentProjectInfo() != null
            && context.getCurrentProjectInfo().getName() != null
            && context.getBuildServer() != null;
    }

    @Override
    public void onClick(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {
        final String projectName = context.getCurrentProjectInfo().getName();
        final TFSTeamProjectCollection collection = context.getServer().getConnection();
        final IBuildDefinition buildDefinition = context.getBuildServer().createBuildDefinition(projectName);
        buildDefinition.setName(BuildPath.RECURSION_OPERATOR);

        final BuildExplorer buildExplorer = BuildHelpers.openBuildExplorer(collection, buildDefinition);
        if (buildExplorer != null) {
            buildExplorer.showOnlyMyBuildsView();
        }
    }
}
