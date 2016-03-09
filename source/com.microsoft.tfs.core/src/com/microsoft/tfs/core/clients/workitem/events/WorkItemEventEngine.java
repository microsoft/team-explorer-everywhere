// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.events;

import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.util.listeners.ListenerCategory;
import com.microsoft.tfs.util.listeners.ListenerRunnable;
import com.microsoft.tfs.util.listeners.MultiListenerList;

/**
 * Coordinates listeners and fires events for {@link WorkItemClient}.
 * <p>
 * Usage details for
 * {@link com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine}
 * also apply to this class.
 *
 * @threadsafety thread-safe
 */
public class WorkItemEventEngine {
    private final MultiListenerList listeners = new MultiListenerList();

    // Categories
    private static final ListenerCategory WORK_ITEM_SAVE = new ListenerCategory(WorkItemSaveListener.class);

    public WorkItemEventEngine() {
    }

    /*
     * Work Item Save
     */

    public void addWorkItemSaveListener(final WorkItemSaveListener listener) {
        listeners.addListener(listener, WORK_ITEM_SAVE);
    }

    public void removeWorkItemSaveListener(final WorkItemSaveListener listener) {
        listeners.removeListener(listener, WORK_ITEM_SAVE);
    }

    public void fireWorkItemSaveEvent(final WorkItemSaveEvent e) {
        listeners.getListenerList(WORK_ITEM_SAVE, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((WorkItemSaveListener) listener).onWorkItemSave(e);
                return true;
            }
        });
    }
}
