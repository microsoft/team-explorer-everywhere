// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;

public class AddQueryToTeamFavoritesAction extends AddQueryToFavoritesAction {
    public AddQueryToTeamFavoritesAction() {
        super(false);
    }

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (selection.isEmpty() || getContext().getCurrentTeam() == null) {
            action.setEnabled(false);
        }
    }

    @Override
    protected void fireFavoritesChangedEvent(final TeamExplorerContext context) {
        context.getEvents().notifyListener(TeamExplorerEvents.TEAM_WORK_ITEM_FAVORITES_CHANGED);
    }
}
