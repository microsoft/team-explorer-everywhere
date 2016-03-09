// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;

public class WorkItemStateListenerSupport {
    private final Set<WorkItemStateListener> listeners = new HashSet<WorkItemStateListener>();
    private final WorkItem workItem;

    public WorkItemStateListenerSupport(final WorkItem workItem) {
        this.workItem = workItem;
    }

    public synchronized void addListener(final WorkItemStateListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(final WorkItemStateListener listener) {
        listeners.remove(listener);
    }

    public synchronized void fireDirtyStateChanged(final boolean isDirty) {
        for (final Iterator<WorkItemStateListener> it = listeners.iterator(); it.hasNext();) {
            it.next().dirtyStateChanged(isDirty, workItem);
        }
    }

    public synchronized void fireValidStateChanged(final boolean isValid) {
        for (final Iterator<WorkItemStateListener> it = listeners.iterator(); it.hasNext();) {
            it.next().validStateChanged(isValid, workItem);
        }
    }

    public synchronized void fireSaved() {
        for (final Iterator<WorkItemStateListener> it = listeners.iterator(); it.hasNext();) {
            it.next().saved(workItem);
        }
    }

    public synchronized void fireSynchedToLatest() {
        for (final Iterator<WorkItemStateListener> it = listeners.iterator(); it.hasNext();) {
            it.next().synchedToLatest(workItem);
        }
    }
}
