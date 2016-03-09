// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.filters;

import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinNotes;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinPendingChanges;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinPolicies;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinWorkItems;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A {@link PendingCheckin} that wraps another {@link PendingCheckin} and
 * filters its data (pending changes, notes, etc.) before they are returned by
 * its get methods.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class FilterPendingCheckin implements PendingCheckin {
    private final PendingCheckin realPendingCheckin;
    private final FilterPendingCheckinPendingChanges filteredPendingChanges;

    /**
     * Creates a pending checkin using the user-supplied evaluator and the
     * loader it was already configured with.
     */
    public FilterPendingCheckin(final PendingCheckin realPendingCheckin, final PendingChangeFilter filter) {
        Check.notNull(realPendingCheckin, "realPendingCheckin"); //$NON-NLS-1$
        Check.notNull(filter, "filter"); //$NON-NLS-1$

        this.realPendingCheckin = realPendingCheckin;
        filteredPendingChanges = new FilterPendingCheckinPendingChanges(realPendingCheckin.getPendingChanges(), filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingCheckinNotes getCheckinNotes() {
        return realPendingCheckin.getCheckinNotes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingCheckinPolicies getCheckinPolicies() {
        return realPendingCheckin.getCheckinPolicies();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingCheckinPendingChanges getPendingChanges() {
        return filteredPendingChanges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingCheckinWorkItems getWorkItems() {
        return realPendingCheckin.getWorkItems();
    }
}
