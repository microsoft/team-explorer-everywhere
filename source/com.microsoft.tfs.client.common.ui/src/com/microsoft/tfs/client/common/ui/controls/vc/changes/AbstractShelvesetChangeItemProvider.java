// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public abstract class AbstractShelvesetChangeItemProvider extends ChangeItemProvider {
    private ChangeItem[] changeItems = null;

    protected AbstractShelvesetChangeItemProvider(final TFSRepository repository) {
        super(repository);
    }

    protected abstract PendingChange[] getPendingChanges();

    @Override
    public final ChangeItem[] getChangeItems() {
        if (changeItems == null) {
            PendingChange[] pendingChanges = getPendingChanges();

            if (pendingChanges == null) {
                pendingChanges = new PendingChange[0];
            }

            changeItems = new ChangeItem[pendingChanges.length];
            for (int i = 0; i < pendingChanges.length; i++) {
                changeItems[i] = new ChangeItem(pendingChanges[i], ChangeItemType.SHELVESET, getRepository());
            }
        }

        return changeItems;
    }

    @Override
    public final ChangeItemType getType() {
        return ChangeItemType.SHELVESET;
    }
}
