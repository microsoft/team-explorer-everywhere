// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.mapper;

public interface SQLMapper {
    public String getSQLColumnTypeFromMetadataColumnType(
        String metadataColumnType,
        String tableName,
        String columnName);

    public Object getSQLObject(String metadataColumnType, String metadataColumnValue);
}
