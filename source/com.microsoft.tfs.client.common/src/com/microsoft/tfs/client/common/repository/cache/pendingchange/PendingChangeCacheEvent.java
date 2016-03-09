// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.cache.pendingchange;

import java.util.EventObject;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

/**
 * <p>
 * A {@link PendingChangeCacheEvent} is sent to
 * {@link PendingChangeCacheListener} methods when {@link PendingChangeCache}
 * events are fired.
 * </p>
 *
 * <p>
 * Every {@link PendingChangeCacheEvent} contains at least a reference to the
 * {@link PendingChangeCache} that fired it ({@link #getCache()}). In addition,
 * an event may optionally contain a pending change ({@link #getPendingChange()}
 * ) and an old pending change ({@link #getOldPendingChange()}). See the
 * individual method documentation on {@link PendingChangeCacheListener} for an
 * explanation of what each field means, if anything, for each event.
 * </p>
 *
 * @see PendingChangeCacheListener
 * @see PendingChangeCache
 */
public class PendingChangeCacheEvent extends EventObject {
    private final PendingChange oldPendingChange;
    private final PendingChange pendingChange;

    /**
     * Creates a new event with no pending change data.
     *
     * @param cache
     *        the {@link PendingChangeCache} that will fire this event (must not
     *        be <code>null</code>)
     */
    public PendingChangeCacheEvent(final PendingChangeCache cache) {
        this(cache, null, null);
    }

    /**
     * Creates a new event with the pending change ({@link #getPendingChange()})
     * field set, and the old pending change ({@link #getOldPendingChange()})
     * field empty.
     *
     * @param cache
     *        the {@link PendingChangeCache} that will fire this event (must not
     *        be <code>null</code>)
     * @param pendingChange
     *        the value to return from {@link #getPendingChange()}
     */
    public PendingChangeCacheEvent(final PendingChangeCache cache, final PendingChange pendingChange) {
        this(cache, null, pendingChange);
    }

    /**
     * Creates a new event that contains pending change data.
     *
     * @param cache
     *        the {@link PendingChangeCache} that will fire this event (must not
     *        be <code>null</code>)
     * @param oldPendingChange
     *        the value to return from {@link #getOldPendingChange()}
     * @param pendingChange
     *        the value to return from {@link #getPendingChange()}
     */
    public PendingChangeCacheEvent(
        final PendingChangeCache cache,
        final PendingChange oldPendingChange,
        final PendingChange pendingChange) {
        super(cache);
        this.oldPendingChange = oldPendingChange;
        this.pendingChange = pendingChange;
    }

    /**
     * @return the {@link PendingChangeCache} that fired this event (never
     *         <code>null</code>)
     */
    public PendingChangeCache getCache() {
        return (PendingChangeCache) getSource();
    }

    /**
     * @return the old pending change associated with this event, if any (may be
     *         <code>null</code>)
     */
    public PendingChange getOldPendingChange() {
        return oldPendingChange;
    }

    /**
     * @return the pending change associated with this event, if any (may be
     *         <code>null</code>)
     */
    public PendingChange getPendingChange() {
        return pendingChange;
    }
}
