// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.compare.ServerItemByItemSpecGenerator;
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.TFSItemNode;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ChooseItemsToCompareDialog;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.client.common.ui.vc.ItemAndVersionResult;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class CompareAction extends TeamViewerAction {
    private TFSItem item;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final String modifiedItem = item.getPath();

        final String originalItem = item.getSourceServerPath();

        final boolean isDirectory = item instanceof TFSFolder;
        final ChooseItemsToCompareDialog dialog =
            new ChooseItemsToCompareDialog(getShell(), modifiedItem, originalItem, isDirectory, getCurrentRepository());

        if (dialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        final Compare compare = new Compare();
        compare.setModified(createCompareElement(dialog.getModifiedResult(), getCurrentRepository()));
        compare.setOriginal(createCompareElement(dialog.getOriginalResult(), getCurrentRepository()));
        compare.addComparator(TFSItemContentComparator.INSTANCE);
        compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(getShell()));
        compare.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);

        if (item == null || item.getDeletionID() != 0) {
            action.setEnabled(false);
            return;
        }

        final String path = item.getLocalPath();
        if (symbolicLinkSelected(path) || pendingAddSelected(path)) {
            action.setEnabled(false);
            return;
        }
    }

    private Object createCompareElement(final ItemAndVersionResult result, final TFSRepository repository) {
        if (result.isLocalItem()) {
            /*
             * Pass null for charset: if this is a resource in the workspace,
             * that charset will be used. Otherwise, it will attempt to
             * autodetect.
             */
            return CompareUtils.createCompareElementForLocalPath(
                result.getFile().getAbsolutePath(),
                null,
                ResourceType.fromFile(result.getFile()),
                null);
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
