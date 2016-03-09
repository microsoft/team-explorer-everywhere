// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.compare.BaselineItemByPendingChangeGenerator;
import com.microsoft.tfs.client.common.ui.compare.ServerItemByItemVersionGenerator;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;

public class CompareWithUnmodifiedAction extends CompareAction {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        if (ActionHelpers.linkSelected(selection)) {
            action.setEnabled(false);
            return;
        }

        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            selection,
            PluginResourceFilters.HAS_PENDING_CHANGES_FILTER,
            true);

        if (selectionInfo.getRepositories().length != 1 || selectionInfo.getResources().length != 1) {
            action.setEnabled(false);
            return;
        }

        if (selectionInfo.getResources()[0].getType() == IResource.FILE) {
            // Only enable the action for certain kinds of file pending changes
            if (selectionInfo.getPendingChanges().length != 1) {
                action.setEnabled(false);
                return;
            }

            action.setEnabled(
                PendingChangesHelpers.canCompareWithWorkspaceVersion(
                    selectionInfo.getRepositories()[0],
                    selectionInfo.getPendingChanges()[0]));
        } else {
            // Folders and other containers can always be compared
            action.setEnabled(true);
        }
    }

    @Override
    protected boolean prepare(final IResource resource, final TFSRepository repository) {
        return true;
    }

    @Override
    protected Object getAncestorCompareElement(final IResource resource, final TFSRepository repository) {
        return null;
    }

    @Override
    protected Object getModifiedCompareElement(final IResource resource, final TFSRepository repository) {
        /*
         * Note: we do not simply use IN_REPOSITORY_FILTER here (as we did for
         * action enablement) so that we can include children with pending adds
         * (that are not in the repository, but do have a pending change.)
         *
         * Action enablement should be a stricter set (in repository only) so
         * that we do not offer compare with latest against pending adds.
         */
        return CompareUtils.createCompareElementForResource(
            resource,
            PluginResourceFilters.HAS_PENDING_CHANGES_OR_IN_REPOSITORY_FILTER);
    }

    @Override
    protected Object getOriginalCompareElement(final IResource resource, final TFSRepository repository) {
        final ItemSpec itemSpec =
            PluginResourceHelpers.typedItemSpecForResource(resource, true, LocationUnavailablePolicy.THROW);

        /*
         * If if's a local workspace, and there's a pending change for the item,
         * and the item is a file, we can use the baseline file and avoid
         * contacting the server.
         */
        if (repository.getWorkspace().getLocation() == WorkspaceLocation.LOCAL) {
            final PendingChange[] pendingChanges =
                PluginResourceHelpers.pendingChangesForResource(resource, repository);

            if (pendingChanges != null
                && pendingChanges.length == 1
                && pendingChanges[0].getItemType() == ItemType.FILE) {
                return new BaselineItemByPendingChangeGenerator(repository.getWorkspace(), pendingChanges[0]);
            }
        }

        return new ServerItemByItemVersionGenerator(
            repository,
            itemSpec,
            new WorkspaceVersionSpec(repository.getWorkspace()),
            new WorkspaceVersionSpec(repository.getWorkspace()));
    }
}
