// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import java.lang.reflect.Method;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.team.ui.synchronize.WorkspaceScope;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;

import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.client.eclipse.ui.sync.SynchronizeParticipant;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.util.Check;

public class SynchronizeAction extends ExtendedAction {
    public SynchronizeAction() {
        super();
        setName(Messages.getString("SynchronizeAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.STANDARD_FILTER)
                && ActionHelpers.getRepositoriesFromSelection(getSelection()).length == 1);
    }

    @Override
    public void doRun(final IAction action) {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.STANDARD_FILTER,
            true);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final IResource[] resources = selectionInfo.getResources();

        // See if we have an existing participant.
        SynchronizeParticipant participant = (SynchronizeParticipant) SubscriberParticipant.getMatchingParticipant(
            SynchronizeParticipant.PARTICIPANT_ID,
            resources);

        // if not create it, using current selection
        if (participant == null) {
            ISynchronizeScope scope;

            /*
             * If all of the selected resources are projects configured to use
             * our team provider, then we can use a workspace scope.
             */
            if (resourcesRepresentAllTFSConfiguredProjects(resources)) {
                scope = new WorkspaceScope();
            } else {
                // Working sets would be nice, but that's an Eclipse 3.1 feature
                // and requires implementation via careful reflection.
                scope = new ResourceScope(resources);
            }

            participant = new SynchronizeParticipant(scope);

            TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {
                participant
            });
        }

        final ISynchronizeManager syncManager = TeamUI.getSynchronizeManager();
        final ISynchronizeView syncView = syncManager.showSynchronizeViewInActivePage();
        syncView.display(participant);

        participant.getSubscriberSyncInfoCollector().setRoots(resources);

        /* Reflection for eclipse 3.0 compat */
        final IViewSite viewSite = syncView.getViewSite();

        try {
            final Method getPartMethod = viewSite.getClass().getMethod("getPart", new Class[0]); //$NON-NLS-1$
            final Object viewSitePart = getPartMethod.invoke(viewSite, new Object[0]);

            if (viewSitePart != null && viewSitePart instanceof IWorkbenchPart) {
                participant.run((IWorkbenchPart) viewSitePart);
            }
        } catch (final Exception e) {
            /* Suppress */
        }
    }

    /**
     * Tests whether all of the given resources are (1) {@link IProject}s and
     * (2) are configured to use our team provider.
     *
     * @param resources
     *        the resources to check (must not be <code>null</code>)
     * @return true if the given resources represent all of the projects
     *         configured to use our team provider, false otherwise
     */
    private boolean resourcesRepresentAllTFSConfiguredProjects(final IResource[] resources) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$

        for (int i = 0; i < resources.length; i++) {
            if (resources[i].getType() != IResource.PROJECT) {
                return false;
            }
        }

        /*
         * If the array (which are all projects) equals the length of all the
         * projects configured with our team provider, return true.
         */
        return TeamUtils.getProjectsConfiguredWith(TFSRepositoryProvider.PROVIDER_ID).length == resources.length;
    }
}