// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.controls.vc.properties.AdvancedPropertiesTab;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.GeneralBranchPropertiesTab;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.GeneralPropertiesTab;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.RelationshipPropertiesTab;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.StatusPropertiesTab;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.branches.BranchesPropertiesTab;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PropertiesDialog;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;

public class PropertiesAction extends TeamViewerAction {
    private TFSItem item;

    // To support BranchHierarchyEditor which references items by ItemIdentifier
    private ItemIdentifier itemId;

    @Override
    public void doRun(final IAction action) {
        if (item != null || itemId != null) {
            final PropertiesDialog propertiesDialog =
                new PropertiesDialog(getShell(), getCurrentRepository(), item, itemId);
            if (item.getExtendedItem() != null && item.getExtendedItem().isBranch()) {
                propertiesDialog.addPropertiesTab(new GeneralBranchPropertiesTab());
                propertiesDialog.addPropertiesTab(new RelationshipPropertiesTab());
            } else {
                propertiesDialog.addPropertiesTab(new GeneralPropertiesTab());
                propertiesDialog.addPropertiesTab(new StatusPropertiesTab());
                propertiesDialog.addPropertiesTab(new BranchesPropertiesTab());
            }
            if (wasShiftKeyPressedWhenActionInvoked()) {
                propertiesDialog.addPropertiesTab(new AdvancedPropertiesTab());
            }

            propertiesDialog.open();
        }
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);
    }

    public void setItemIdentifier(final ItemIdentifier itemId) {
        this.itemId = itemId;
    }
}
