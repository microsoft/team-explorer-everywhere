// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.cache.conflict;

import java.util.EventObject;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.util.Check;

/**
 * This is for add/remove conflict events fired by the ConflictManager. It is
 * called ConflictManagerEvent to avoid ambiguity with import
 * {@link com.microsoft.tfs.core.clients.versioncontrol.events.ConflictEvent}.
 */
public class ConflictCacheEvent extends EventObject {
    public static final int ADDED = 1;
    public static final int REMOVED = 2;
    public static final int MODIFIED = 3;

    private final int eventType;
    private final Conflict conflict;

    public ConflictCacheEvent(final ConflictCache source, final int eventType, final Conflict conflict) {
        super(source);

        if (eventType != ADDED && eventType != REMOVED && eventType != MODIFIED) {
            throw new IllegalArgumentException("illegal event type: " + eventType); //$NON-NLS-1$
        }

        Check.notNull(conflict, "newChange"); //$NON-NLS-1$

        this.eventType = eventType;
        this.conflict = conflict;
    }

    public int getEventType() {
        return eventType;
    }

    public Conflict getConflict() {
        return conflict;
    }

    @Override
    public String toString() {
        String description = ""; //$NON-NLS-1$

        if (eventType == ADDED) {
            description += "added"; //$NON-NLS-1$
        } else if (eventType == REMOVED) {
            description += "removed"; //$NON-NLS-1$
        }

        description += " conflict=[" + conflict + "]"; //$NON-NLS-1$ //$NON-NLS-2$

        return description;
    }
}
