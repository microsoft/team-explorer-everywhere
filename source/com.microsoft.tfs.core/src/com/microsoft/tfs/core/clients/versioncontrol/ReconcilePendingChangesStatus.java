// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Contains the results of a
 * {@link Workspace#findReconcilablePendingChangesForChangeset(Changeset, PendingChange[])}
 * call.
 */
public final class ReconcilePendingChangesStatus {
    private final boolean matchedAtLeastOnePendingChange;
    private final PendingChange[] reconcilablePendingChanges;

    public ReconcilePendingChangesStatus(final boolean matchedAtLeastOnePendingChange) {
        this(false, new PendingChange[0]);
    }

    public ReconcilePendingChangesStatus(
        final boolean matchedAtLeastOnePendingChange,
        final PendingChange[] reconcilablePendingChanges) {
        super();

        Check.notNull(reconcilablePendingChanges, "reconcilablePendingChanges"); //$NON-NLS-1$

        this.matchedAtLeastOnePendingChange = matchedAtLeastOnePendingChange;
        this.reconcilablePendingChanges = reconcilablePendingChanges;
    }

    public boolean matchedAtLeastOnePendingChange() {
        return matchedAtLeastOnePendingChange;
    }

    public PendingChange[] getReconcilablePendingChanges() {
        return reconcilablePendingChanges;
    }

}
