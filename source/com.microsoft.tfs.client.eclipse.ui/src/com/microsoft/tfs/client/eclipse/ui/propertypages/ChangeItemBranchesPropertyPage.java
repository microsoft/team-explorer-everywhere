// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;

public class ChangeItemBranchesPropertyPage extends BaseBranchesPropertyPage {
    @Override
    protected String getLocation() {
        final ChangeItem changeItem = getChangeItem();

        if (changeItem == null) {
            return null;
        }

        return changeItem.getServerItem();
    }

    @Override
    protected TFSRepository getRepository() {
        final ChangeItem changeItem = getChangeItem();

        if (changeItem == null) {
            return null;
        }

        return changeItem.getRepository();
    }

    private ChangeItem getChangeItem() {
        return (ChangeItem) getElement().getAdapter(ChangeItem.class);
    }
}
