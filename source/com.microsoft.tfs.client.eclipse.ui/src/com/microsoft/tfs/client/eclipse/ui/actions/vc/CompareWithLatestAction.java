// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.compare.ServerItemByItemVersionGenerator;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;

public class CompareWithLatestAction extends CompareAction {
    private ItemSpec itemSpec;

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

        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.IN_REPOSITORY_FILTER));
    }

    @Override
    protected boolean prepare(final IResource resource, final TFSRepository repository) {
        itemSpec = PluginResourceHelpers.typedItemSpecForResource(resource, true, LocationUnavailablePolicy.THROW);
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
        return new ServerItemByItemVersionGenerator(
            repository,
            itemSpec,
            new WorkspaceVersionSpec(repository.getWorkspace()),
            LatestVersionSpec.INSTANCE);
    }
}
