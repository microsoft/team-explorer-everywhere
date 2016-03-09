// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.sync;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshResourceSelectionPage;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.sync.SynchronizeParticipant;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;

public class SynchronizeWizard extends Wizard {
    private GlobalRefreshResourceSelectionPage resourceSelectionPage;

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public SynchronizeWizard() {
        super();

        setDefaultPageImageDescriptor(imageHelper.getImageDescriptor("images/wizard/pageheader.png")); //$NON-NLS-1$
    }

    @Override
    public void addPages() {
        super.addPages();

        if (TFSEclipseClientPlugin.getDefault().getProjectManager().isConnecting()) {
            addPage(new SynchronizeWizardErrorPage(SynchronizeWizardErrorPage.CONNECTING));
            return;
        }

        final IProject[] allProjects = TeamUtils.getProjectsConfiguredWith(TFSRepositoryProvider.PROVIDER_ID);
        final List onlineProjects = new ArrayList();

        for (int i = 0; i < allProjects.length; i++) {
            final ProjectRepositoryStatus status =
                TFSEclipseClientPlugin.getDefault().getProjectManager().getProjectStatus(allProjects[i]);

            if (status == ProjectRepositoryStatus.ONLINE) {
                onlineProjects.add(allProjects[i]);
            }
        }

        if (onlineProjects.size() == 0) {
            addPage(new SynchronizeWizardErrorPage(SynchronizeWizardErrorPage.OFFLINE));
            return;
        }

        resourceSelectionPage = new GlobalRefreshResourceSelectionPage(
            (IProject[]) onlineProjects.toArray(new IProject[onlineProjects.size()]));
        resourceSelectionPage.setTitle(Messages.getString("SynchronizeWizard.ResourceSelectionPageTitle")); //$NON-NLS-1$
        resourceSelectionPage.setMessage(Messages.getString("SynchronizeWizard.ResourceSelectionPageDescription")); //$NON-NLS-1$

        addPage(resourceSelectionPage);
    }

    @Override
    public boolean performFinish() {
        final SynchronizeParticipant syncParticipant = new SynchronizeParticipant();

        final ISynchronizeManager syncManager = TeamUI.getSynchronizeManager();
        syncManager.addSynchronizeParticipants(new ISynchronizeParticipant[] {
            syncParticipant
        });

        final ISynchronizeView syncView = syncManager.showSynchronizeViewInActivePage();
        syncView.display(syncParticipant);

        final IResource[] roots = resourceSelectionPage.getRootResources();
        syncParticipant.getSubscriberSyncInfoCollector().setRoots(roots);

        /* Reflection for eclipse 3.0 compat */
        final IViewSite viewSite = syncView.getViewSite();

        try {
            final Method getPartMethod = viewSite.getClass().getMethod("getPart", new Class[0]); //$NON-NLS-1$
            final Object viewSitePart = getPartMethod.invoke(viewSite, new Object[0]);

            if (viewSitePart != null && viewSitePart instanceof IWorkbenchPart) {
                syncParticipant.run((IWorkbenchPart) viewSitePart);
            }
        } catch (final Exception e) {
            /* Suppress */
        }

        return true;
    }
}
