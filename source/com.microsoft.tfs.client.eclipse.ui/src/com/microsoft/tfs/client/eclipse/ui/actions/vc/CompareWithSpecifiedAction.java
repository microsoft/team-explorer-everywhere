// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.compare.ServerItemByItemSpecGenerator;
import com.microsoft.tfs.client.common.ui.compare.TFSItemNode;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ChooseItemsToCompareDialog;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.client.common.ui.vc.ItemAndVersionResult;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class CompareWithSpecifiedAction extends CompareAction {
    private static final Log log = LogFactory.getLog(CompareWithSpecifiedAction.class);

    private ChooseItemsToCompareDialog dialog;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        if (ActionHelpers.linkSelected(selection) || ActionHelpers.pendingAddSelected(selection)) {
            action.setEnabled(false);
            return;
        }

        /*
         * Use the standard filter to allow comparing non-repository items.
         */
        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.STANDARD_FILTER)
                && ActionHelpers.getRepositoriesFromSelection(getSelection()).length == 1);
    }

    @Override
    protected boolean prepare(final IResource resource, final TFSRepository repository) {
        final String modifiedItem = Resources.getLocation(resource, LocationUnavailablePolicy.THROW);

        /*
         * If the selected item is in the repository, use its server path as the
         * right item, otherwise use the local path and let the user browse
         * around.
         */
        String originalItem = modifiedItem;

        if (PluginResourceFilters.IN_REPOSITORY_FILTER.filter(resource).isAccept()) {
            originalItem = repository.getWorkspace().getMappedServerPath(modifiedItem);
        }

        final boolean isDirectory = LocalPath.getDirectory(modifiedItem).equals(modifiedItem);

        dialog = new ChooseItemsToCompareDialog(getShell(), modifiedItem, originalItem, isDirectory, repository);

        return IDialogConstants.OK_ID == dialog.open();
    }

    @Override
    protected Object getAncestorCompareElement(final IResource resource, final TFSRepository repository) {
        return null;
    }

    @Override
    protected Object getModifiedCompareElement(final IResource resource, final TFSRepository repository) {
        return createCompareElement(dialog.getModifiedResult(), repository);
    }

    @Override
    protected Object getOriginalCompareElement(final IResource resource, final TFSRepository repository) {
        return createCompareElement(dialog.getOriginalResult(), repository);
    }

    private Object createCompareElement(final ItemAndVersionResult result, final TFSRepository repository) {
        if (result.isLocalItem()) {
            /*
             * Pass null for charset: if this is a resource in the workspace,
             * that charset will be used. Otherwise, we have no means of
             * determining.
             */
            return CompareUtils.createCompareElementForLocalPath(
                result.getFile().getAbsolutePath(),
                null,
                ResourceType.fromFile(result.getFile()),
                PluginResourceFilters.STANDARD_FILTER);
        } else {
            if (ItemType.FILE == result.getItem().getItemType()) {
                return new TFSItemNode(result.getItem(), repository.getVersionControlClient());
            }

            final ItemSpec itemSpec =
                new ItemSpec(result.getItem().getServerItem(), RecursionType.FULL, result.getItem().getDeletionID());

            return new ServerItemByItemSpecGenerator(repository, itemSpec, result.getTargetVersion());
        }
    }
}
