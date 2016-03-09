// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

public class WorkItemTypeCategoryMetadata {
    private final int categoryId;
    private final int projectId;
    private final String name;
    private final String referenceName;
    private final int defaultWorkItemTypeId;

    public WorkItemTypeCategoryMetadata(
        final int categoryId,
        final int projectId,
        final String name,
        final String referenceName,
        final int defaultWorkItemTypeId) {
        this.categoryId = categoryId;
        this.projectId = projectId;
        this.name = name;
        this.referenceName = referenceName;
        this.defaultWorkItemTypeId = defaultWorkItemTypeId;
    }

    public int getCategoryID() {
        return categoryId;
    }

    public int getProjectID() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public int getDefaultWorkItemTypeID() {
        return defaultWorkItemTypeId;
    }
}
