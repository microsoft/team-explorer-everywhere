// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;

public class ChangesetChangeItemProvider extends ChangeItemProvider {
    private final Changeset changeset;

    public ChangesetChangeItemProvider(final TFSRepository repository, final Changeset changeset) {
        super(repository);
        this.changeset = changeset;
    }

    @Override
    public ChangeItem[] getChangeItems() {
        final TFSRepository repository = getRepository();

        final Change[] changes = changeset.getChanges();
        final ChangeItem[] changeItems = new ChangeItem[changes.length];
        for (int i = 0; i < changes.length; i++) {
            changeItems[i] = new ChangeItem(changes[i], repository);
        }

        return changeItems;
    }

    @Override
    public ChangeItemType getType() {
        return ChangeItemType.CHANGESET;
    }
}
