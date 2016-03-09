// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.sync;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public final class LocalResourceData {
    private final PendingChange pendingChange;
    private Item item;

    public LocalResourceData(final PendingChange pendingChange) {
        Check.notNull(pendingChange, "pendingChange"); //$NON-NLS-1$

        this.pendingChange = pendingChange;
    }

    public LocalResourceData(final PendingChange pendingChange, final Item item) {
        this(pendingChange);

        this.item = item;
    }

    public PendingChange getPendingChange() {
        return pendingChange;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(final Item item) {
        this.item = item;
    }
}
