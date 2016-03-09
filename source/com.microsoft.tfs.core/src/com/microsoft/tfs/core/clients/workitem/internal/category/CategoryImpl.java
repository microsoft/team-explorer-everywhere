// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.category;

import com.microsoft.tfs.core.clients.workitem.category.Category;

public class CategoryImpl implements Category {
    private final int id;
    private final String name;
    private final String referenceName;
    private final int defaultWorkItemTypeId;

    public CategoryImpl(final int id, final String name, final String referenceName, final int defaultWorkItemTypeId) {
        this.id = id;
        this.name = name;
        this.referenceName = referenceName;
        this.defaultWorkItemTypeId = defaultWorkItemTypeId;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getReferenceName() {
        return referenceName;
    }

    @Override
    public int getDefaultWorkItemTypeID() {
        return defaultWorkItemTypeId;
    }
}
