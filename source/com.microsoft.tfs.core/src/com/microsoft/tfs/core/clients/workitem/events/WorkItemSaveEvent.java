// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.util.Check;

public class WorkItemSaveEvent extends CoreClientEvent {
    private final WorkItem workItem;

    public WorkItemSaveEvent(final EventSource source, final WorkItem workItem) {
        super(source);

        Check.notNull(workItem, "workItem"); //$NON-NLS-1$
        this.workItem = workItem;
    }

    public WorkItem getWorkItem() {
        return workItem;
    }
}
