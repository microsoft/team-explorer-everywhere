// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

public class WorkItemTypeCategoryMemberMetadata {
    private final int categoryMemberID;
    private final int categoryID;
    private final int workItemTypeID;

    public WorkItemTypeCategoryMemberMetadata(
        final int categoryMemberID,
        final int categoryID,
        final int workItemTypeID) {
        this.categoryMemberID = categoryMemberID;
        this.categoryID = categoryID;
        this.workItemTypeID = workItemTypeID;
    }

    public int getCategoryMemberID() {
        return categoryMemberID;
    }

    public int getCategoryID() {
        return categoryID;
    }

    public int getWorkItemTypeID() {
        return workItemTypeID;
    }
}
