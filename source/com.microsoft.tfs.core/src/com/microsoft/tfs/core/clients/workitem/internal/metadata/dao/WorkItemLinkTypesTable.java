// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

/**
 * Interface which exposes the link types table from the WIT metadata.
 */
public interface WorkItemLinkTypesTable {
    public WorkItemLinkTypeMetadata[] getLinkTypes();
}
