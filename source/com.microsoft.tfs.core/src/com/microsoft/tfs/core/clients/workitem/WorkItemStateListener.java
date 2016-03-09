// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

/**
 * Defines an interface for listeners of work item state change events.
 *
 * @since TEE-SDK-10.1
 */
public interface WorkItemStateListener {
    public void dirtyStateChanged(boolean isDirty, WorkItem workItem);

    public void validStateChanged(boolean isValid, WorkItem workItem);

    public void saved(WorkItem workItem);

    public void synchedToLatest(WorkItem workItem);
}
