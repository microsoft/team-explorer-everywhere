// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

import com.microsoft.tfs.core.clients.workitem.WorkItem;

/**
 * @since TEE-SDK-10.1
 */
public interface RelatedLink extends Link {
    public WorkItem getSourceWorkItem();

    public int getTargetWorkItemID();

    public int getWorkItemLinkTypeID();

    public WorkItem getTargetWorkItem();

    public void setSourceWorkItem(WorkItem workItem);
}
