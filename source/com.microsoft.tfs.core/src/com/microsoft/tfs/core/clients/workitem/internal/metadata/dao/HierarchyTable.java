// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

public interface HierarchyTable {
    public NodeMetadata[] getNodesWithParentID(int parentId);

    public NodeMetadata getRootNode();

    public int getParentID(int childId);

    public NodeMetadata[] getNodesWithTypeID(int typeId);

    public NodeMetadata[] getAllNodes();
}