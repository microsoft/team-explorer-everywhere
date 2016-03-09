// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.cache.pendingchange;

/**
 * {@link PendingChangeCacheAdapter} implements that
 * {@link PendingChangeCacheListener} interface and provides do-nothing
 * implementations of all the methods.
 */
public class PendingChangeCacheAdapter implements PendingChangeCacheListener {
    /**
     * {@inheritDoc}
     */
    @Override
    public void onAfterUpdatePendingChanges(
        final PendingChangeCacheEvent event,
        final boolean modifiedDuringOperation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBeforeUpdatePendingChanges(final PendingChangeCacheEvent event) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPendingChangeAdded(final PendingChangeCacheEvent event) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPendingChangeModified(final PendingChangeCacheEvent event) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPendingChangeRemoved(final PendingChangeCacheEvent event) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPendingChangesCleared(final PendingChangeCacheEvent event) {
    }
}
