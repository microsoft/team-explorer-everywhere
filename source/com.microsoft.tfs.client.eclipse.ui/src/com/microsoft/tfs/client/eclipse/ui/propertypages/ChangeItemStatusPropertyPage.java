// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemType;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;

public class ChangeItemStatusPropertyPage extends PropertyPage {
    public ChangeItemStatusPropertyPage() {
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(final Composite parent) {
        final StatusItem[] statusItems = getStatusItems(getChangeItem());

        final Composite composite = new Composite(parent, SWT.NONE);
        SWTUtil.gridLayout(composite);

        if (statusItems.length == 0) {
            SWTUtil.createLabel(
                composite,
                Messages.getString("ChangeItemStatusPropertyPage.NoPendingChangesLabelText")); //$NON-NLS-1$
        } else {
            SWTUtil.createLabel(composite, Messages.getString("ChangeItemStatusPropertyPage.StatusLabelText")); //$NON-NLS-1$

            final StatusItemTable table = new StatusItemTable(composite, SWT.NONE);
            GridDataBuilder.newInstance().grab().fill().applyTo(table);

            table.setStatusItems(statusItems);
        }

        return composite;
    }

    private StatusItem[] getStatusItems(final ChangeItem changeItem) {
        final List statusItems = new ArrayList();

        final String authenticatedUserName =
            changeItem.getRepository().getWorkspace().getClient().getConnection().getAuthorizedIdentity().getDisplayName();

        final PendingChange localPendingChange = getLocalPendingChange(changeItem);

        if (localPendingChange != null) {
            final StatusItem statusItem = new StatusItem(
                authenticatedUserName,
                localPendingChange.getChangeType(),
                changeItem.getRepository().getWorkspace().getName(),
                changeItem.getPropertyValues());
            statusItems.add(statusItem);
        }

        final String originalServerItem =
            changeItem.getSourceServerItem() != null ? changeItem.getSourceServerItem() : changeItem.getServerItem();
        final String parentServerItem = ServerPath.getParent(originalServerItem);

        final PendingSet[] pendingSets =
            changeItem.getRepository().getVersionControlClient().queryPendingSets(new String[] {
                parentServerItem
        }, RecursionType.ONE_LEVEL, false, null, null);

        if (pendingSets != null) {
            for (int i = 0; i < pendingSets.length; i++) {
                final PendingSet pendingSet = pendingSets[i];
                final PendingChange[] pendingChanges = pendingSet.getPendingChanges();
                for (int j = 0; j < pendingChanges.length; j++) {
                    if (changeItem.getItemID() == pendingChanges[j].getItemID()
                        && (localPendingChange == null
                            || localPendingChange.getPendingChangeID() != pendingChanges[j].getPendingChangeID())) {
                        final StatusItem statusItem = new StatusItem(
                            pendingSet.getOwnerDisplayName(),
                            pendingChanges[j].getChangeType(),
                            pendingSet.getName(),
                            pendingChanges[j].getPropertyValues());
                        statusItems.add(statusItem);
                    }
                }
            }
        }

        return (StatusItem[]) statusItems.toArray(new StatusItem[statusItems.size()]);
    }

    private PendingChange getLocalPendingChange(final ChangeItem changeItem) {
        if (ChangeItemType.PENDING == changeItem.getType()) {
            return changeItem.getPendingChange();
        }

        final String serverPath = changeItem.getServerItem();

        if (serverPath == null) {
            return null;
        }

        return changeItem.getRepository().getPendingChangeCache().getPendingChangeByServerPath(serverPath);
    }

    private ChangeItem getChangeItem() {
        return (ChangeItem) getElement().getAdapter(ChangeItem.class);
    }
}
