// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

/**
 * @threadsafety unknown
 */
public class ShelvesetChangeItemProvider extends AbstractShelvesetChangeItemProvider {
    private final PendingChange[] pendingChanges;

    public ShelvesetChangeItemProvider(final TFSRepository repository, final PendingChange[] pendingChanges) {
        super(repository);

        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

        this.pendingChanges = pendingChanges;
    }

    @Override
    protected PendingChange[] getPendingChanges() {
        return pendingChanges;
    }
}
