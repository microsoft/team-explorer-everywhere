// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wittype;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadata;

/**
 * Caches the "next state on action" computation result for a specific work item
 * type.
 *
 * Threadsafe.
 */
public class NextStateCache {
    private final Map<Key, String> cache = new HashMap<Key, String>();
    private final int workItemTypeId;
    private final IMetadata metadata;

    public NextStateCache(final int workItemTypeId, final IMetadata metadata) {
        this.workItemTypeId = workItemTypeId;
        this.metadata = metadata;
    }

    public synchronized String getNextState(final String currentState, final String action) {
        final Key key = new Key(currentState, action);

        if (!cache.containsKey(key)) {
            cache.put(key, computeNextState(currentState, action));
        }

        return cache.get(key);
    }

    public synchronized void clearCache() {
        cache.clear();
    }

    private String computeNextState(final String currentState, final String action) {
        return metadata.getActionsTable().getNextStateForAction(currentState, action, workItemTypeId);
    }

    private static class Key {
        private final String currentState;
        private final String action;

        public Key(final String currentState, final String action) {
            this.currentState = currentState;
            this.action = action;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Key) {
                final Key key = (Key) obj;
                return currentState.equals(key.currentState) && action.equals(key.action);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return currentState.hashCode() + action.hashCode();
        }
    }
}
