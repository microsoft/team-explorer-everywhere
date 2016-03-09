// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheAdapter;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheEvent;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class RepositoryChangeItemProvider extends ChangeItemProvider {
    private final PendingChangeCacheListener pendingChangeCacheListener;

    public RepositoryChangeItemProvider(final TFSRepository repository) {
        super(repository);

        pendingChangeCacheListener = new PendingChangeCacheAdapter() {
            private boolean ignore = false;
            private boolean fullRefresh = false;

            @Override
            public void onAfterUpdatePendingChanges(
                final PendingChangeCacheEvent event,
                final boolean modifiedDuringOperation) {
                ignore = false;

                /*
                 * Only notify items have changed if the changes were modified
                 * in the operation.
                 */
                if (modifiedDuringOperation || fullRefresh) {
                    notifyOfUpdatedChangeItems();
                    fullRefresh = false;
                }
            }

            @Override
            public void onBeforeUpdatePendingChanges(final PendingChangeCacheEvent event) {
                ignore = true;
            }

            @Override
            public void onPendingChangeAdded(final PendingChangeCacheEvent event) {
                if (!ignore) {
                    notifyOfUpdatedChangeItems();
                }
            }

            @Override
            public void onPendingChangeModified(final PendingChangeCacheEvent event) {
                if (!ignore) {
                    notifyOfUpdatedChangeItems();
                }
            }

            @Override
            public void onPendingChangeRemoved(final PendingChangeCacheEvent event) {
                if (!ignore) {
                    notifyOfUpdatedChangeItems();
                }
            }

            @Override
            public void onPendingChangesCleared(final PendingChangeCacheEvent event) {
                if (!ignore) {
                    notifyOfUpdatedChangeItems();

                    /*
                     * Pending changes only get cleared when we do a full
                     * refresh - we should respect that and trigger an event to
                     * update the view.
                     */
                    fullRefresh = true;
                }
            }
        };

        repository.getPendingChangeCache().addListener(pendingChangeCacheListener);
    }

    @Override
    public void dispose() {
        getRepository().getPendingChangeCache().removeListener(pendingChangeCacheListener);
    }

    @Override
    public ChangeItem[] getChangeItems() {
        final TFSRepository repository = getRepository();
        final PendingChange[] pendingChanges = repository.getPendingChangeCache().getPendingChanges();

        return getChangeItemsFromPendingChanges(repository, pendingChanges);
    }

    @Override
    public ChangeItemType getType() {
        return ChangeItemType.PENDING;
    }

    public static ChangeItem[] getChangeItemsFromPendingChanges(
        final TFSRepository repository,
        final PendingChange[] pendingChanges) {
        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

        final ChangeItem[] changeItems = new ChangeItem[pendingChanges.length];
        for (int i = 0; i < pendingChanges.length; i++) {
            changeItems[i] = new ChangeItem(pendingChanges[i], ChangeItemType.PENDING, repository);
        }
        return changeItems;
    }
}
