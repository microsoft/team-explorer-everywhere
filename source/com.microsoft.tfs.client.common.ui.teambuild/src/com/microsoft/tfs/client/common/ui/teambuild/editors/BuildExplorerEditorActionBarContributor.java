// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.editors;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;

import com.microsoft.tfs.client.common.ui.teambuild.actions.RefreshBuildExplorerAction;

public class BuildExplorerEditorActionBarContributor extends MultiPageEditorActionBarContributor {
    private final RefreshBuildExplorerAction refreshAction = new RefreshBuildExplorerAction();

    // SetPriorityAction setPriorityAction;
    // PostponeBuildAction postponeBuildAction;
    // StopBuildAction stopBuildAction;
    // EditBuildQualityAction editBuildQualityAction;
    // ToggleProtectionAction keepBuildAction;
    // DeleteBuildAction deleteBuildAction;
    // QueueBuildAction queueBuildAction;
    // ManageBuildAgentsAction manageBuildAgentsAction;
    // ManageBuildQualitiesAction manageBuildQualitiesAction;

    public BuildExplorerEditorActionBarContributor() {
        super();
    }

    /**
     * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToToolBar(org.eclipse.jface.action.IToolBarManager)
     */
    @Override
    public void contributeToToolBar(final IToolBarManager manager) {
        manager.add(new GroupMarker("com.microsoft.tfs.build")); //$NON-NLS-1$
        manager.add(refreshAction);
        manager.add(new Separator("com.microsoft.tfs.build.queue")); //$NON-NLS-1$
        // manager.add(new
        // ControlContribution("com.microsoft.tfs.build.priority"){
        manager.add(new Separator("com.microsoft.tfs.build.build")); //$NON-NLS-1$
        manager.add(new Separator("com.microsoft.tfs.build.global")); //$NON-NLS-1$

    }

    /**
     * @see org.eclipse.ui.part.MultiPageEditorActionBarContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
     */
    @Override
    public void setActiveEditor(final IEditorPart activeEditor) {
        super.setActiveEditor(activeEditor);
        if (activeEditor instanceof BuildExplorer) {
            final BuildExplorer buildExplorer = ((BuildExplorer) activeEditor);
            refreshAction.setActiveEditor(buildExplorer);

            final IActionBars actionBars = getActionBars();
            actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);
            actionBars.updateActionBars();
        }
    }

    /**
     * @see org.eclipse.ui.part.MultiPageEditorActionBarContributor#setActivePage(org.eclipse.ui.IEditorPart)
     */
    @Override
    public void setActivePage(final IEditorPart activeEditor) {
        // TODO Auto-generated method stub
    }

}
