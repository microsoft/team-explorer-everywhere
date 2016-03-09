// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.cache.conflict;

import java.util.EventListener;

/**
 * ConflictManagerListener implementation for event listeners in the
 * ConflictManager.
 */
public interface ConflictCacheListener extends EventListener {
    /**
     * This event will be called for all additions and removals of conflicts in
     * the ConflictManager.
     *
     * @param event
     *        details of the added/removed conflict
     */
    public void onConflictEvent(ConflictCacheEvent event);
}
