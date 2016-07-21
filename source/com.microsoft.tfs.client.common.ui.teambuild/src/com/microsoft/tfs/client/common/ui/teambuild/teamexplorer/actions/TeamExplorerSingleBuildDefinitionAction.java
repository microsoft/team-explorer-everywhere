// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import com.microsoft.alm.teamfoundation.build.webapi.DefinitionType;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.BuildFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDetail;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.QueuedBuild;

@SuppressWarnings("restriction")
public abstract class TeamExplorerSingleBuildDefinitionAction extends TeamExplorerBaseAction
    implements IEditorActionDelegate {
    protected IBuildDefinition selectedDefinition;

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            if (action.isEnabled() && selection instanceof IStructuredSelection) {
                final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                if (structuredSelection.size() != 1) {
                    action.setEnabled(false);
                    return;
                }

                final Object obj = structuredSelection.getFirstElement();
                if (obj instanceof IBuildDefinition) {
                    selectedDefinition = (IBuildDefinition) obj;
                    return;
                } else if (obj instanceof BuildDetail) {
                    final BuildDetail buildDetail = (BuildDetail) obj;
                    if (buildDetail.getBuildDefinition() != null) {
                        selectedDefinition = buildDetail.getBuildDefinition();
                        return;
                    }
                } else if (obj instanceof QueuedBuild) {
                    final QueuedBuild queuedBuild = (QueuedBuild) obj;
                    if (queuedBuild.getBuildDefinition() != null) {
                        selectedDefinition = queuedBuild.getBuildDefinition();
                        return;
                    }
                } else if (obj instanceof BuildFavoriteItem) {
                    final BuildFavoriteItem favorite = (BuildFavoriteItem) obj;
                    if (favorite.getBuildDefinitionType() == DefinitionType.XAML) {
                        selectedDefinition = (IBuildDefinition) favorite.getBuildDefinition();
                        return;
                    }
                }

                selectedDefinition = null;
                action.setEnabled(false);
                return;
            }
        }
    }

    @Override
    public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
        this.setActivePart(action, targetEditor);
    }
}
