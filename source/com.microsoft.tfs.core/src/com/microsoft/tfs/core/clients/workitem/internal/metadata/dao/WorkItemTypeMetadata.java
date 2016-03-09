// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

public class WorkItemTypeMetadata {
    private final int id;
    private final String name;

    public WorkItemTypeMetadata(final int id, final String name) {
        this.id = id;
        this.name = name;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }
}
