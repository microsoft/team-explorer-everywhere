// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.views.PendingChangesView;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

/**
 * This class maps pending changes created by a shelveset into change items.
 * This is for unshelving a shelveset and then checking the affected change
 * items in the pending changes view. This means that we query the pending
 * changes view for the actual underlying pending changes objects that were
 * affected, we DO NOT create new change items from the resultant change items.
 *
 * This is NOT for shelveset details, for shelveset details see
 * {@link ShelvesetChangeItemProvider}.
 */
public class UnshelvedPendingChangesChangeItemProvider extends ChangeItemProvider {
    private final PendingChangesView pendingChangesView;
    private final PendingChange[] unshelvedPendingChanges;

    public UnshelvedPendingChangesChangeItemProvider(
        final TFSRepository repository,
        final PendingChangesView pendingChangesView,
        final PendingChange[] unshelvedPendingChanges) {
        super(repository);

        Check.notNull(pendingChangesView, "pendingChangesView"); //$NON-NLS-1$
        Check.notNull(unshelvedPendingChanges, "unshelvedPendingChanges"); //$NON-NLS-1$

        this.pendingChangesView = pendingChangesView;
        this.unshelvedPendingChanges = unshelvedPendingChanges;
    }

    @Override
    public ChangeItem[] getChangeItems() {
        final ChangeItem[] allChangeItems =
            pendingChangesView.getCheckinControl().getSourceFilesSubControl().getChangesTable().getChangeItems();
        final List unshelvedItems = new ArrayList();

        for (int i = 0; i < unshelvedPendingChanges.length; i++) {
            for (int j = 0; j < allChangeItems.length; j++) {
                if (ServerPath.equals(unshelvedPendingChanges[i].getServerItem(), allChangeItems[j].getServerItem())) {
                    unshelvedItems.add(allChangeItems[j]);
                }
            }
        }

        return (ChangeItem[]) unshelvedItems.toArray(new ChangeItem[unshelvedItems.size()]);
    }

    @Override
    public ChangeItemType getType() {
        return ChangeItemType.PENDING;
    }
}
