// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.teamfoundation.build.webapi.model.BuildDefinition;
import com.microsoft.teamfoundation.build.webapi.model.DefinitionType;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.BuildFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.core.TFSTeamProjectCollection;

public abstract class BuildDefinitionVNextAction extends TeamExplorerBaseAction {
    protected BuildDefinition selectedDefinition;

    protected BuildDefinition getSelectedBuildDefinition() {
        return selectedDefinition;
    }

    protected TFSTeamProjectCollection getConnection() {
        return getContext().getServer().getConnection();
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            if (BuildExplorer.getInstance() != null && !BuildExplorer.getInstance().isConnected()) {
                action.setEnabled(false);
                return;
            }

            if (selection instanceof IStructuredSelection) {
                final Object o = ((IStructuredSelection) selection).getFirstElement();

                if (o instanceof BuildFavoriteItem) {
                    final BuildFavoriteItem favorite = (BuildFavoriteItem) o;

                    if (favorite.getBuildDefinitionType() == DefinitionType.BUILD) {
                        selectedDefinition = (BuildDefinition) favorite.getBuildDefinition();
                        return;
                    }
                }

                if (o instanceof BuildDefinition) {
                    selectedDefinition = (BuildDefinition) o;
                    return;
                }
            }

            selectedDefinition = null;
            action.setEnabled(false);
            return;
        }

    }
}
