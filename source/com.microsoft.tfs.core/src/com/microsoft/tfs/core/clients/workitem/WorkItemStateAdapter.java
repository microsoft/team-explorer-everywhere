// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

/**
 * Default implementation of {@link WorkItemStateListener} which does nothing in
 * response to events.
 *
 * @since TEE-SDK-10.1
 */
public class WorkItemStateAdapter implements WorkItemStateListener {
    @Override
    public void dirtyStateChanged(final boolean isDirty, final WorkItem workItem) {
    }

    @Override
    public void saved(final WorkItem workItem) {
    }

    @Override
    public void validStateChanged(final boolean isValid, final WorkItem workItem) {
    }

    @Override
    public void synchedToLatest(final WorkItem workItem) {
    }
}
