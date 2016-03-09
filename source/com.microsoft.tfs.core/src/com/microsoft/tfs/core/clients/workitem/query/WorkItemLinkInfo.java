// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.link.Link;

/**
 * Structure that represents {@link Link} query results.
 *
 * @since TEE-SDK-10.1
 */
public class WorkItemLinkInfo {
    private final int sourceId;
    private final int targetId;
    private final int linkTypeId;
    private final boolean isLocked;

    public WorkItemLinkInfo(final int sourceId, final int targetId, final int linkTypeId, final boolean isLocked) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.linkTypeId = linkTypeId;
        this.isLocked = isLocked;
    }

    public int getSourceID() {
        return sourceId;
    }

    public int getTargetID() {
        return targetId;
    }

    public int getLinkTypeID() {
        return linkTypeId;
    }

    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + linkTypeId;
        result = prime * result + sourceId;
        result = prime * result + targetId;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WorkItemLinkInfo other = (WorkItemLinkInfo) obj;
        return isLocked == other.isLocked
            && linkTypeId == other.linkTypeId
            && sourceId == other.sourceId
            && targetId == other.targetId;
    }

}
