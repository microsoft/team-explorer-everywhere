// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.cache.pendingchange;

import java.util.EventListener;

/**
 * <p>
 * A {@link PendingChangeCacheListener} can be attached to a
 * {@link PendingChangeCache} to receive notifications when the cached pending
 * change data has changed.
 * </p>
 *
 * <p>
 * The {@link PendingChangeCacheAdapter} class provides null implementations of
 * all of the listener methods defined in this interface. It can be subclassed
 * when only a subset of the methods are needed.
 * </p>
 *
 * @see PendingChangeCache
 * @see PendingChangeCacheAdapter
 */
public interface PendingChangeCacheListener extends EventListener {
    /**
     * <p>
     * An event that is sent whenever a significant update to the pending change
     * cache is started. The intent is for listeners who are not interested in
     * receiving every fine-grained update to starting ignoring notifications
     * after receiving this event. After the update process has finished, a
     * matching event will be sent through
     * {@link #onAfterUpdatePendingChanges(PendingChangeCacheEvent)}. At that
     * point, listeners can get the latest data from the cache and begin
     * processing notifications again.
     * </p>
     *
     * <p>
     * Every {@link #onBeforeUpdatePendingChanges(PendingChangeCacheEvent)} is
     * guaranteed to subsequently be matched by a call to
     * {@link #onAfterUpdatePendingChanges(PendingChangeCacheEvent)}.
     * </p>
     *
     * <p>
     * The event object does not contain any pending change data for this event.
     * </p>
     *
     * @param event
     *        the {@link PendingChangeCacheEvent} (never <code>null</code>)
     */
    public void onBeforeUpdatePendingChanges(PendingChangeCacheEvent event);

    /**
     * <p>
     * A matching event that is sent sometime after a call to
     * {@link #onBeforeUpdatePendingChanges(PendingChangeCacheEvent)}. Listeners
     * who started ignoring notifications in that method should use this event
     * to get the latest data from the cache and begin processing notifications
     * again.
     * </p>
     *
     * <p>
     * The event object does not contain any pending change data for this event.
     * </p>
     *
     * @param event
     *        the {@link PendingChangeCacheEvent} (never <code>null</code>)
     * @param modifiedDuringOperation
     *        <code>true</code> if the {@link PendingChangeCache}'s contents
     *        were actually modified (a pending change was added to the
     *        collection, removed from it, or one that was in the collection was
     *        changed, or the collection was cleaered) since the matching call
     *        to {@link #onBeforeUpdatePendingChanges(PendingChangeCacheEvent)},
     *        <code>false</code> if the cache's contents were not modified
     */
    public void onAfterUpdatePendingChanges(PendingChangeCacheEvent event, boolean modifiedDuringOperation);

    /**
     * <p>
     * An event that is sent when a new pending change is added to the cache.
     * </p>
     *
     * <p>
     * The new pending change can be obtained by calling
     * {@link PendingChangeCacheEvent#getPendingChange()} on the event object.
     * </p>
     *
     * @param event
     *        the {@link PendingChangeCacheEvent} (never <code>null</code>)
     */
    public void onPendingChangeAdded(PendingChangeCacheEvent event);

    /**
     * <p>
     * An event that is sent when a pending change is removed from the cache.
     * </p>
     *
     * <p>
     * The removed pending change can be obtained by calling
     * {@link PendingChangeCacheEvent#getPendingChange()} on the event object.
     * </p>
     *
     * @param event
     *        the {@link PendingChangeCacheEvent} (never <code>null</code>)
     */
    public void onPendingChangeRemoved(PendingChangeCacheEvent event);

    /**
     * <p>
     * An event that is sent when a pending change already in the cache is
     * updated.
     * </p>
     *
     * <p>
     * The old pending change (
     * {@link PendingChangeCacheEvent#getOldPendingChange()}) and the new
     * pending change ({@link PendingChangeCacheEvent#getPendingChange()}) are
     * available from the event object.
     * </p>
     *
     * @param event
     *        the {@link PendingChangeCacheEvent} (never <code>null</code>)
     */
    public void onPendingChangeModified(PendingChangeCacheEvent event);

    /**
     * <p>
     * An event that is sent when all pending changes that were in the cache
     * were removed.
     * </p>
     *
     * <p>
     * The event object does not contain any pending change data for this event.
     * </p>
     *
     * @param event
     *        the {@link PendingChangeCacheEvent} (never <code>null</code>)
     */
    public void onPendingChangesCleared(PendingChangeCacheEvent event);
}
