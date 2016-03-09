// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

/**
 * This class has no analog in MS code. It exists simply as a dumb data holder
 * class to handle the parsed data from GetStoredQueryItems.
 *
 * @threadsafety thread safe
 */
public final class GetStoredQueryItemRowSet {
    private final String id;
    private final String name;
    private final String text;
    private final String ownerIdentifier;
    private final String ownerType;
    private final String parentId;
    private final boolean folder;
    private final boolean deleted;
    private final long cacheStamp;

    public GetStoredQueryItemRowSet(
        final String id,
        final String name,
        final String text,
        final String ownerIdentifier,
        final String ownerType,
        final String parentId,
        final boolean folder,
        final boolean deleted,
        final long cacheStamp) {
        this.id = id;
        this.name = name;
        this.text = text;
        this.ownerIdentifier = ownerIdentifier;
        this.ownerType = ownerType;
        this.parentId = parentId;
        this.folder = folder;
        this.deleted = deleted;
        this.cacheStamp = cacheStamp;
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public String getOwnerIdentifier() {
        return ownerIdentifier;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public String getParentID() {
        return parentId;
    }

    public boolean isFolder() {
        return folder;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public long getCacheStamp() {
        return cacheStamp;
    }
}
