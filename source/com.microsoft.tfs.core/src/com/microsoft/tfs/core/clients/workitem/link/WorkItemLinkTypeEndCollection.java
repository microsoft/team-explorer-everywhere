// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

import java.util.Iterator;

/**
 * @since TEE-SDK-10.1
 */
public interface WorkItemLinkTypeEndCollection extends Iterable<WorkItemLinkTypeEnd> {
    public int getCount();

    public WorkItemLinkTypeEnd get(String linkTypeEndName);

    public boolean contains(int id);

    public boolean contains(String linkTypeName);

    public WorkItemLinkTypeEnd getByID(int id);

    @Override
    public Iterator<WorkItemLinkTypeEnd> iterator();

    public WorkItemLinkTypeEnd[] toArray(WorkItemLinkTypeEnd[] array);
}
