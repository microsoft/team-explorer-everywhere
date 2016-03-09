// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

import java.util.Date;

public class StoredQueryMetadata {
    private final String guid;
    private final int projectId;
    private final String name;
    private final boolean publicQuery;
    private final String wiql;
    private final String description;
    private final Date createTime;

    public StoredQueryMetadata(
        final String guid,
        final int projectId,
        final String name,
        final boolean publicQuery,
        final String wiql,
        final String description,
        final Date createTime) {
        this.guid = guid;
        this.projectId = projectId;
        this.name = name;
        this.publicQuery = publicQuery;
        this.wiql = wiql;
        this.description = description;
        this.createTime = createTime;
    }

    public String getGUID() {
        return guid;
    }

    public String getName() {
        return name;
    }

    public int getProjectID() {
        return projectId;
    }

    public boolean isPublicQuery() {
        return publicQuery;
    }

    public String getWIQL() {
        return wiql;
    }

    public String getDescription() {
        return description;
    }

    public Date getCreateTime() {
        return createTime;
    }
}
