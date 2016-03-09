// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.events;

import java.util.EventListener;

public interface WorkItemSaveListener extends EventListener {
    void onWorkItemSave(WorkItemSaveEvent e);
}
